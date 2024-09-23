package cn.edu.hit.cache;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.utils.DateUtils;

public class CacheEntry {
    private static final DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME; // 日期格式 RFC 1123
    private final HttpResponse response; // 缓存的对象内容
    private final LocalDateTime lastModified; // 缓存对象的最后修改时间

    public CacheEntry(HttpResponse response) {
        this.response = response;
        String lastModified = response.getLastModified();
        if (lastModified != null) {
            this.lastModified = DateUtils.parseHttpDateTime(lastModified);
        } else {
            this.lastModified = null;
        }
    }

    public HttpResponse getResponse() {
        return response;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }
}
