package com.site.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class CertificateGenerator {
    
    private static final String CERT_DIR = "cert";
    private static final String KEYSTORE_FILE = "server.p12";
    private static final String KEYSTORE_PASSWORD = "your_default_password";
    private static final String KEY_ALIAS = "tomcat";
    
    public void generateCertificate(String domainName) {
        try {
            // 创建证书目录
            createCertDirectory();
            
            // 构建 keytool 命令
            String command = String.format(
                "keytool -genkeypair " +
                "-alias %s " +
                "-keyalg RSA " +
                "-keysize 2048 " +
                "-keystore %s/%s " +
                "-storetype PKCS12 " +
                "-storepass %s " +
                "-validity 3650 " +
                "-dname \"CN=%s, OU=IT, O=Company, L=City, ST=State, C=CN\" " +
                "-ext \"SAN=DNS:%s\"",
                KEY_ALIAS,
                CERT_DIR,
                KEYSTORE_FILE,
                KEYSTORE_PASSWORD,
                domainName,
                domainName
            );
            
            // 执行命令
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                log.info("证书生成成功：{}", domainName);
            } else {
                log.error("证书生成失败：{}", domainName);
            }
            
        } catch (Exception e) {
            log.error("生成证书时发生错误", e);
        }
    }
    
    private void createCertDirectory() throws IOException {
        Path certPath = Paths.get(CERT_DIR);
        if (!Files.exists(certPath)) {
            Files.createDirectories(certPath);
        }
    }
    
    public String getCertificatePath() {
        return new File(CERT_DIR, KEYSTORE_FILE).getAbsolutePath();
    }
    
    public String getKeyPassword() {
        return KEYSTORE_PASSWORD;
    }
    
    public String getKeyAlias() {
        return KEY_ALIAS;
    }
} 