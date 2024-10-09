package cn.edu.hit.cache;

import java.time.ZonedDateTime;

/**
 * @param filePath 缓存文件的路径
 * @param lastModified 响应的最后修改时间
 */
public record CacheEntry(String filePath, ZonedDateTime lastModified) {
}
