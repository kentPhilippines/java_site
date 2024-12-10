package com.site.service;

import com.site.entity.Site;
import com.site.entity.SiteCertificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcmeService {
    
    private final CertificateService certificateService;
    private final NginxService nginxService;
    private static final String CERT_BASE_PATH = "certs";
    private static final String EMAIL = "admin@example.com"; // 配置你的邮箱
    
    public void requestCertificate(Site site) {
        String domain = site.getName().replaceAll("https?://", "");
        
        try {
            log.info("开始为域名 {} 申请证书", domain);
            
            // 创建证书目录
            Path certPath = Paths.get(CERT_BASE_PATH, domain);
            Files.createDirectories(certPath);
            
            // 创建证书记录
            SiteCertificate cert = new SiteCertificate();
            cert.setSiteId(site.getId());
            cert.setDomain(domain);
            cert.setStatus(SiteCertificate.STATUS_PENDING);
            cert.setAutoRenew(true);
            cert.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            certificateService.saveCertificate(cert);
            
            // 停止Nginx
            log.info("停止Nginx服务");
            executeCommand("systemctl stop nginx");
            
            try {
                // 申请证书
                String certbotCmd = String.format(
                    "certbot certonly --standalone " +
                    "--non-interactive " +
                    "--agree-tos " +
                    "--email %s " +
                    "--domain %s " +
                    "--cert-path %s " +
                    "--preferred-challenges http " +
                    "--http-01-port 80 " +
                    "--force-renewal " +
                    "--debug-challenges",
                    EMAIL, domain, certPath.toAbsolutePath()
                );
                
                log.info("执行certbot命令: {}", certbotCmd);
                Process process = Runtime.getRuntime().exec(certbotCmd);
                
                // 读取输出
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("Certbot输出: {}", line);
                    }
                }
                
                // 读取错误
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.warn("Certbot错误: {}", line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("Certbot命令执行失败，退出码: " + exitCode);
                }
                
                // 检查证书文件
                String fullchainPath = CERT_BASE_PATH + "/" + domain + "/fullchain.pem";
                String privkeyPath = CERT_BASE_PATH + "/" + domain + "/privkey.pem";
                String chainPath = CERT_BASE_PATH + "/" + domain + "/chain.pem";
                
                if (!Files.exists(Paths.get(fullchainPath)) || !Files.exists(Paths.get(privkeyPath))) {
                    throw new RuntimeException("证书文件未生成");
                }
                
                // 更新证书记录
                cert.setStatus(SiteCertificate.STATUS_ACTIVE);
                cert.setCertFile(fullchainPath);
                cert.setKeyFile(privkeyPath);
                cert.setChainFile(chainPath);
                certificateService.saveCertificate(cert);
                
                // 生成Nginx配置
                nginxService.generateSiteConfig(domain);
                
                log.info("证书申请成功: {}", domain);
                
            } finally {
                // 重启Nginx
                try {
                    log.info("重启Nginx服务");
                    executeCommand("systemctl start nginx");
                } catch (Exception e) {
                    log.error("重启Nginx失败: {}", e.getMessage(), e);
                }
            }
            
        } catch (Exception e) {
            log.error("证书申请失败: {}", e.getMessage(), e);
            throw new RuntimeException("证书申请失败: " + e.getMessage(), e);
        }
    }
    
    private void executeCommand(String command) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("命令执行失败: " + command + ", 退出码: " + exitCode);
        }
    }
} 