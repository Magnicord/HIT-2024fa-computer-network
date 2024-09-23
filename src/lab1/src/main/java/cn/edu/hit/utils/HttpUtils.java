package cn.edu.hit.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.time.LocalDateTime;

import cn.edu.hit.core.HttpConstant;
import cn.edu.hit.core.HttpRequest;
import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.core.HttpStatus;

public class HttpUtils {

    public static HttpRequest parseHttpRequest(BufferedReader clientReader) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        String requestLine = clientReader.readLine();
        String[] requestLineParts = requestLine.split(" ");
        builder.method(requestLineParts[0]).uri(URI.create(requestLineParts[1]));

        String line;
        while ((line = clientReader.readLine()) != null && !line.isEmpty()) {
            String[] headerParts = line.split(": ", 2); // 指定最多只拆分两部分
            if (headerParts.length == 2) { // 检查是否成功拆分，防止解析失败
                builder.header(headerParts[0], headerParts[1]);
            }
        }

        StringBuilder body = new StringBuilder();
        while ((line = clientReader.readLine()) != null) {
            body.append(line).append("\r\n");
        }

        builder.body(body.toString());
        return builder.build();
    }

    public static HttpResponse parseHttpResponse(BufferedReader reader) throws IOException {
        HttpResponse.Builder builder = HttpResponse.newBuilder();
        String statusLine = reader.readLine();
        String[] statusParts = statusLine.split(" ");
        builder.version(statusParts[0]);
        int statusCode = Integer.parseInt(statusParts[1]);
        builder.statusCode(HttpStatus.getStatusFromCode(statusCode));

        String line;
        while (!(line = reader.readLine()).isEmpty()) {
            String[] headerParts = line.split(": ", 2);
            if (headerParts.length == 2) {
                builder.header(headerParts[0], headerParts[1]);
            }
        }

        // 读取响应体
        StringBuilder body = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            body.append(line).append("\r\n");
        }
        builder.body(body.toString());

        return builder.build();
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
