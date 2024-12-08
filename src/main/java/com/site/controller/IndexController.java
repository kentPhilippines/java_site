package com.site.controller;

import com.site.config.ProxyConfig;
import com.site.service.SiteConfigService;
import com.site.util.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class IndexController {
    
    private final ProxyConfig proxyConfig;
    private final HttpUtils httpUtils;
    private final SiteConfigService configService;

    
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
            log.info("路径为: {}", path);
            log.info("完整路径为: {}", fullUrl);
            if(isStaticResource(path)){
                //重定向到原始地址
                response.sendRedirect(fullUrl);
                return null;
            }
            
            return httpUtils.get(fullUrl);
            
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    private boolean isStaticResource(String path) {
        String lowercasePath = path.toLowerCase();
        return proxyConfig.getStaticExtensions().stream()
            .anyMatch(ext -> lowercasePath.endsWith(ext));
    }
} 