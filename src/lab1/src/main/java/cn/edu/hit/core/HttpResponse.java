package cn.edu.hit.core;

import static cn.edu.hit.utils.DateUtils.parseHttpDateTime;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 表示一个 HTTP 响应。
 */
public class HttpResponse implements Serializable {

    // HTTP 版本，例如 HTTP/1.1
    private final String version;
    // HTTP 状态码，例如 200, 304 等
    private final HttpStatus statusCode;
    // HTTP 响应头的映射
    private final Map<String, String> headers;
    // HTTP 响应体的字节数组
    private final byte[] body;

    /**
     * 使用 Builder 构造 HttpResponse 对象。
     *
     * @param builder 用于构造 HttpResponse 的 Builder 对象
     */
    private HttpResponse(Builder builder) {
        this.version = builder.version;
        this.statusCode = builder.statusCode;
        this.headers = builder.headers;
        this.body = builder.body;
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
     * 更新响应头并返回新的 HttpResponse 对象。
     *
     * @param headerName 响应头名称
     * @param headerValue 响应头值
     * @return 更新后的 HttpResponse 对象
     */
    public HttpResponse updateHeader(String headerName, String headerValue) {
        Builder builder = new Builder(this);
        builder.header(headerName, headerValue);
        return builder.build();
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
     * 获取响应状态码。
     *
     * @return 响应状态码
     */
    public HttpStatus getStatusCode() {
        return statusCode;
    }

    /**
     * 获取响应头的不可修改映射。
     *
     * @return 响应头的不可修改映射
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * 获取响应体的字节数组。
     *
     * @return 响应体的字节数组
     */
    public byte[] getBody() {
        return body;
    }

    /**
     * 获取 Last-Modified 时间。
     *
     * @return Last-Modified 时间
     */
    public ZonedDateTime getLastModified() {
        String timeStr = headers.getOrDefault("Last-Modified", null);
        return timeStr != null ? parseHttpDateTime(timeStr) : null;
    }

    /**
     * 返回响应的字符串表示形式。
     *
     * @return 响应的字符串表示形式
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String statusLine = String.format("%s %d %s\r\n", version, statusCode.getCode(), statusCode.getDescription());
        sb.append(statusLine);
        headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\r\n"));
        sb.append("\r\n");
        return sb.toString();
    }

    /**
     * 用于构建 HttpResponse 对象的 Builder 类。
     */
    public static class Builder {
        // 响应头的映射
        private final Map<String, String> headers = new HashMap<>();
        // 默认 HTTP 版本
        private String version = HttpConstant.HTTP_DEFAULT_VERSION;
        // HTTP 状态码
        private HttpStatus statusCode;
        // 响应体的字节数组
        private byte[] body;

        /**
         * 默认构造函数。
         */
        public Builder() {}

        /**
         * 使用现有的 HttpResponse 对象构造 Builder。
         *
         * @param response 现有的 HttpResponse 对象
         */
        public Builder(HttpResponse response) {
            this.version = response.version;
            this.statusCode = response.statusCode;
            this.headers.putAll(response.headers);
            this.body = response.body;
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
         * 设置 HTTP 状态码。
         *
         * @param statusCode HTTP 状态码
         * @return 当前 Builder 实例
         */
        public Builder statusCode(HttpStatus statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * 添加响应头。
         *
         * @param name 响应头名称
         * @param value 响应头值
         * @return 当前 Builder 实例
         */
        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        /**
         * 设置响应头的映射。
         *
         * @param headers 响应头的映射
         * @return 当前 Builder 实例
         */
        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        /**
         * 设置响应体的字节数组。
         *
         * @param body 响应体的字节数组
         * @return 当前 Builder 实例
         */
        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        /**
         * 构建 HttpResponse 对象。
         *
         * @return 构建的 HttpResponse 对象
         */
        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
}
