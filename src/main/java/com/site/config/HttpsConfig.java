package com.site.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.catalina.Context;
import org.springframework.core.io.Resource;

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
                // 禁用SSL配置，使用动态配置
                context.setUseHttpOnly(true);
            }
        };
        
        // 设置SSL为false，使用动态配置
        tomcat.setSsl(null);
        
        // 添加HTTP连接器
        tomcat.addAdditionalTomcatConnectors(createStandardConnector());
        
        return tomcat;
    }

    private Connector createStandardConnector() {
        Connector connector = new Connector(Http11NioProtocol.class.getName());
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setScheme("http");
        connector.setRedirectPort(httpsPort);
        
        // 配置连接器参数
        connector.setProperty("relaxedPathChars", "[]|");
        connector.setProperty("relaxedQueryChars", "[]|{}^&#x5c;&#x60;&lt;&gt;");
        connector.setProperty("maxThreads", "200");
        connector.setProperty("acceptCount", "100");
        connector.setProperty("maxConnections", "10000");
        connector.setProperty("connectionTimeout", "20000");
        connector.setProperty("maxHttpHeaderSize", "8192");
        
        return connector;
    }
} 