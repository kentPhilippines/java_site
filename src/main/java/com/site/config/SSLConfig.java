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
        tomcat.addAdditionalTomcatConnectors(createStandardConnector());
        
        // 配置 SSL
        tomcat.addConnectorCustomizers(connector -> {
            List<CertificateConfig> certificates = certificateRepository.findByEnabled(true);
            for (CertificateConfig cert : certificates) {
                connector.setAttribute("keystoreFile", cert.getCertificatePath());
                connector.setAttribute("keystorePass", cert.getKeyPassword());
                connector.setAttribute("keystoreType", cert.getStoreType());
                connector.setAttribute("keyAlias", cert.getKeyAlias());
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