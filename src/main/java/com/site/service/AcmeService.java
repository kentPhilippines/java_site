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
    private static final String EMAIL = "admin@example.com";
    private static final String REQUEST_CERT_SCRIPT = "scripts/request-cert.sh";
    private static final String CERTBOT_WEBROOT = "/var/lib/letsencrypt/.well-known/acme-challenge";
    
    /**
     * 设置文件执行权限
     */
    private void setExecutablePermission(Path path) throws IOException {
        File file = path.toFile();
        // 确保文件存在
        if (!file.exists()) {
            throw new RuntimeException("脚本文件不存在: " + path);
        }
        
        // 设置所有者执行权限
        if (!file.setExecutable(true, false)) {
            log.warn("无法设置文件执行权限: {}", path);
        }
        
        // 设置读取权限
        if (!file.setReadable(true, false)) {
            log.warn("无法设置文件读取权限: {}", path);
        }
    }

    /**
     * 检查命令是否存在
     */
    private boolean checkCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"which", cmd});
            return process.waitFor() == 0;
        } catch (Exception e) {
            log.warn("命令{}不存在: {}", cmd, e.getMessage());
            return false;
        }
    }

    /**
     * 执行命令并返回输出
     */
    private String executeCommandWithOutput(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
            .redirectErrorStream(true)
            .start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info(line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("命令执行失败: " + String.join(" ", command) + 
                "\n输出: " + output.toString());
        }
        
        return output.toString();
    }

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
            return null;
        } catch (IOException e) {
            log.error("读取challenge文件失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    public void requestCertificate(Site site) {
        String domain = site.getUrl().replaceAll("https?://", "");
        
        try {
            log.info("开始为域名 {} 申请证书", domain);
            
            // 创建证书记录
            SiteCertificate cert = new SiteCertificate();
            cert.setSiteId(site.getId());
            cert.setDomain(domain);
            cert.setStatus(SiteCertificate.STATUS_PENDING);
            cert.setAutoRenew(true);
            cert.setCertType(SiteCertificate.TYPE_ACME);
            cert.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            certificateService.saveCertificate(cert);
            
            try {
                // 设置脚本执行权限
                Path scriptPath = Paths.get(REQUEST_CERT_SCRIPT);
                setExecutablePermission(scriptPath);
                
                // 执行证书申请脚本
                ProcessBuilder pb = new ProcessBuilder(
                    "sh",
                    REQUEST_CERT_SCRIPT,
                    domain
                );
                pb.redirectErrorStream(true);
                
                // 设置工作目录为脚本所在目录
                Path scriptDir = Paths.get(REQUEST_CERT_SCRIPT).getParent();
                if (scriptDir != null) {
                    pb.directory(scriptDir.toFile());
                }
                
                log.info("执行命令: sh {} {} {}", REQUEST_CERT_SCRIPT, domain, EMAIL);
                
                Process process = pb.start();
                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        log.info(line);
                    }
                }
                
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    throw new RuntimeException("证书申请失败:\n" + output.toString());
                }
                
                // 检查证书文件
                String fullchainPath = CERT_BASE_PATH + "/" + domain + "/fullchain.pem";
                String privkeyPath = CERT_BASE_PATH + "/" + domain + "/privkey.pem";
                String chainPath = CERT_BASE_PATH + "/" + domain + "/chain.pem";
                
                if (!Files.exists(Paths.get(fullchainPath)) || 
                    !Files.exists(Paths.get(privkeyPath))) {
                    throw new RuntimeException("证书文件未生成");
                }
                
                // 更新证书记录
                cert.setStatus(SiteCertificate.STATUS_ACTIVE);
                cert.setCertFile(fullchainPath);
                cert.setKeyFile(privkeyPath);
                cert.setChainFile(chainPath);
                certificateService.saveCertificate(cert);
                
                log.info("证书申请成功: {}", domain);
                
            } catch (Exception e) {
                log.error("证书申请失败: {}", e.getMessage(), e);
                cert.setStatus(SiteCertificate.STATUS_EXPIRED);
                certificateService.saveCertificate(cert);
                throw e;
            }
            
        } catch (Exception e) {
            log.error("证书申请失败: {}", e.getMessage(), e);
            throw new RuntimeException("证书申请失败: " + e.getMessage(), e);
        }
    }
} 