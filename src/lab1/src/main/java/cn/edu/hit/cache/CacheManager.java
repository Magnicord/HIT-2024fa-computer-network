package cn.edu.hit.cache;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheManager {
    private static final long CACHE_EXPIRATION_SECONDS = 3600; // 缓存过期时间，1小时

    // 使用线程安全的ConcurrentHashMap作为缓存
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    // 从缓存中获取对象
    public CacheEntry get(String url) {
        CacheEntry entry = cache.get(url);
        if (entry != null && !isExpired(entry)) {
            return entry;
        }
        return null; // 缓存不存在或过期
    }

    // 存储对象到缓存
    public void put(String url, CacheEntry entry) {
        cache.put(url, entry);
    }

    // 判断缓存是否过期
    private boolean isExpired(CacheEntry entry) {
        long ageInSeconds = Duration.between(entry.getLastModified(), LocalDateTime.now()).getSeconds();
        return ageInSeconds > CACHE_EXPIRATION_SECONDS;
    }

    // 清理过期缓存
    public void clearExpiredCache() {
        cache.entrySet().removeIf(e -> isExpired(e.getValue()));
    }
}
