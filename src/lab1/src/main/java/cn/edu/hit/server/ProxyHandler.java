package cn.edu.hit.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

import cn.edu.hit.cache.CacheManager;
import cn.edu.hit.core.HttpConstant;
import cn.edu.hit.core.HttpRequest;
import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.core.HttpStatus;
import cn.edu.hit.filter.FilterManager;
import cn.edu.hit.utils.HttpUtils;

/**
 * 代理处理器类，用于处理客户端请求并转发到目标服务器。
 */
public class ProxyHandler implements Runnable {

    private static final CacheManager cacheManager = new CacheManager(); // 全局缓存管理器
    private final FilterManager filterManager; // 全局过滤器管理器
    private final Socket clientSocket; // 客户端套接字

    /**
     * 构造函数，初始化代理处理器。
     *
     * @param clientSocket 客户端套接字
     */
    public ProxyHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.filterManager = new FilterManager();
        System.out.println("[ProxyHandler] 创建新的代理处理器，客户端地址: " + clientSocket.getRemoteSocketAddress());
    }

    /**
     * 构造函数，初始化代理处理器。
     *
     * @param clientSocket 客户端套接字
     * @param blockedSites 被禁止访问的网站集合
     * @param blockedUsers 被禁止访问的用户集合
     * @param redirectSites 网站重定向映射
     */
    public ProxyHandler(Socket clientSocket, Set<String> blockedSites, Set<String> blockedUsers,
        Map<String, String> redirectSites) {
        this.clientSocket = clientSocket;
        this.filterManager = new FilterManager(blockedSites, blockedUsers, redirectSites);
        System.out.println("[ProxyHandler] 创建新的代理处理器，客户端地址: " + clientSocket.getRemoteSocketAddress());
    }

    /**
     * 运行方法，处理客户端请求。
     */
    @Override
    public void run() {
        try {
            handleProxy(clientSocket); // 调用代理处理逻辑
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[ProxyHandler] 处理请求时发生错误: " + e.getMessage());
        }
    }

    /**
     * 处理代理逻辑。
     *
     * @param clientSocket 客户端套接字
     * @throws IOException 如果发生 I/O 错误
     * @throws ClassNotFoundException 如果类未找到
     */
    private void handleProxy(Socket clientSocket) throws IOException, ClassNotFoundException {
        String user = clientSocket.getInetAddress().getHostAddress();
        System.out.printf("[ProxyHandler] 正在处理用户 [%s] 的请求\n", user);
        if (filterManager.isUserBlocked(user)) {
            System.out.println("[ProxyHandler] 该用户已被禁止访问: " + user);
            return;
        }

        System.out.println("[ProxyHandler] 开始处理客户端请求: " + clientSocket.getRemoteSocketAddress());
        try (InputStream clientIn = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream()) {
            // 读取客户端请求，解析HTTP请求头
            HttpRequest httpRequest = HttpUtils.parseHttpRequest(clientIn);
            System.out.println("[ProxyHandler] 已解析客户端请求，请求体如下: ");
            System.out.println("========================================");
            System.out.print(httpRequest);
            System.out.println("========================================");

            String host = httpRequest.getHost();

            // 过滤逻辑
            if (filterManager.isSiteBlocked(host)) {
                System.out.println("[ProxyHandler] 该网站已被禁止访问: " + host);
                return;
            }

            if (httpRequest.getPort() != HttpConstant.HTTP_DEFAULT_PORT) {
                System.err.println("[ProxyHandler] 仅支持HTTP协议，不支持HTTPS");
                return;
            }

            // 检查是否有网站重定向规则
            String redirectSite = filterManager.getRedirect(host);
            if (redirectSite != null) {
                System.out.printf("[ProxyHandler] 该网站 [%s] 已被重定向至: [%s]\n", host, redirectSite);
                // 重定向目标网站
                httpRequest = httpRequest.updateHeader("Host", redirectSite);
            }

            // 获取请求的 URI，用于缓存
            String uri = httpRequest.getUri().toString();
            HttpResponse cachedHttpResponse = cacheManager.get(uri);
            System.out.println("[ProxyHandler] 正在处理请求: " + uri);

            // 如果缓存命中，则添加 If-Modified-Since 头部
            if (cachedHttpResponse != null) {
                System.out.println("[ProxyHandler] 缓存命中，添加 If-Modified-Since 头部");
                httpRequest = HttpUtils.addIfModifiedSinceHeader(httpRequest, cachedHttpResponse.getLastModified());
            } else {
                System.out.println("[ProxyHandler] 缓存未命中，直接转发请求");
            }

            try (
                // 连接目标服务器
                Socket serverSocket = HttpUtils.connectToServer(httpRequest.getHost(), httpRequest.getPort());
                InputStream serverIn = serverSocket.getInputStream();
                OutputStream serverOut = serverSocket.getOutputStream()) {
                System.out.println("[ProxyHandler] 成功连接目标服务器: " + httpRequest.getHost() + ":" + httpRequest.getPort());

                // 将客户端的请求转发给目标服务器
                HttpUtils.forwardHttpRequest(httpRequest, serverOut);
                System.out.println("[ProxyHandler] 已将请求转发至目标服务器");

                // 读取目标服务器响应，解析HTTP响应头
                HttpResponse httpResponse = HttpUtils.parseHttpResponse(serverIn);
                System.out.println("[ProxyHandler] 已接收到目标服务器响应，状态码: " + httpResponse.getStatusCode());

                if (cachedHttpResponse != null && httpResponse.getStatusCode().equals(HttpStatus.NOT_MODIFIED)) {
                    // 如果缓存命中，且目标服务器返回 304 Not Modified，则直接返回缓存的响应
                    System.out.println("[ProxyHandler] 目标服务器返回 304 Not Modified，直接使用缓存响应");
                    System.out.println("========================================");
                    System.out.print(cachedHttpResponse);
                    System.out.println("========================================");
                    HttpUtils.forwardHttpResponse(cachedHttpResponse, clientOut);
                } else {
                    // 如果目标服务器返回新的响应或缓存不存在，则将响应转发给客户端
                    if (httpResponse.getLastModified() != null) {
                        System.out.println("[ProxyHandler] 目标服务器返回新响应中存在 Last-Modified，更新缓存");
                        cacheManager.put(uri, httpResponse);
                    } else {
                        System.out.println("[ProxyHandler] 目标服务器返回新响应中不存在 Last-Modified，不更新缓存");
                    }
                    HttpUtils.forwardHttpResponse(httpResponse, clientOut);
                }

                System.out.println("[ProxyHandler] 已将响应转发至客户端");

            } catch (IOException e) {
                System.err.println("[ProxyHandler] 与目标服务器通信时发生错误: " + e.getMessage());
                throw e;
            }
        }
    }
}
