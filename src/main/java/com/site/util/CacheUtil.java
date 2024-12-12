package com.site.util;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.site.entity.Site;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheUtil {

    private static final String CACHE_DIR = "cache";

    public void put(String key, String value, Site site) {
        try {
            String filePath = getFilePath(key, site);
            createDirectories(filePath);
            Files.write(Paths.get(filePath), value.getBytes(StandardCharsets.UTF_8));
            log.info("缓存已保存到: {}", filePath);
        } catch (IOException e) {
            log.error("保存缓存文件失败", e);
        }
    }

    public void putBytes(String key, byte[] value, Site site) {
        try {
            String filePath = getFilePath(key, site);
            createDirectories(filePath);
            Files.write(Paths.get(filePath), value);
            log.info("二进制文件已缓存: {}", filePath);
        } catch (IOException e) {
            log.error("保存二进制文件失败", e);
        }
    }

    private void createDirectories(String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                throw new IOException("无法创建目录: " + parent.getAbsolutePath());
            }
        }
    }

    public byte[] getBytes(String key, Site site) {
        try {
            String filePath = getFilePath(key, site);
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return null;
            }

            long fileSize = Files.size(path);
            
            // 小文件（小于8KB）直接读取
            if (fileSize < 8192) {
                return Files.readAllBytes(path);
            }
            
            // 中等文件（小于2MB）使用缓冲流分块读取
            if (fileSize < 2 * 1024 * 1024) {
                byte[] data = new byte[(int) fileSize];
                try (BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(path))) {
                    int offset = 0;
                    int numRead;
                    while (offset < data.length && (numRead = bis.read(data, offset, data.length - offset)) >= 0) {
                        offset += numRead;
                    }
                }
                return data;
            }
            
            // 大文件使用零拷贝
            try (FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.READ)) {
                ByteBuffer buffer = ByteBuffer.allocateDirect((int) fileSize);
                while (buffer.hasRemaining()) {
                    if (fileChannel.read(buffer) == -1) {
                        break;
                    }
                }
                buffer.flip();
                byte[] data = new byte[buffer.limit()];
                buffer.get(data);
                return data;
            }
        } catch (IOException e) {
            log.error("读取二进制文件失败: {}", key, e);
            return null;
        }
    }

    private String getFilePath(String key, Site site) {
        try {
            // 移除协议和域名部分，只保留路径
            String relativePath = key.replaceFirst("^https?://[^/]+", "");
            
            // 处理根路径和空路径的情况
            if (relativePath.isEmpty() || relativePath.equals("/")) {
                relativePath = "/index.html";
            } else if (relativePath.endsWith("/")) {
                relativePath = relativePath + "index.html";
            } else if (!relativePath.contains(".")) {
                // 如果路径没有扩展名，认为是目录，添加index.html
                relativePath = relativePath + "/index.html";
            }
            
            // 确保路径安全，移除 .. 等危险字符
            relativePath = relativePath.replaceAll("[^a-zA-Z0-9./\\-_]", "_");
            // 处理Windows路径问题
            relativePath = relativePath.replace('/', File.separatorChar);
            
            // 使用绝对路径
            return new File(CACHE_DIR,  File.separator + relativePath).getAbsolutePath();
        } catch (Exception e) {
            log.error("生成文件路径失败: {}", e.getMessage());
            // 降级处理：使用简单文件名
            return new File(CACHE_DIR, site.getName() + File.separator +
                    key.replaceAll("[^a-zA-Z0-9.]", "_")).getAbsolutePath();
        }
    }
    public String get(String key, Site site) {
        byte[] data = getBytes(key, site);
        return data != null ? new String(data, StandardCharsets.UTF_8) : null;
    }
}
