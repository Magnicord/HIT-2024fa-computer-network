package cn.edu.hit.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private final String version;
    private final HttpStatus statusCode; // HTTP状态码 (200, 304等)
    private final Map<String, String> headers; // HTTP响应头
    private final String body; // HTTP响应体

    private HttpResponse(Builder builder) {
        this.version = builder.version;
        this.statusCode = builder.statusCode;
        this.headers = builder.headers;
        this.body = builder.body;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static void main(String[] args) {
        HttpResponse response =
            HttpResponse.newBuilder().version(HttpConstant.HTTP_DEFAULT_VERSION).statusCode(HttpStatus.OK)
                .header("Content-Type", "application/json").header("Last-Modified", "Wed, 21 Oct 2015 07:28:00 GMT")
                .body("{\"name\": \"Aim\", " + "\"age\": 25}").build();

        System.out.println(response);
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

    public String getBody() {
        return body;
    }

    // 获取Last-Modified时间
    public String getLastModified() {
        return headers.getOrDefault("Last-Modified", null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String statusLine = String.format("%s %d %s\n", version, statusCode.getCode(), statusCode.getDescription());
        sb.append(statusLine);
        headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\n"));
        if (body != null && !body.isEmpty()) {
            sb.append("\n").append(body);
        }
        return sb.toString();
    }

    // Builder类
    public static class Builder {
        private final Map<String, String> headers = new HashMap<>();
        private String version = HttpConstant.HTTP_DEFAULT_VERSION; // 默认HTTP版本
        private HttpStatus statusCode;
        private String body;

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

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
}
