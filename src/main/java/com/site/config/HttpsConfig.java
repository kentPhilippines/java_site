package com.site.config;

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.catalina.Context;
import org.springframework.core.io.Resource;
import java.io.File;

@Slf4j
@Configuration
public class HttpsConfig {

    @Value("${server.http.port:80}")
    private int httpPort;

    @Value("${server.port:443}")
    private int httpsPort;
    
    @Value("${ssl.cert-path:certs}")
    private String certPath;

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
            }
        };
        
        // 添加HTTP连接器
        tomcat.addAdditionalTomcatConnectors(createStandardConnector());
        // 添加HTTPS连接器
        tomcat.addAdditionalTomcatConnectors(createHttpsConnector());
        
        return tomcat;
    }

    private Connector createStandardConnector() {
        Connector connector = new Connector(Http11NioProtocol.class.getName());
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setScheme("http");
        connector.setRedirectPort(httpsPort);
        return connector;
    }

    private Connector createHttpsConnector() {
        Connector connector = new Connector(Http11NioProtocol.class.getName());
        connector.setPort(httpsPort);
        connector.setSecure(true);
        connector.setScheme("https");
        return connector;
    }
} 