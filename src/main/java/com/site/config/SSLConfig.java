package com.site.config;

import com.site.entity.CertificateConfig;
import com.site.repository.CertificateConfigRepository;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class SSLConfig {
    
    private final CertificateConfigRepository certificateRepository;
    
    @Value("${server.http.port:80}")
    private int httpPort;
    
    public SSLConfig(CertificateConfigRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }
    
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        
        // 添加 HTTP 连接器
        tomcat.addAdditionalTomcatConnectors(createStandardConnector());
        
        // 配置 SSL
        tomcat.addContextCustomizers(context -> {
            List<CertificateConfig> certificates = certificateRepository.findByEnabled(true);
            for (CertificateConfig cert : certificates) {
                SSLHostConfig sslHostConfig = new SSLHostConfig();
                sslHostConfig.setHostName(cert.getDomainName());
                sslHostConfig.setCertificateKeystoreFile(cert.getCertificatePath());
                sslHostConfig.setCertificateKeystorePassword(cert.getKeyPassword());
                sslHostConfig.setCertificateKeystoreType(cert.getStoreType());
                context.addSSLHostConfig(sslHostConfig);
            }
        });
        
        return tomcat;
    }
    
    private Connector createStandardConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(httpPort);
        return connector;
    }
} 