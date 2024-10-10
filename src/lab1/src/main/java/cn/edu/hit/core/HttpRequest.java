package cn.edu.hit.core;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 表示一个 HTTP 请求。
 */
public class HttpRequest {

    // HTTP 请求方法，例如 GET 或 POST
    private final String method;
    // 请求的 URI
    private final URI uri;
    // HTTP 版本，例如 HTTP/1.1
    private final String version;
    // 请求头的映射
    private final Map<String, String> headers;
    // 请求体的字节数组
    private final byte[] body;

    /**
     * 使用 Builder 构造 HttpRequest 对象。
     *
     * @param builder 用于构造 HttpRequest 的 Builder 对象
     */
    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.uri = builder.uri;
        this.headers = builder.headers;
        this.body = builder.body;
        this.version = builder.version;
    }

    /**
     * 创建一个新的 Builder 实例。
     *
     * @return 新的 Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 更新请求头并返回新的 HttpRequest 对象。
     *
     * @param headerName 请求头名称
     * @param headerValue 请求头值
     * @return 更新后的 HttpRequest 对象
     */
    public HttpRequest updateHeader(String headerName, String headerValue) {
        Builder builder = new Builder(this);
        builder.header(headerName, headerValue);
        return builder.build();
    }

    /**
     * 获取请求的方案（scheme）。
     *
     * @return 请求的方案
     */
    public String getScheme() {
        String scheme = uri.getScheme();
        if (scheme == null) {
            scheme = version.split("/")[0];
        }
        return scheme;
    }

    /**
     * 获取请求方法。
     *
     * @return 请求方法
     */
    public String getMethod() {
        return method;
    }

    /**
     * 获取请求的 URI。
     *
     * @return 请求的 URI
     */
    public URI getUri() {
        return uri;
    }

    /**
     * 获取 HTTP 版本。
     *
     * @return HTTP 版本
     */
    public String getVersion() {
        return version;
    }

    /**
     * 获取请求头的不可修改映射。
     *
     * @return 请求头的不可修改映射
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * 获取请求体的字节数组。
     *
     * @return 请求体的字节数组
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * 获取请求的主机名。
     *
     * @return 请求的主机名
     */
    public String getHost() {
        return headers.getOrDefault("Host", uri.getHost());
    }

    /**
     * 获取请求的端口号。
     *
     * @return 请求的端口号
     */
    public int getPort() {
        // 尝试从 Host 头部获取端口
        String host = getHost();
        if (host.contains(":")) {
            String[] parts = host.split(":");
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // 如果端口号不是数字，则返回 -1
                return -1;
            }
        }

        // 从 URI 中获取端口
        int portFromUri = uri.getPort();
        if (portFromUri != -1) {
            return portFromUri; // 返回 URI 中指定的端口
        }

        // 返回默认端口
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return HttpConstant.HTTPS_DEFAULT_PORT; // HTTPS 默认端口
        } else {
            return HttpConstant.HTTP_DEFAULT_PORT; // HTTP 默认端口
        }
    }

    /**
     * 返回请求的字符串表示形式。
     *
     * @return 请求的字符串表示形式
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(uri.toString()).append(" ").append(version).append("\r\n");
        headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\r\n"));
        // 添加空行，表示请求头结束
        sb.append("\r\n");
        return sb.toString();
    }

    /**
     * 用于构建 HttpRequest 对象的 Builder 类。
     */
    public static class Builder {
        // 请求头的映射
        private final Map<String, String> headers = new HashMap<>();
        // 默认 HTTP 版本
        private String version = HttpConstant.HTTP_DEFAULT_VERSION;
        // 请求方法
        private String method;
        // 请求的 URI
        private URI uri;
        // 请求体的字节数组
        private byte[] body;

        /**
         * 默认构造函数。
         */
        public Builder() {}

        /**
         * 使用现有的 HttpRequest 对象构造 Builder。
         *
         * @param request 现有的 HttpRequest 对象
         */
        public Builder(HttpRequest request) {
            this.method = request.method;
            this.uri = request.uri;
            this.headers.putAll(request.headers);
            this.version = request.version;
            this.body = request.body;
        }

        /**
         * 设置请求方法。
         *
         * @param method 请求方法
         * @return 当前 Builder 实例
         */
        public Builder method(String method) {
            this.method = method;
            return this;
        }

        /**
         * 设置请求的 URI。
         *
         * @param uri 请求的 URI
         * @return 当前 Builder 实例
         */
        public Builder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        /**
         * 添加请求头。
         *
         * @param name 请求头名称
         * @param value 请求头值
         * @return 当前 Builder 实例
         */
        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        /**
         * 设置请求头的映射。
         *
         * @param headers 请求头的映射
         * @return 当前 Builder 实例
         */
        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        /**
         * 设置请求体的字节数组。
         *
         * @param body 请求体的字节数组
         * @return 当前 Builder 实例
         */
        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        /**
         * 设置 HTTP 版本。
         *
         * @param version HTTP 版本
         * @return 当前 Builder 实例
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * 构建 HttpRequest 对象。
         *
         * @return 构建的 HttpRequest 对象
         */
        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}
