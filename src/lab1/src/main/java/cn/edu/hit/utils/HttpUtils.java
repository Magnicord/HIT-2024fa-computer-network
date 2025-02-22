package cn.edu.hit.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import cn.edu.hit.core.HttpConstant;
import cn.edu.hit.core.HttpRequest;
import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.core.HttpStatus;

/**
 * HTTP 工具类，提供解析 HTTP 请求和响应、转发请求和响应等功能。
 */
public class HttpUtils {

    /**
     * 解析 HTTP 请求。
     *
     * @param clientIn 客户端输入流
     * @return 解析后的 HttpRequest 对象
     * @throws IOException 如果发生 I/O 错误
     */
    public static HttpRequest parseHttpRequest(InputStream clientIn) throws IOException {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        // 读取请求行
        String httpHeader = parseHttpHeader(clientIn);
        String[] lines = httpHeader.split("\r\n");
        String[] requestLineParts = lines[0].split(" ");
        String method = requestLineParts[0]; // 请求方法
        URI uri = URI.create(requestLineParts[1]); // 请求 URI
        String version = requestLineParts[2]; // HTTP 版本
        builder.method(method).uri(uri).version(version);

        // 解析头部字段（从第 2 行开始）
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue; // 跳过空行
            }
            // 每个头部字段是 key: value 格式
            String[] headerParts = lines[i].split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }

        builder.headers(headers); // 设置请求头

        // 读取请求体
        if (headers.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            byte[] body = parseHttpBodyFixed(clientIn, contentLength);
            builder.body(body);
        }
        return builder.build();
    }

    /**
     * 解析 HTTP 响应。
     *
     * @param serverIn 服务器输入流
     * @return 解析后的 HttpResponse 对象
     * @throws IOException 如果发生 I/O 错误
     */
    public static HttpResponse parseHttpResponse(InputStream serverIn) throws IOException {
        HttpResponse.Builder builder = HttpResponse.newBuilder();
        // 读取响应头
        String httpHeader = parseHttpHeader(serverIn);
        // 拆分头部字符串为各行
        String[] lines = httpHeader.split("\r\n");

        // 第一行是状态行，格式如: HTTP/1.1 200 OK
        String[] statusLineParts = lines[0].split(" ");
        String version = statusLineParts[0]; // HTTP 版本
        int statusCode = Integer.parseInt(statusLineParts[1]); // 状态码
        HttpStatus status = HttpStatus.getStatusFromCode(statusCode);

        builder.version(version); // 设置版本
        builder.statusCode(status); // 设置状态码

        // 解析头部字段（从第 2 行开始）
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue; // 跳过空行
            }
            // 每个头部字段是 key: value 格式
            String[] headerParts = lines[i].split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }

        builder.headers(headers); // 设置响应头

        // 读取响应体
        byte[] body = new byte[0];
        if (headers.containsKey("Content-Length")) {
            int contentLength = Integer.parseInt(headers.get("Content-Length"));
            System.out.println("[HttpUtils] 使用固定长度解析响应体");
            body = parseHttpBodyFixed(serverIn, contentLength);
        } else if (headers.containsKey("Transfer-Encoding") && headers.get("Transfer-Encoding").equals("chunked")) {
            System.out.println("[HttpUtils] 使用分块传输解析响应体");
            body = parseHttpBodyChunked(serverIn);
        }
        builder.body(body);
        return builder.build();
    }

    /**
     * 解析 HTTP 头部。
     *
     * @param in 输入流
     * @return 解析后的头部字符串
     * @throws IOException 如果发生 I/O 错误
     */
    public static String parseHttpHeader(InputStream in) throws IOException {
        StringBuilder headerBuilder = new StringBuilder();
        int previous = -1, current;

        // 逐字节读取，直到遇到连续的 \r\n\r\n 作为结束标志
        while ((current = in.read()) != -1) {
            headerBuilder.append((char)current);
            // 检查是否遇到了 \r\n\r\n，表示头部结束
            if (previous == '\r' && current == '\n' && headerBuilder.toString().endsWith("\r\n\r\n")) {
                break;
            }
            previous = current;
        }
        return headerBuilder.toString(); // 返回完整的头部字符串
    }

    /**
     * 解析固定长度的 HTTP 响应体。
     *
     * @param inputStream 输入流
     * @param contentLength 内容长度
     * @return 解析后的响应体字节数组
     * @throws IOException 如果发生 I/O 错误
     */
    private static byte[] parseHttpBodyFixed(InputStream inputStream, int contentLength) throws IOException {
        byte[] body = new byte[contentLength];
        int bytesReadTotal = 0; // 已经读取的字节数
        while (bytesReadTotal < contentLength) {
            // 读取剩余的字节
            int bytesRead = inputStream.read(body, bytesReadTotal, contentLength - bytesReadTotal);
            if (bytesRead == -1) {
                // 连接已经关闭或读取到末尾，但数据尚未完全读取
                throw new IOException("流已关闭，未能完全读取响应体");
            }
            bytesReadTotal += bytesRead;
        }
        return body;
    }

    /**
     * 解析分块传输的 HTTP 响应体。
     *
     * @param inputStream 输入流
     * @return 解析后的响应体字节数组
     * @throws IOException 如果发生 I/O 错误
     */
    private static byte[] parseHttpBodyChunked(InputStream inputStream) throws IOException {
        ByteArrayOutputStream chunkedBody = new ByteArrayOutputStream();
        while (true) {
            // 读取块大小行
            String chunkSizeLine = readLine(inputStream);
            // 解析块大小
            int chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16); // 块大小为 16 进制
            if (chunkSize == 0) {
                break; // 读取到最后一个块，结束
            }

            // 读取该块的数据
            byte[] chunk = new byte[chunkSize];
            int bytesRead = inputStream.read(chunk, 0, chunkSize);
            chunkedBody.write(chunk, 0, bytesRead);

            // 读取块末尾的 \r\n
            readLine(inputStream);
        }
        // 最后读取尾部的 \r\n（如果有）
        readLine(inputStream);

        return chunkedBody.toByteArray();
    }

    /**
     * 从输入流中读取一行。
     *
     * @param in 输入流
     * @return 读取的行字符串
     * @throws IOException 如果发生 I/O 错误
     */
    private static String readLine(InputStream in) throws IOException {
        StringBuilder lineBuilder = new StringBuilder();
        int current;
        while ((current = in.read()) != -1) {
            if (current == '\r') {
                int next = in.read();
                if (next == '\n') {
                    break; // 遇到 \r\n 则行结束
                } else {
                    lineBuilder.append((char)current);
                    if (next != -1) {
                        lineBuilder.append((char)next);
                    }
                }
            } else {
                lineBuilder.append((char)current);
            }
        }
        return lineBuilder.toString();
    }

    /**
     * 转发 HTTP 请求。
     *
     * @param request HTTP 请求对象
     * @param out 输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void forwardHttpRequest(HttpRequest request, OutputStream out) throws IOException {
        // 1. 转发请求头
        out.write(request.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();

        // 2. 转发请求体
        byte[] body = request.getBody();
        if (request.getHeaders().containsKey("Content-Length")) { // 检查是否有 Content-Length 头部
            forwardBodyFixed(body, out);
        }
    }

    /**
     * 转发 HTTP 响应。
     *
     * @param response HTTP 响应对象
     * @param out 输出流
     * @throws IOException 如果发生 I/O 错误
     */
    public static void forwardHttpResponse(HttpResponse response, OutputStream out) throws IOException {
        // 1. 转发响应头
        out.write(response.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();

        // 2. 转发响应体
        byte[] body = response.getBody();
        if (body != null && body.length > 0) {
            String transferEncoding = response.getHeaders().get("Transfer-Encoding");
            if ("chunked".equalsIgnoreCase(transferEncoding)) { // 检查是否为 chunked 传输编码
                forwardBodyChunked(body, out);
            } else if (response.getHeaders().containsKey("Content-Length")) { // 检查是否有 Content-Length 头部
                forwardBodyFixed(body, out);
            }
        }
    }

    /**
     * 以分块传输方式转发响应体。
     *
     * @param body 响应体字节数组
     * @param out 输出流
     * @throws IOException 如果发生 I/O 错误
     */
    private static void forwardBodyChunked(byte[] body, OutputStream out) throws IOException {
        // 将响应体以 chunked 传输方式发送
        int contentLength = body.length; // 响应体长度
        int chunkSize = 1024; // 每个块的大小（可以调整）
        int bytesWrittenTotal = 0; // 已经写入的字节数

        while (bytesWrittenTotal < contentLength) {
            // 计算当前写入块大小
            int currentChunkSize = Math.min(chunkSize, contentLength - bytesWrittenTotal);
            // 表示为 16 进制
            String chunkSizeHex = Integer.toHexString(currentChunkSize) + "\r\n";

            // 1. 写入块大小
            out.write(chunkSizeHex.getBytes(StandardCharsets.UTF_8));
            // 2. 写入块数据
            out.write(body, bytesWrittenTotal, currentChunkSize);
            // 3. 写入块末尾的 \r\n
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            bytesWrittenTotal += currentChunkSize;
        }

        // 写入最后一个空块，表示结束
        out.write("0\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /**
     * 以固定长度方式转发响应体。
     *
     * @param body 响应体字节数组
     * @param out 输出流
     * @throws IOException 如果发生 I/O 错误
     */
    private static void forwardBodyFixed(byte[] body, OutputStream out) throws IOException {
        int contentLength = body.length; // 响应体长度
        int bytesWrittenTotal = 0; // 已经写入的字节数
        int minBytesWritten = 8192; // 每次写入的最小字节数
        while (bytesWrittenTotal < contentLength) {
            int bytesWritten = Math.min(contentLength - bytesWrittenTotal, minBytesWritten);
            out.write(body, bytesWrittenTotal, bytesWritten);
            out.flush();
            bytesWrittenTotal += bytesWritten;
        }
    }

    /**
     * 连接到服务器。
     *
     * @param host 服务器主机名
     * @param port 服务器端口
     * @return 连接的 Socket 对象
     * @throws IOException 如果发生 I/O 错误
     */
    public static Socket connectToServer(String host, int port) throws IOException {
        return connectToServer(host, port, HttpConstant.DEFAULT_CONNECT_TIMEOUT);
    }

    /**
     * 连接到服务器。
     *
     * @param host 服务器主机名
     * @param port 服务器端口
     * @param timeout 连接超时时间
     * @return 连接的 Socket 对象
     * @throws IOException 如果发生 I/O 错误
     */
    public static Socket connectToServer(String host, int port, int timeout) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        return socket;
    }

    /**
     * 添加 If-Modified-Since 头部到 HTTP 请求。
     *
     * @param request HTTP 请求对象
     * @param lastModified 上次修改时间
     * @return 更新后的 HTTP 请求对象
     */
    public static HttpRequest addIfModifiedSinceHeader(HttpRequest request, ZonedDateTime lastModified) {
        return request.updateHeader("If-Modified-Since",
            lastModified != null ? DateUtils.parseHttpDateTime(lastModified) : null);
    }
}
