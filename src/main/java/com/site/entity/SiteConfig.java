package com.site.entity;

import lombok.Data;

@Data
public class SiteConfig {
    private Long id;
    private String configKey;
    private String configValue;
    private Boolean enabled;
    private String createTime;
    private String updateTime;
} 