package com.site.service;

import com.site.entity.SiteConfig;
import com.site.repository.SiteConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteConfigService {
    
    private final SiteConfigRepository configRepository;
    private static final String DEFAULT_BASE_URL = "https://sci.ncut.edu.cn";
    
    public String getBaseUrl() {
        SiteConfig config = configRepository.findByKey("BASE_URL");
        return config != null ? config.getValue() : DEFAULT_BASE_URL;
    }
    
    public void setBaseUrl(String baseUrl) {
        SiteConfig config = configRepository.findByKey("BASE_URL");
        if (config == null) {
            config = new SiteConfig();
            config.setKey("BASE_URL");
        }
        config.setValue(baseUrl);
        configRepository.save(config);
    }
} 