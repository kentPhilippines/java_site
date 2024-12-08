package com.site.controller;

import com.site.config.ProxyConfig;
import com.site.util.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    
    @GetMapping("/**")
    public String proxy(HttpServletRequest request, HttpServletResponse response) {
        try {
            String path = request.getRequestURI();
            String queryString = request.getQueryString();
            
            String fullUrl = BASE_URL + path;
            if (queryString != null && !queryString.isEmpty()) {
                fullUrl += "?" + queryString;
            }
            
            log.info("代理请求: {} -> {}", path, fullUrl);
            
            if(isStaticResource(path)) {
                response.sendRedirect(fullUrl);
                return null;
            }
            
            return httpUtils.get(fullUrl);
            
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