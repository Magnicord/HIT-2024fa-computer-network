package cn.edu.hit.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import cn.edu.hit.cache.CacheEntry;
import cn.edu.hit.cache.CacheManager;
import cn.edu.hit.core.HttpConstant;
import cn.edu.hit.core.HttpRequest;
import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.core.HttpStatus;
import cn.edu.hit.utils.HttpUtils;

public class ProxyHandler implements Runnable {
    private static final CacheManager cacheManager = new CacheManager(); // 全局缓存管理器
    private final Socket clientSocket;

    public ProxyHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        System.out.println("[ProxyHandler] 创建新的代理处理器，客户端地址: " + clientSocket.getRemoteSocketAddress());
    }

    @Override
    public void run() {
        try {
            handleProxy(clientSocket); // 调用代理处理逻辑
        } catch (IOException e) {
            System.err.println("[ProxyHandler] 处理请求时发生错误: " + e.getMessage());
        }
    }

    private void handleProxy(Socket clientSocket) throws IOException {
        System.out.println("[ProxyHandler] 开始处理客户端请求: " + clientSocket.getRemoteSocketAddress());
        try (InputStream clientIn = clientSocket.getInputStream()) {
            // 读取客户端请求，解析HTTP请求头
            HttpRequest httpRequest = HttpUtils.parseHttpRequest(clientIn);
            System.out.println("[ProxyHandler] 已解析客户端请求，URI: " + httpRequest.getUri());

            if (!httpRequest.getVersion().equals(HttpConstant.HTTP_DEFAULT_VERSION)) {
                System.err.println("[ProxyHandler] 仅支持HTTP协议，不支持HTTPS");
                return;
            }

            System.out.println("[ProxyHandler] 请求头部: " + httpRequest.getHeaders());
            // host
            System.out.println("[ProxyHandler] Host: " + httpRequest.getHost());
            // port
            System.out.println("[ProxyHandler] Port: " + httpRequest.getPort());

            // 获取请求的 URI，用于缓存
            String uri = httpRequest.getUri().toString();
            CacheEntry cachedEntry = cacheManager.get(uri);

            // 如果缓存命中，则添加 If-Modified-Since 头部
            if (cachedEntry != null) {
                System.out.println("[ProxyHandler] 缓存命中，添加 If-Modified-Since 头部");
                httpRequest = HttpUtils.addIfModifiedSinceHeader(httpRequest, cachedEntry.getLastModified());
            } else {
                System.out.println("[ProxyHandler] 缓存未命中，直接转发请求");
            }

            try (
                // 连接目标服务器
                Socket serverSocket = HttpUtils.connectToServer(httpRequest.getHost(), httpRequest.getPort());
                InputStream serverIn = serverSocket.getInputStream();
                OutputStream serverOut = serverSocket.getOutputStream();
                OutputStream clientOut = clientSocket.getOutputStream()) {
                System.out.println("[ProxyHandler] 成功连接目标服务器: " + httpRequest.getHost() + ":" + httpRequest.getPort());

                // 将客户端的请求转发给目标服务器
                HttpUtils.forwardHttpRequest(httpRequest, serverOut);
                System.out.println("[ProxyHandler] 已将请求转发至目标服务器");

                // 读取目标服务器响应，解析HTTP响应头
                HttpResponse httpResponse = HttpUtils.parseHttpResponse(serverIn);
                System.out.println("[ProxyHandler] 已接收到目标服务器响应，状态码: " + httpResponse.getStatusCode());

                if (cachedEntry != null && httpResponse.getStatusCode().equals(HttpStatus.NOT_MODIFIED)) {
                    // 如果缓存命中，且目标服务器返回 304 Not Modified，则直接返回缓存的响应
                    System.out.println("[ProxyHandler] 目标服务器返回304 Not Modified，直接使用缓存响应");
                    HttpUtils.forwardHttpResponse(cachedEntry.getResponse(), clientOut);
                } else {
                    // 如果目标服务器返回新的响应，则更新缓存
                    System.out.println("[ProxyHandler] 目标服务器返回新响应，更新缓存");
                    cacheManager.put(uri, new CacheEntry(httpResponse));
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
