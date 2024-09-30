package cn.edu.hit.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpProxyServer {
    private static final int DEFAULT_THREAD_POOL_SIZE = 20; // 默认线程池大小
    private static final int DEFAULT_PROXY_PORT = 10240; // 默认代理服务器端口
    private final ExecutorService threadPool; // 线程池
    private final int threadPoolSize; // 线程池大小
    private final int proxyPort; // 代理服务器端口

    public HttpProxyServer(int threadPoolSize, int proxyPort) {
        this.threadPoolSize = threadPoolSize;
        this.proxyPort = proxyPort;
        threadPool = Executors.newFixedThreadPool(threadPoolSize); // 创建线程池
        System.out.println("[HttpProxyServer] 初始化线程池，大小: " + threadPoolSize + ", 监听端口: " + proxyPort);
    }

    public HttpProxyServer() {
        threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        proxyPort = DEFAULT_PROXY_PORT;
        threadPool = Executors.newFixedThreadPool(threadPoolSize); // 创建线程池
        System.out.println("[HttpProxyServer] 初始化默认线程池，大小: " + threadPoolSize + ", 监听端口: " + proxyPort);
    }

    public static void main(String[] args) {
        try {
            new HttpProxyServer().start();
        } catch (IOException e) {
            System.err.println("[HttpProxyServer] 代理服务器启动失败: " + e.getMessage());
        }
    }

    public void start() throws IOException {
        try (ServerSocket proxyServerSocket = new ServerSocket(proxyPort)) { // 创建ServerSocket监听端口
            System.out.println("[HttpProxyServer] 代理服务器正在运行，监听端口: " + proxyPort);
            while (true) {
                Socket clientSocket = proxyServerSocket.accept(); // 接收客户端连接
                System.out.println("[HttpProxyServer] 接收到客户端连接: " + clientSocket.getRemoteSocketAddress());
                threadPool.submit(new ProxyHandler(clientSocket)); // 提交任务到线程池
            }
        } catch (IOException e) {
            System.err.println("[HttpProxyServer] 服务器异常: " + e.getMessage());
            throw e;
        }
    }
}
