package com.site.service;

import com.site.entity.Site;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NginxService {

    @Value("${nginx.conf-path:/opt/java_site/nginx}")
    private String nginxConfPath;

    @Value("${nginx.cert-path:/opt/java_site/certs}")
    private String certPath;


    /**
     * 为站点生成Nginx配置
     */
    public void generateSiteConfig(String domain) {

        try {
            String configPath = nginxConfPath + "/conf.d/" + domain + ".conf";
            
            // 创建配置目录
            new File(nginxConfPath + "/conf.d").mkdirs();
            
            // 生成配置文件
            String config = generateNginxConfig(domain);
            Files.write(Paths.get(configPath), config.getBytes());
            
            log.info("已生成站点 {} 的Nginx配置: {}", domain, configPath);
            
            // 重新加载Nginx配置
            reloadNginx();
        } catch (Exception e) {
            log.error("生成Nginx配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成Nginx配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除站点的Nginx配置
     */
    public void deleteSiteConfig(String domain) {
        try {
            String configPath = nginxConfPath + "/conf.d/" + domain + ".conf";
            Files.deleteIfExists(Paths.get(configPath));
            
            log.info("已删除站点 {} 的Nginx配置", domain);
            
            // 重新加��Nginx配置
            reloadNginx();
        } catch (Exception e) {
            log.error("删除Nginx配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除Nginx配置失败: " + e.getMessage());
        }
    }

  

    /**
     * 生成Nginx配置内容
     */
    private String generateNginxConfig(String domain) {
        StringBuilder config = new StringBuilder();
        config.append("# HTTP server\n")
             .append("server {\n")
             .append("    listen 80;\n")
             .append(String.format("    server_name %s;\n", domain))
             .append("    return 301 https://$host$request_uri;\n")
             .append("}\n\n")
             .append("# HTTPS server\n")
             .append("server {\n")
             .append("    listen 443 ssl http2;\n")
             .append(String.format("    server_name %s;\n\n", domain))
             .append("    # SSL证书配置\n")
             .append(String.format("    ssl_certificate %s/%s/fullchain.pem;\n", certPath, domain))
             .append(String.format("    ssl_certificate_key %s/%s/privkey.pem;\n", certPath, domain))
             .append(String.format("    ssl_trusted_certificate %s/%s/chain.pem;\n\n", certPath, domain))
             .append("    # OCSP Stapling\n")
             .append("    ssl_stapling on;\n")
             .append("    ssl_stapling_verify on;\n")
             .append("    resolver 8.8.8.8 8.8.4.4 valid=300s;\n")
             .append("    resolver_timeout 5s;\n\n")
             .append("    # 安全相关头部\n")
             .append("    add_header Strict-Transport-Security \"max-age=31536000; includeSubDomains\" always;\n")
             .append("    add_header X-Frame-Options SAMEORIGIN;\n")
             .append("    add_header X-Content-Type-Options nosniff;\n")
             .append("    add_header X-XSS-Protection \"1; mode=block\";\n\n")
             .append("    # 反向代理到Java应用\n")
             .append("    location / {\n")
             .append("        proxy_pass http://localhost:9090;\n")
             .append("        proxy_set_header Host $host;\n")
             .append("        proxy_set_header X-Real-IP $remote_addr;\n")
             .append("        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;\n")
             .append("        proxy_set_header X-Forwarded-Proto $scheme;\n\n")
             .append("        # WebSocket支持\n")
             .append("        proxy_http_version 1.1;\n")
             .append("        proxy_set_header Upgrade $http_upgrade;\n")
             .append("        proxy_set_header Connection \"upgrade\";\n\n")
             .append("        # 超时设置\n")
             .append("        proxy_connect_timeout 60s;\n")
             .append("        proxy_send_timeout 60s;\n")
             .append("        proxy_read_timeout 60s;\n")
             .append("    }\n\n")
             .append("    # 静态文件缓存\n")
             .append("    location ~* \\.(jpg|jpeg|png|gif|ico|css|js)$ {\n")
             .append("        proxy_pass http://localhost:9090;\n")
             .append("        proxy_set_header Host $host;\n")
             .append("        expires 30d;\n")
             .append("        add_header Cache-Control \"public, no-transform\";\n")
             .append("    }\n")
             .append("}\n");
        
        return config.toString();
    }

    /**
     * 重新加载Nginx配置
     */
    private void reloadNginx() {
        try {
            Process process = Runtime.getRuntime().exec("nginx -p " + System.getProperty("user.dir") + " -c " + nginxConfPath + "/nginx.conf -s reload");
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Nginx重新加载失败，退出码: " + exitCode);
            }
            log.info("Nginx��置已重新加载");
        } catch (Exception e) {
            log.error("重新加载Nginx配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("重新加载Nginx配置失败: " + e.getMessage());
        }
    }
} 