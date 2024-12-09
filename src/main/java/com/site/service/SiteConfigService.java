package com.site.service;

import com.site.entity.SiteConfig;
import com.site.mapper.SiteConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SiteConfigService {
    
    private final SiteConfigMapper configMapper;
    
    public String getBaseUrl() {
        SiteConfig config = configMapper.findByKey("base_url");
        return config != null ? config.getConfigValue() : null;
    }
    
    public void setBaseUrl(String baseUrl) {
        SiteConfig config = new SiteConfig();
        config.setConfigKey("base_url");
        config.setConfigValue(baseUrl);
        config.setEnabled(true);
        
        if (configMapper.existsByKey("base_url") > 0) {
            configMapper.update(config);
        } else {
            configMapper.insert(config);
        }
    }
} 