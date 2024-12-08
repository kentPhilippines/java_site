package com.site.util;

import org.springframework.stereotype.Component;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CacheUtil {
    
    // 内存缓存
    private final Map<String, CacheItem> memoryCache = new ConcurrentHashMap<>();
    // 缓存文件根目录
    private final String CACHE_DIR = "cache";
    // 缓存过期时间（1小时）
    private static final long EXPIRE_TIME = 3600000;
    // 清理任务执行间隔（5分钟）
    private static final long CLEAN_INTERVAL = 300000;
    
    public CacheUtil() {
        // 创建缓存目录
        try {
            Files.createDirectories(Paths.get(CACHE_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // 启动定时清理任务
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::cleanExpiredCache, CLEAN_INTERVAL, CLEAN_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 获取缓存内容
     */
    public String get(String key) {
        // 先从内存缓存获取
        CacheItem item = memoryCache.get(key);
        if (item != null && !item.isExpired()) {
            return item.getValue();
        }
        
        // 如果内存中没有，从文件获取
        String filePath = getFilePath(key);
        try {
            File cacheFile = new File(filePath);
            if (cacheFile.exists() && !isFileExpired(cacheFile)) {
                String content = new String(Files.readAllBytes(cacheFile.toPath()), StandardCharsets.UTF_8);
                // 放入内存缓存
                memoryCache.put(key, new CacheItem(content));
                return content;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 设置缓存内容
     */
    public void put(String key, String value) {
        // 存入内存缓存
        memoryCache.put(key, new CacheItem(value));
        
        // 存入文件
        String filePath = getFilePath(key);
        try {
            Files.write(Paths.get(filePath), value.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取缓存文件路径
     */
    private String getFilePath(String key) {
        // 将URL转换为合法的文件名
        String fileName = key.replaceAll("[^a-zA-Z0-9]", "_");
        return CACHE_DIR + File.separator + fileName;
    }
    
    /**
     * 检查文件是否过期
     */
    private boolean isFileExpired(File file) {
        return System.currentTimeMillis() - file.lastModified() > EXPIRE_TIME;
    }
    
    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        // 清理内存缓存
        memoryCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        
        // 清理文件缓存
        try {
            Files.walk(Paths.get(CACHE_DIR))
                .filter(Files::isRegularFile)
                .map(Path::toFile)
                .filter(this::isFileExpired)
                .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 缓存项内部类
     */
    private static class CacheItem {
        private final String value;
        private final long timestamp;
        
        public CacheItem(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRE_TIME;
        }
    }
}
