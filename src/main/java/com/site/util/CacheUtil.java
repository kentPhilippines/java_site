package com.site.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import com.site.entity.Site;
import com.site.service.SiteService;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheUtil {

    private final Map<String, CacheItem> memoryCache = new ConcurrentHashMap<>();
    private static final String CACHE_DIR = "cache";
    private static final long EXPIRE_TIME = TimeUnit.HOURS.toMillis(1);  // 1小时过期
    private final SiteService siteService;

    public void put(String key, String value, Site site) {
        memoryCache.put(key, new CacheItem(value));
        try {
            String baseDir = generateBasePath(site);
            String filePath = getFilePath(key, baseDir);
            Files.createDirectories(Paths.get(filePath).getParent());
            Files.write(Paths.get(filePath), value.getBytes(StandardCharsets.UTF_8));
            log.info("缓存已保存到: {}", filePath);
        } catch (IOException e) {
            log.error("保存缓存文件失败", e);
        }
    }

    public void putBytes(String key, byte[] value, Site site) {
        try {
            String baseDir = generateBasePath(site);
            String filePath = getFilePath(key, baseDir);
            Files.createDirectories(Paths.get(filePath).getParent());
            Files.write(Paths.get(filePath), value);
            log.info("二进制文件已缓存: {}", filePath);
        } catch (IOException e) {
            log.error("保存二进制文件失败", e);
        }
    }

    public byte[] getBytes(String key, Site site) {
        try {
            String baseDir = generateBasePath(site);
            String filePath = getFilePath(key, baseDir);
            if (Files.exists(Paths.get(filePath))) {
                return Files.readAllBytes(Paths.get(filePath));
            }
        } catch (IOException e) {
            log.error("读取二进制文件失败", e);
        }
        return null;
    }

    public String get(String key, Site site) {
        // 先从内存缓存获取
        CacheItem item = memoryCache.get(key);
        if (item != null && !item.isExpired()) {
            return item.getValue();
        }

        // 如果内存中没有或已过期，从文件获取
        try {
            String baseDir = generateBasePath(site);
            String filePath = getFilePath(key, baseDir);
            if (Files.exists(Paths.get(filePath))) {
                String content = new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
                memoryCache.put(key, new CacheItem(content));
                return content;
            }
        } catch (IOException e) {
            log.error("读取缓存文件失败", e);
        }

        return null;
    }

    private String generateBasePath(Site site) {
        return CACHE_DIR + File.separator + site.getName() + File.separator;
    }

    private String getFilePath(String key, String baseDir) {
        try {
            // 移除协议和域名部分，只保留路径
            String relativePath = key.replaceFirst("^https?://[^/]+", "");
            if (relativePath.isEmpty()) {
                relativePath = "index.html";
            }
            // 确保路径安全，移除 .. 等危险字符
            relativePath = relativePath.replaceAll("[^a-zA-Z0-9./\\-_]", "_");
            return baseDir + relativePath;
        } catch (Exception e) {
            log.error("生成文件路径失败", e);
            // 降级处理：使用简单文件名
            return baseDir + File.separator + 
                    key.replaceAll("[^a-zA-Z0-9.]", "_");
        }
    }

    @Data
    private static class CacheItem {
        private final String value;
        private final long timestamp;

        public CacheItem(String value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > EXPIRE_TIME;
        }
    }
}
