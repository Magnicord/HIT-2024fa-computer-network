package cn.edu.hit.core;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private final String method;
    private final URI uri;
    private final String version;
    private final Map<String, String> headers;
    private final String body;

    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.uri = builder.uri;
        this.headers = builder.headers;
        this.body = builder.body;
        this.version = builder.version;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    // 示例用法
    public static void main(String[] args) {
        HttpRequest request = HttpRequest.newBuilder().method(HttpConstant.POST).uri(URI.create("""
            https://example.com/api/resource""")).version(HttpConstant.HTTP_DEFAULT_VERSION)
            .header("Content-Type", "application/json").body("{\"key\": \"value\"}").build();

        System.out.println(request);
    }

    public String getScheme() {
        String scheme = uri.getScheme();
        if (scheme == null) {
            scheme = version.split("/")[0];
        }
        return scheme;
    }

    public String getMethod() {
        return method;
    }

    public URI getUri() {
        return uri;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getBody() {
        return body;
    }

    public String getHost() {
        return headers.getOrDefault("Host", uri.getHost());
    }

    public int getPort() {
        // 尝试从Host头部获取端口
        String host = getHost();
        if (host.contains(":")) {
            String[] parts = host.split(":");
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                // 如果端口号不是数字，则返回-1
                return -1;
            }
        }

        // 从URI中获取端口
        int portFromUri = uri.getPort();
        if (portFromUri != -1) {
            return portFromUri; // 返回URI中指定的端口
        }

        // 返回默认端口
        if ("https".equalsIgnoreCase(uri.getScheme())) {
            return HttpConstant.HTTPS_DEFAULT_PORT; // HTTPS默认端口
        } else {
            return HttpConstant.HTTP_DEFAULT_PORT; // HTTP默认端口
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(uri.toString()).append(" ").append(version).append("\r\n");
        headers.forEach((key, value) -> sb.append(key).append(": ").append(value).append("\r\n"));
        if (body != null && !body.isEmpty()) {
            sb.append("\r\n").append(body);
        }
        return sb.toString();
    }

    public static class Builder {
        private final Map<String, String> headers = new HashMap<>();
        private String version = HttpConstant.HTTP_DEFAULT_VERSION; // 默认HTTP版本
        private String method;
        private URI uri;
        private String body;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder uri(URI uri) {
            this.uri = uri;
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

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }
}