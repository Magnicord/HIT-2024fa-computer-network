package cn.edu.hit.cache;

import java.time.LocalDateTime;

import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.utils.DateUtils;

public class CacheEntry {
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
