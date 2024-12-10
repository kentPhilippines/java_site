package com.site.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;

@Slf4j
@Service
public class NginxService {

    @Value("${nginx.base-path:/opt/java_site/nginx}")
    private String nginxBasePath;

    @Value("${nginx.cert-path:/opt/java_site/certs}")
    private String certPath;

    public void generateSiteConfig(String domain) {
        try {
            String confPath = nginxBasePath + "/conf/conf.d/" + domain + ".conf";
            String config = generateNginxConfig(domain);
            
            // 确保目录存在
            new File(nginxBasePath + "/conf/conf.d").mkdirs();
            
            // 写入配置文件
            try (FileWriter writer = new FileWriter(confPath)) {
                writer.write(config);
            }
            
            // 设置证书文件权限
            setCertificatePermissions(domain);
            
            log.info("已生成站点 {} 的Nginx配置: {}", domain, confPath);
            
            // 重新加载Nginx配置
            reloadNginx();
            
        } catch (Exception e) {
            log.error("生成Nginx配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成Nginx配置失败: " + e.getMessage());
        }
    }

    public void deleteSiteConfig(String domain) {
        try {
            String confPath = nginxBasePath + "/conf/conf.d/" + domain + ".conf";
            Files.deleteIfExists(Paths.get(confPath));
            
            log.info("已删除站点 {} 的Nginx配置", domain);
            
            // 重新加载Nginx配置
            reloadNginx();
        } catch (Exception e) {
            log.error("删除Nginx配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("删除Nginx配置失败: " + e.getMessage());
        }
    }

    private void reloadNginx() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                "nginx", "-s", "reload", "-c", nginxBasePath + "/conf/nginx.conf"
            });
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Nginx重新加载失败，退出码: " + exitCode);
            }
            log.info("Nginx配置已重新加载");
        } catch (Exception e) {
            log.error("重新加载Nginx配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("重新加载Nginx配置失败: " + e.getMessage());
        }
    }

    private void setCertificatePermissions(String domain) {
        try {
            String certDir = certPath + "/" + domain;
            String[] certFiles = {"cert.pem", "chain.pem", "keystore.p12", "privkey.pem", "fullchain.pem"};
            
            // 设置目录权限
            Process process = Runtime.getRuntime().exec(new String[]{
                "sh", "-c", "chown -R nginx:nginx " + certDir + " && chmod -R 755 " + certDir
            });
            process.waitFor();
            
            // 设置文件权限
            for (String certFile : certFiles) {
                String filePath = certDir + "/" + certFile;
                File file = new File(filePath);
                if (file.exists()) {
                    process = Runtime.getRuntime().exec(new String[]{
                        "sh", "-c", "chown nginx:nginx " + filePath + " && chmod 644 " + filePath
                    });
                    process.waitFor();
                }
            }
            
            log.info("已设置证书文件权限: {}", certDir);
        } catch (Exception e) {
            log.error("设置证书权限失败: {}", e.getMessage(), e);
        }
    }

    private String generateNginxConfig(String domain) {
        StringBuilder config = new StringBuilder();
        config.append("server {\n")
             .append("    listen 80;\n")
             .append(String.format("    server_name %s;\n", domain))
             .append("    return 301 https://$host$request_uri;\n")
             .append("}\n\n")
             .append("server {\n")
             .append("    listen 443 ssl http2;\n")
             .append(String.format("    server_name %s;\n\n", domain))
             .append("    # SSL证书配置\n")
             .append(String.format("    ssl_certificate %s/%s/fullchain.pem;\n", certPath, domain))
             .append(String.format("    ssl_certificate_key %s/%s/privkey.pem;\n", certPath, domain))
             .append("\n")
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
}
