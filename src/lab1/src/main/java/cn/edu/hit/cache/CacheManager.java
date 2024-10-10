package cn.edu.hit.cache;

import static org.apache.commons.io.FileUtils.deleteDirectory;

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

    private static final String CWD = System.getProperty("user.dir"); // 获取当前工作目录
    public static final String CACHE_DIRECTORY = CWD + File.separator + "cache"; // 缓存目录

    private static final long CACHE_EXPIRATION_SECONDS = 360000; // 缓存过期时间（100小时）
    private static final String CACHE_FILE_EXTENSION = ".cache"; // 缓存文件扩展名

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>(); // 缓存映射

    /**
     * 构造一个 CacheManager 并初始化缓存目录。
     */
    public CacheManager() {
        try {
            deleteDirectory(new File(CACHE_DIRECTORY)); // 删除缓存目录及其内容
            Files.createDirectories(Paths.get(CACHE_DIRECTORY)); // 创建缓存目录
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 为给定的 URI 生成一个唯一的缓存文件路径。
     *
     * @param uri 要生成缓存文件路径的 URI
     * @return 生成的缓存文件路径
     */
    private static String generateUniqueCacheFilePath(String uri) {
        String fileName = FileUtils.generateUniqueFileName(uri, CACHE_FILE_EXTENSION); // 生成唯一文件名
        return CACHE_DIRECTORY + File.separator + fileName; // 返回完整的缓存文件路径
    }

    /**
     * 如果存在且未过期，检索给定 URI 的缓存 HttpResponse。
     *
     * @param uri 要检索缓存响应的 URI
     * @return 缓存的 HttpResponse，如果未找到或已过期则返回 null
     * @throws IOException 如果发生 I/O 错误
     * @throws ClassNotFoundException 如果找不到序列化对象的类
     */
    public HttpResponse get(String uri) throws IOException, ClassNotFoundException {
        CacheEntry entry = cache.get(uri); // 从缓存中获取条目
        if (entry != null && !isExpired(entry)) { // 检查条目是否存在且未过期
            return FileUtils.readObject(entry.filePath(), HttpResponse.class); // 读取并返回缓存的 HttpResponse
        } else {
            return null; // 返回 null 表示未找到或已过期
        }
    }

    /**
     * 为指定的 URI 缓存给定的 HttpResponse。
     *
     * @param uri 要缓存响应的 URI
     * @param response 要缓存的 HttpResponse
     * @throws IOException 如果发生 I/O 错误
     */
    public void put(String uri, HttpResponse response) throws IOException {
        String filePath = generateUniqueCacheFilePath(uri); // 生成缓存文件路径
        FileUtils.writeObject(filePath, response); // 将 HttpResponse 写入文件
        cache.put(uri, new CacheEntry(filePath, response.getLastModified())); // 将缓存条目放入缓存映射
    }

    /**
     * 检查缓存条目是否已过期。
     *
     * @param entry 要检查的缓存条目
     * @return 如果缓存条目已过期则返回 true，否则返回 false
     */
    private boolean isExpired(CacheEntry entry) {
        long ageInSeconds = Duration.between(entry.lastModified(), ZonedDateTime.now()).getSeconds(); // 计算条目年龄
        return ageInSeconds > CACHE_EXPIRATION_SECONDS; // 判断是否超过过期时间
    }

    /**
     * 清理过期的缓存条目。
     */
    public void clearExpiredCache() {
        cache.entrySet().stream().filter(e -> isExpired(e.getValue())).forEach(e -> { // 过滤出过期条目
            try {
                Files.delete(Paths.get(e.getValue().filePath())); // 删除过期的缓存文件
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
