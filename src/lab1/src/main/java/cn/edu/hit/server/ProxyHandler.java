package cn.edu.hit.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import cn.edu.hit.cache.CacheEntry;
import cn.edu.hit.cache.CacheManager;
import cn.edu.hit.core.HttpRequest;
import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.core.HttpStatus;
import cn.edu.hit.utils.HttpUtils;

public class ProxyHandler implements Runnable {
    private static final CacheManager cacheManager = new CacheManager(); // 全局缓存管理器
    private final Socket clientSocket;

    public ProxyHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            handleProxy(clientSocket); // 调用代理处理逻辑
        } catch (IOException e) {
            System.err.println("处理请求时发生错误: " + e.getMessage());
        }
    }

    private void handleProxy(Socket clientSocket) throws IOException {
        try (clientSocket;
            // 读取客户端请求
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true)) {
            // 解析HTTP请求头
            HttpRequest httpRequest = HttpUtils.parseHttpRequest(clientReader);
            String url = httpRequest.getUri().toString();
            CacheEntry cachedEntry = cacheManager.get(url);

            // 如果缓存命中，则添加 If-Modified-Since 头部
            if (cachedEntry != null) {
                httpRequest = HttpUtils.addIfModifiedSinceHeader(httpRequest, cachedEntry.getLastModified());
            }

            try (
                // 连接目标服务器
                Socket serverSocket = HttpUtils.connectToServer(httpRequest.getHost(), httpRequest.getPort());
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                PrintWriter serverWriter = new PrintWriter(serverSocket.getOutputStream(), true)) {

                // 将客户端的请求转发给目标服务器
                serverWriter.println(httpRequest);

                // 读取目标服务器的响应
                HttpResponse httpResponse = HttpUtils.parseHttpResponse(serverReader);
                if (cachedEntry != null && httpResponse.getStatusCode().equals(HttpStatus.NOT_MODIFIED)) {
                    // 如果目标服务器返回 304 Not Modified，则直接返回缓存的响应
                    clientWriter.println(cachedEntry.getResponse());
                } else {
                    // 如果目标服务器返回新的响应，则更新缓存
                    cacheManager.put(url, new CacheEntry(httpResponse));
                    clientWriter.println(httpResponse);
                }

                // // 将目标服务器的响应转发给客户端
                // forwardData(serverReader, clientWriter);
            }
        }
    }
    //
    // private void forwardData(BufferedReader reader, PrintWriter writer) throws IOException {
    // String line;
    // while ((line = reader.readLine()) != null) {
    // writer.println(line); // 转发数据
    // }
    // }
}
