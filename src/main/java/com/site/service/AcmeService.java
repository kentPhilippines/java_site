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
    private static final String CERTBOT_WEBROOT = "/var/lib/letsencrypt/.well-known/acme-challenge";
    
    /**
     * 获取域名验证响应
     * @param token 验证token
     * @return 验证响应内容
     */
    public String getChallengeResponse(String token) {
        try {
            Path challengePath = Paths.get(CERTBOT_WEBROOT, token);
            if (Files.exists(challengePath)) {
                return Files.readString(challengePath);
            }
            log.warn("未找到验证文件: {}", challengePath);
            return null;
        } catch (IOException e) {
            log.error("读取验证文件失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
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
            cert.setCertType(SiteCertificate.TYPE_ACME);
            cert.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            certificateService.saveCertificate(cert);
            
            // 创建验证目录
            Files.createDirectories(Paths.get(CERTBOT_WEBROOT));
            
            try {
                // 申请证书
                String certbotCmd = String.format(
                    "certbot certonly --webroot " +
                    "--non-interactive " +
                    "--agree-tos " +
                    "--email %s " +
                    "--domain %s " +
                    "--webroot-path %s " +
                    "--preferred-challenges http " +
                    "--force-renewal " +
                    "--debug-challenges",
                    EMAIL, domain, CERTBOT_WEBROOT
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