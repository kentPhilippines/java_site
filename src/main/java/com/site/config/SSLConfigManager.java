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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.coyote.http11.Http11NioProtocol;

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
            // 验证证书文件
            File keystoreFile = new File(keystorePath);
            if (!keystoreFile.exists()) {
                throw new RuntimeException("证书文件不存在: " + keystorePath);
            }

            TomcatWebServer tomcat = (TomcatWebServer) serverContext.getWebServer();
            Connector[] connectors = tomcat.getTomcat().getService().findConnectors();
            
            for (Connector connector : connectors) {
                if (connector.getScheme().equals("https")) {
                    Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
                    
                    // 创建新的SSL配置
                    SSLHostConfig sslHostConfig = new SSLHostConfig();
                    sslHostConfig.setHostName(domain);
                    sslHostConfig.setCertificateKeystoreFile(keystoreFile.getAbsolutePath());
                    sslHostConfig.setCertificateKeystorePassword(KEYSTORE_PASSWORD);
                    sslHostConfig.setCertificateKeystoreType("PKCS12");
                    sslHostConfig.setProtocols("TLSv1.2,TLSv1.3");
                    

                    
                    // 更新配置
                    protocol.addSslHostConfig(sslHostConfig);

                    log.info("成功为域名 {} 添加SSL证书配置", domain);
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