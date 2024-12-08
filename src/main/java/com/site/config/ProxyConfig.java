package com.site.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "proxy")
public class ProxyConfig {
    private List<String> allowedDomains;
    private boolean cacheEnabled;
    private List<String> staticExtensions;
    private List<String> forwardHeaders;
} 