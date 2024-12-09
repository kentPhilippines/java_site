package com.site.controller;

import com.site.config.ProxyConfig;
import com.site.util.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
public class IndexController {
    
    private final ProxyConfig proxyConfig;
    private final HttpUtils httpUtils;
    
    private static final String BASE_URL = "https://sci.ncut.edu.cn";
    private String string;
    @Value("${admin.path:#{T(java.util.UUID).randomUUID().toString()}}")
    private String adminPath;
     
    @GetMapping("/**")
    public String proxy(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = request.getRequestURI();
            
            // 如果是管理页面请求，直接返回null，让Spring继续处理
            if (path.startsWith(adminPath)) {
                return null;
            }
            
            String queryString = request.getQueryString();
            String fullUrl = BASE_URL + path;
            if (queryString != null && !queryString.isEmpty()) {
                fullUrl += "?" + queryString;
            }
            
            log.info("代理请求: {} -> {}", path, fullUrl);
            
            if (path.contains("login") || path.contains("auth")) {
                response.sendRedirect(fullUrl);
                return null;
            }
            
            if(isStaticResource(path)) {
                response.sendRedirect(fullUrl);
                return null;
            }
            string = httpUtils.get(fullUrl);
            return string;
            
        } catch (Exception e) {
            log.error("代理请求失败", e);
            return "Error: " + e.getMessage();
        }
    }
    
    private boolean isStaticResource(String path) {
        String lowercasePath = path.toLowerCase();
        return proxyConfig.getStaticExtensions().stream()
            .anyMatch(ext -> lowercasePath.endsWith(ext));
    }
} 