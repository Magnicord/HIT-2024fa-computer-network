package cn.edu.hit;

import java.io.IOException;

import cn.edu.hit.server.HttpProxyServer;

public class Main {
    public static void main(String[] args) throws IOException {
        int threadPoolSize = 10; // 线程池大小
        int proxyPort = 10240; // 代理服务器端口
        new HttpProxyServer(threadPoolSize, proxyPort).start();
    }
}
