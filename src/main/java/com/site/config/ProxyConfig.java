package com.site.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Set;

@Data
@Configuration
@ConfigurationProperties(prefix = "proxy")
public class ProxyConfig {
    private List<String> allowedDomains;
    private Set<String> staticExtensions;
    private List<String> forwardHeaders;
    private boolean cacheEnabled;
} 