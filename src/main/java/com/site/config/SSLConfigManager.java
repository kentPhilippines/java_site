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
        log.info("添加SSL证书配置: {} {}", domain, keystorePath);
        try {
            // 验证证书文件
            File keystoreFile = new File(keystorePath);
            if (!keystoreFile.exists()) {
                throw new RuntimeException("证书文件不存在: " + keystorePath);
            }

            TomcatWebServer tomcat = (TomcatWebServer) serverContext.getWebServer();
            Connector[] connectors = tomcat.getTomcat().getService().findConnectors();
            log.info("找到的连接器数量: {}", connectors.length);
            for (Connector connector : connectors) {
                log.info("连接器: {}", connector.getScheme());
                if (connector.getScheme().equals("https")) {
                    // 创建新的SSL配置
                    SSLHostConfig sslHostConfig = new SSLHostConfig();
                    sslHostConfig.setHostName(domain);
                    sslHostConfig.setCertificateKeystoreFile(keystoreFile.getAbsolutePath());
                    sslHostConfig.setCertificateKeystorePassword(KEYSTORE_PASSWORD);
                    sslHostConfig.setCertificateKeystoreType("PKCS12");
                    sslHostConfig.setProtocols("+TLSv1.2,+TLSv1.3");
                    
                    // 检查现有配置
                    SSLHostConfig[] existingConfigs = connector.findSslHostConfigs();
                    log.info("现有SSL主机配置数量: {}", existingConfigs.length);
                    for (SSLHostConfig config : existingConfigs) {
                        log.info("已配置的域名: {}, 证书文件: {}", 
                            config.getHostName(), 
                            config.getCertificateKeystoreFile());
                    }
                    // 更新配置
                    connector.addSslHostConfig(sslHostConfig);
                    log.info("成功为域名 {} 添加SSL证书配置", domain);
                    
                    // 验证配置是否生效
                    SSLHostConfig[] sslHostConfigs = connector.findSslHostConfigs();
                    for (SSLHostConfig config : sslHostConfigs) {
                        log.info("SSL配置验证成功 - 域名: {}, 证书文件: {}, 协议: {}", 
                            config.getHostName(),
                            config.getCertificateKeystoreFile(),
                            config.getProtocols());
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
            if (!certsDir.exists()) {
                certsDir.mkdirs();
                log.info("创建证书目录: {}", certsDir.getAbsolutePath());
                return;
            }

            if (!certsDir.isDirectory()) {
                log.error("证书路径不是目录: {}", certsDir.getAbsolutePath());
                return;
            }

            File[] domainDirs = certsDir.listFiles(File::isDirectory);
            if (domainDirs == null) {
                log.warn("证书目录为空");
                return;
            }

            for (File domainDir : domainDirs) {
                File keystoreFile = new File(domainDir, "keystore.p12");
                if (keystoreFile.exists()) {
                    String domain = domainDir.getName();
                    addCertificate(domain, keystoreFile.getAbsolutePath());
                    log.info("已加载域名 {} 的SSL证书", domain);
                } else {
                    log.warn("域名 {} 的keystore文件不存在: {}", domainDir.getName(), keystoreFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("扫描证书失败", e);
        }
    }
} 