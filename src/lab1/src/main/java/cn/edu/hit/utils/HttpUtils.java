package cn.edu.hit.utils;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import cn.edu.hit.core.HttpConstant;
import cn.edu.hit.core.HttpRequest;
import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.core.HttpStatus;

public class HttpUtils {

    public static HttpRequest parseHttpRequest(InputStream clientIn) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        try (BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientIn))) {
            String requestLine = clientReader.readLine();
            String[] requestLineParts = requestLine.split(" ");
            builder.method(requestLineParts[0]).uri(URI.create(requestLineParts[1]));

            String headLine;
            Map<String, String> headers = new HashMap<>();
            while ((headLine = clientReader.readLine()) != null && !headLine.isEmpty()) {
                String[] headerParts = headLine.split(": ", 2);// 指定最多只拆分两部分
                headers.put(headerParts[0], headerParts[1]);
                if (headerParts.length == 2) { // 检查是否成功拆分，防止解析失败
                    headers.put(headerParts[0], headerParts[1]);
                }
            }
            builder.headers(headers);

            // 读取请求体
            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                byte[] body = new byte[contentLength];
                int bytesRead = clientIn.read(body);
                if (bytesRead == contentLength) {
                    builder.body(body);
                }
            }
        }

        return builder.build();
    }

    public static HttpResponse parseHttpResponse(InputStream serverIn) throws IOException {
        HttpResponse.Builder builder = HttpResponse.newBuilder();
        try (BufferedReader serverReader = new BufferedReader(new InputStreamReader(serverIn))) {
            String statusLine = serverReader.readLine();
            String[] statusParts = statusLine.split(" ");
            builder.version(statusParts[0]);
            int statusCode = Integer.parseInt(statusParts[1]);
            builder.statusCode(HttpStatus.getStatusFromCode(statusCode));

            String headLine;
            Map<String, String> headers = new HashMap<>();
            while ((headLine = serverReader.readLine()) != null && !headLine.isEmpty()) {
                String[] headerParts = headLine.split(": ", 2);// 指定最多只拆分两部分
                headers.put(headerParts[0], headerParts[1]);
                if (headerParts.length == 2) { // 检查是否成功拆分，防止解析失败
                    headers.put(headerParts[0], headerParts[1]);
                }
            }
            builder.headers(headers);

            // 读取响应体
            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                byte[] body = new byte[contentLength];
                int bytesRead = serverIn.read(body);
                if (bytesRead == contentLength) {
                    builder.body(body);
                }
            }
        }
        return builder.build();
    }

    public static void forwardHttpRequest(HttpRequest request, OutputStream out) throws IOException {
        // 转发请求头
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        writer.write(request.toString());
        writer.flush();

        // 转发请求体
        if (request.getBody() != null) {
            out.write(request.getBody());
        }
    }

    public static void forwardHttpResponse(HttpResponse response, OutputStream out) throws IOException {
        // 转发响应头
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        writer.write(response.toString());
        writer.flush();

        // 转发响应体
        if (response.getBody() != null) {
            out.write(response.getBody());
        }
    }

    public static Socket connectToServer(String host, int port) throws IOException {
        return connectToServer(host, port, HttpConstant.DEFAULT_CONNECT_TIMEOUT);
    }

    public static Socket connectToServer(String host, int port, int timeout) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        return socket;
    }

    // 添加If-Modified-Since头部
    public static HttpRequest addIfModifiedSinceHeader(HttpRequest request, LocalDateTime lastModified) {
        HttpRequest.Builder modifiedRequest = HttpRequest.newBuilder();
        modifiedRequest.method(request.getMethod()).uri(request.getUri()).headers(request.getHeaders())
            .version(request.getVersion());

        if (lastModified != null) {
            String lastModifiedString = DateUtils.parseHttpDateTime(lastModified);
            modifiedRequest.header("If-Modified-Since", lastModifiedString);
        }

        return modifiedRequest.build();
    }
}
