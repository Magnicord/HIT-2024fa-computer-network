package cn.edu.hit.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.edu.hit.core.HttpResponse;
import cn.edu.hit.utils.FileUtils;

public class CacheManager {
    private static final String CWD = System.getProperty("user.dir");
    public static final String CACHE_DIRECTORY = CWD + File.separator + "cache"; // 缓存目录

    private static final long CACHE_EXPIRATION_SECONDS = 360000; // 缓存过期时间（100小时）
    private static final String CACHE_FILE_EXTENSION = ".cache";

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public CacheManager() {
        try {
            Files.createDirectories(Paths.get(CACHE_DIRECTORY));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateUniqueCacheFilePath(String uri) {
        String fileName = FileUtils.generateUniqueFileName(uri, CACHE_FILE_EXTENSION);
        return CACHE_DIRECTORY + File.separator + fileName;
    }

    public HttpResponse get(String uri) throws IOException, ClassNotFoundException {
        CacheEntry entry = cache.get(uri);
        if (entry != null && !isExpired(entry)) {
            return FileUtils.readObject(entry.filePath(), HttpResponse.class);
        } else {
            return null;
        }
    }

    public void put(String uri, HttpResponse response) throws IOException {
        String filePath = generateUniqueCacheFilePath(uri);
        FileUtils.writeObject(filePath, response);
        cache.put(uri, new CacheEntry(filePath, response.getLastModified()));
    }

    // 检查缓存是否已过期
    private boolean isExpired(CacheEntry entry) {
        long ageInSeconds = Duration.between(entry.lastModified(), ZonedDateTime.now()).getSeconds();
        return ageInSeconds > CACHE_EXPIRATION_SECONDS;
    }

    // 清理过期的缓存
    public void clearExpiredCache() {
        cache.entrySet().stream().filter(e -> isExpired(e.getValue())).forEach(e -> {
            try {
                Files.delete(Paths.get(e.getValue().filePath()));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
