package cn.edu.hit.core;

import static cn.edu.hit.utils.DateUtils.parseHttpDateTime;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse implements Serializable {

    private final String version;
    private final HttpStatus statusCode; // HTTP状态码 (200, 304等)
    private final Map<String, String> headers; // HTTP响应头
    private final byte[] body; // HTTP响应体

    private HttpResponse(Builder builder) {
        this.version = builder.version;
        this.statusCode = builder.statusCode;
        this.headers = builder.headers;
        this.body = builder.body;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public HttpResponse updateHeader(String headerName, String headerValue) {
        Builder builder = new Builder(this);
        builder.header(headerName, headerValue);
        return builder.build();
    }

    public String getVersion() {
        return version;
    }

    // 获取响应状态码
    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public byte[] getBody() {
        return body;
    }

    // 获取Last-Modified时间
    public ZonedDateTime getLastModified() {
        String timeStr = headers.getOrDefault("Last-Modified", null);
        return timeStr != null ? parseHttpDateTime(timeStr) : null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String statusLine = String.format("%s %d %s\r\n", version, statusCode.getCode(), statusCode.getDescription());
        sb.append(statusLine);
        headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\r\n"));
        sb.append("\r\n");
        return sb.toString();
    }

    // Builder类
    public static class Builder {
        private final Map<String, String> headers = new HashMap<>();
        private String version = HttpConstant.HTTP_DEFAULT_VERSION; // 默认HTTP版本
        private HttpStatus statusCode;
        private byte[] body;

        public Builder() {}

        public Builder(HttpResponse response) {
            this.version = response.version;
            this.statusCode = response.statusCode;
            this.headers.putAll(response.headers);
            this.body = response.body;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder statusCode(HttpStatus statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
}
