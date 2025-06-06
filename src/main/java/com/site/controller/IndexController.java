package com.site.controller;

import com.site.config.ProxyConfig;
import com.site.entity.Site;
import com.site.util.HttpUtils;
import com.site.util.CacheUtil;
import com.site.service.SiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Enumeration;

@Slf4j
@RestController
@RequiredArgsConstructor
public class IndexController {

    private final ProxyConfig proxyConfig;
    private final HttpUtils httpUtils;
    private final CacheUtil cacheUtil;
    private final SiteService siteService;
    @Value("${admin.path:#{T(java.util.UUID).randomUUID().toString()}}")
    private String adminPath;

    private byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[4096];
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }

    @GetMapping("/**")
    public String proxyGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = request.getRequestURI();
            log.info("请求路径: {}", path);
            String host = request.getHeader("Host");
            log.info("请求主机: {}, 路径: {}", host, path);
            Site site = siteService.getSiteByUrl(host);
            if (site == null) {
                log.error("站点不存在: {}", host);
                return "Error: 站点不存在";
            }
            log.info("匹配到站点: {}, 目标URL: {}", site.getName(), site.getUrl());
            if (path.startsWith(adminPath)) {
                return null;
            }
            String fullUrl = host + path;
            // 判断是否为静态资源
            if (isStaticResource(path)) {
                handleStaticResource(host, path, site, response);
                return null;
            }
            // 特殊处理favicon.ico
            if (path.equals("/favicon.ico")) {
                handleFaviconRequest(site, response);
                return null;
            }
            // 处理非静态资源
            if (site != null && site.getIsCache() == 1) {
                String cachedContent = cacheUtil.get(fullUrl, site);
                if (cachedContent != null) {
                    log.info("从缓存获取内容: {}", fullUrl);
                    return cachedContent;
                }
            }
            fullUrl = site.getUrl() + path;
            log.info("代理请求: {} -> {}", path, fullUrl);
            String responseContent = httpUtils.get(fullUrl);
            if (site != null && site.getIsCache() == 1) {
                cacheUtil.put(host + path, responseContent, site);
                log.info("内容已缓存: {}", fullUrl);
            }
            return responseContent;
        } catch (Exception e) {
            log.error("代理请求失败", e);
            return "Error: " + e.getMessage();
        }
    }

    private void handleFaviconRequest(Site site, HttpServletResponse response) throws Exception {
        String faviconPath = "/favicon.ico";
        String cacheKey = site.getName() + faviconPath;
        // 先尝试从缓存获取
        byte[] cachedFavicon = cacheUtil.getBytes(cacheKey, site);
        if (cachedFavicon != null) {
            response.setContentType("image/x-icon");
            response.getOutputStream().write(cachedFavicon);
            return;
        }
        // 缓存未命中，从源站获取
        String faviconUrl = site.getUrl() + faviconPath;
        try {
            URL url = new URL(faviconUrl);
            try (InputStream is = url.openStream()) {
                byte[] faviconBytes = readAllBytes(is);
                cacheUtil.putBytes(cacheKey, faviconBytes, site);
                response.setContentType("image/x-icon");
                response.getOutputStream().write(faviconBytes);
            }
        } catch (Exception e) {
            log.warn("获取favicon失败: {}", faviconUrl, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void handleStaticResource(String host, String path, Site site, HttpServletResponse response)
            throws Exception {
        String cacheKey = host + path;
        String fullUrl = site.getUrl() + path;
        // 先尝试从缓存获取
        byte[] cachedContent = cacheUtil.getBytes(cacheKey, site);
        if (cachedContent != null) {
            response.setContentType(getContentType(path));
            response.getOutputStream().write(cachedContent);
            return;
        }
        // 缓存未命中，从源站获取并缓存
        try {
            URL url = new URL(fullUrl);
            try (InputStream is = url.openStream()) {
                byte[] resourceBytes = readAllBytes(is);
                cacheUtil.putBytes(cacheKey, resourceBytes, site);
                response.setContentType(getContentType(path));
                response.getOutputStream().write(resourceBytes);
            }
        } catch (Exception e) {
            log.error("获取静态资源失败: {}", fullUrl, e);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private String getContentType(String path) {
        String extension = path.substring(path.lastIndexOf(".")).toLowerCase();
        switch (extension) {
            case ".jpg":
            case ".jpeg":
                return "image/jpeg";
            case ".png":
                return "image/png";
            case ".gif":
                return "image/gif";
            case ".css":
                return "text/css";
            case ".js":
                return "application/javascript";
            case ".pdf":
                return "application/pdf";
            case ".mp4":
                return "video/mp4";
            case ".mp3":
                return "audio/mpeg";
            case ".ico":
                return "image/x-icon";
            default:
                return "application/octet-stream";
        }
    }

    private boolean isStaticResource(String path) {
        if (path.equals("/favicon.ico")) {
            return true;
        }
        String lowercasePath = path.toLowerCase();
        return proxyConfig.getStaticExtensions().stream()
                .anyMatch(ext -> lowercasePath.endsWith(ext));
    }

    @PostMapping("/**")
    public void proxyPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = request.getRequestURI();
            log.info("请求路径: {}", path);
            String host = request.getHeader("Host");
            log.info("请求主机: {}, 路径: {}", host, path);
            Site site = siteService.getSiteByUrl(host);
            if (site == null) {
                log.error("站点不存在: {}", host);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            log.info("匹配到站点: {}, 目标URL: {}", site.getName(), site.getUrl());
            if (path.startsWith(adminPath)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            // 构建目标URL
            String fullUrl = site.getUrl() + path;
            log.info("代理请求: {} -> {}", path, fullUrl);
            // 读取请求体
            String requestBody = null;
            try (BufferedReader reader = request.getReader()) {
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                requestBody = builder.toString();
            }
            // 转发请求
            URL url = new URL(fullUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            // 复制请求头
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                String headerValue = request.getHeader(headerName);
                // 跳过某些特殊的请求头
                if (!"host".equalsIgnoreCase(headerName) && 
                    !"content-length".equalsIgnoreCase(headerName)) {
                    conn.setRequestProperty(headerName, headerValue);
                }
            }
            // 设置通用请求头
            conn.setRequestProperty("Content-Type", request.getContentType());
            conn.setRequestProperty("Accept", "*/*");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            // 写入请求体
            if (requestBody != null && !requestBody.isEmpty()) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.getBytes("UTF-8");
                    os.write(input, 0, input.length);
                }
            }
            // 获取响应
            int responseCode = conn.getResponseCode();
            response.setStatus(responseCode);
            // 复制响应头
            conn.getHeaderFields().forEach((key, values) -> {
                if (key != null) {  // 跳过状态行
                    values.forEach(value -> response.addHeader(key, value));
                }
            });
            // 复制响应体
            try (InputStream is = responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
                 OutputStream os = response.getOutputStream()) {
                if (is != null) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = is.read(buffer)) != -1) {
                        os.write(buffer, 0, length);
                    }
                }
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("代理请求失败", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}