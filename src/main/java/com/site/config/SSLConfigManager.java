package com.site.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.stereotype.Component;
import java.io.File;
import javax.annotation.PostConstruct;
import org.apache.catalina.LifecycleState;

@Slf4j
@Component
@RequiredArgsConstructor
public class SSLConfigManager {

    private final ServletWebServerApplicationContext serverContext;
    private static final String KEYSTORE_PASSWORD = "changeit";

    @PostConstruct
    public void init() {
        scanExistingCertificates();
    }

    public void addCertificate(String domain, String keystorePath) {
        try {
            TomcatWebServer tomcatWebServer = (TomcatWebServer) serverContext.getWebServer();
            Connector[] connectors = tomcatWebServer.getTomcat().getService().findConnectors();
            
            for (Connector connector : connectors) {
                if (connector.getScheme().equals("https")) {
                    if (connector.getState().isAvailable()) {
                        // 为域名配置 SSL
                        SSLHostConfig sslHostConfig = new SSLHostConfig();
                        sslHostConfig.setHostName(domain);
                        sslHostConfig.setCertificateKeystoreFile(new File(keystorePath).getAbsolutePath());
                        sslHostConfig.setCertificateKeystorePassword(KEYSTORE_PASSWORD);
                        sslHostConfig.setCertificateKeystoreType("PKCS12");
                        
                        connector.addSslHostConfig(sslHostConfig);
                        log.info("成功为域名 {} 添加SSL证书配置", domain);
                    } else {
                        log.warn("Connector 不可用，无法添加SSL配置");
                    }
                }
            }
        } catch (Exception e) {
            log.error("添加SSL证书配置失败: {}", e.getMessage(), e);
            throw new RuntimeException("添加SSL证书配置失败: " + e.getMessage());
        }
    }

    private void scanExistingCertificates() {
        try {
            File certsDir = new File("certs");
            if (!certsDir.exists() || !certsDir.isDirectory()) {
                return;
            }

            File[] domainDirs = certsDir.listFiles(File::isDirectory);
            if (domainDirs == null) {
                return;
            }

            for (File domainDir : domainDirs) {
                File keystoreFile = new File(domainDir, "keystore.p12");
                if (keystoreFile.exists()) {
                    String domain = domainDir.getName();
                    addCertificate(domain, keystoreFile.getAbsolutePath());
                    log.info("已加载域名 {} 的SSL证书", domain);
                }
            }
        } catch (Exception e) {
            log.error("扫描证书失败", e);
        }
    }
} 