package cn.edu.hit.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP 代理服务器类，负责监听客户端请求并将其转发到目标服务器。
 */
public class HttpProxyServer {

    private static final int DEFAULT_THREAD_POOL_SIZE = 20; // 默认线程池大小
    private static final int DEFAULT_PROXY_PORT = 20000; // 默认代理服务器端口
    private final ExecutorService threadPool; // 线程池
    private final int threadPoolSize; // 线程池大小
    private final int proxyPort; // 代理服务器端口

    /**
     * 构造函数，初始化代理服务器。
     *
     * @param threadPoolSize 线程池大小
     * @param proxyPort 代理服务器端口
     */
    public HttpProxyServer(int threadPoolSize, int proxyPort) {
        this.threadPoolSize = threadPoolSize;
        this.proxyPort = proxyPort;
        threadPool = Executors.newFixedThreadPool(threadPoolSize); // 创建线程池
        System.out.println("[HttpProxyServer] 初始化线程池，大小: " + threadPoolSize + ", 监听端口: " + proxyPort);
    }

    /**
     * 默认构造函数，使用默认线程池大小和端口。
     */
    public HttpProxyServer() {
        threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        proxyPort = DEFAULT_PROXY_PORT;
        threadPool = Executors.newFixedThreadPool(threadPoolSize); // 创建线程池
        System.out.println("[HttpProxyServer] 初始化默认线程池，大小: " + threadPoolSize + ", 监听端口: " + proxyPort);
    }

    /**
     * 主方法，启动代理服务器。
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        Set<String> blockedSites = Set.of("www.hit.edu.cn");
        Set<String> blockedUsers = Set.of("127.0.0.2");
        Map<String, String> redirectSites = Map.of("jwes.hit.edu.cn", "jwts.hit.edu.cn");
        try {
            new HttpProxyServer().start(blockedSites, blockedUsers, redirectSites);
        } catch (IOException e) {
            System.err.println("[HttpProxyServer] 代理服务器启动失败: " + e.getMessage());
        }
    }

    /**
     * 启动代理服务器，使用指定的过滤规则。
     *
     * @param blockedSites 被禁止访问的网站集合
     * @param blockedUsers 被禁止访问的用户集合
     * @param redirectSites 网站重定向映射
     * @throws IOException 如果发生 I/O 错误
     */
    public void start(Set<String> blockedSites, Set<String> blockedUsers, Map<String, String> redirectSites)
        throws IOException {
        try (ServerSocket proxyServerSocket = new ServerSocket(proxyPort)) { // 创建ServerSocket监听端口
            System.out.println("[HttpProxyServer] 代理服务器正在运行，监听端口: " + proxyPort);
            while (true) {
                // 接收客户端连接
                Socket clientSocket = proxyServerSocket.accept();
                System.out.println("[HttpProxyServer] 接收到客户端连接: " + clientSocket.getRemoteSocketAddress());
                // 提交任务到线程池
                threadPool.submit(new ProxyHandler(clientSocket, blockedSites, blockedUsers, redirectSites));
            }
        } catch (IOException e) {
            System.err.println("[HttpProxyServer] 服务器异常: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 启动代理服务器，使用默认的过滤规则。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    public void start() throws IOException {
        try (ServerSocket proxyServerSocket = new ServerSocket(proxyPort)) { // 创建ServerSocket监听端口
            System.out.println("[HttpProxyServer] 代理服务器正在运行，监听端口: " + proxyPort);
            while (true) {
                // 接收客户端连接
                Socket clientSocket = proxyServerSocket.accept();
                System.out.println("[HttpProxyServer] 接收到客户端连接: " + clientSocket.getRemoteSocketAddress());
                // 提交任务到线程池
                threadPool.submit(new ProxyHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("[HttpProxyServer] 服务器异常: " + e.getMessage());
            throw e;
        }
    }
}
