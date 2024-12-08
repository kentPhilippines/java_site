package com.site.util;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
@RequiredArgsConstructor
public class HttpUtils {
    
    private final CacheUtil cacheUtil;
    
    public String get(String urlStr) {
        // 先尝试从缓存获取
        String cachedContent = cacheUtil.get(urlStr);
        if (cachedContent != null) {
            return cachedContent;
        }
        
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            
            BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            String content = response.toString();
            // 存入缓存
            cacheUtil.put(urlStr, content);
            
            return content;
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
} 