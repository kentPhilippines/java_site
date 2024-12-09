package com.site.entity;

import lombok.Data;

@Data
public class TargetSite {
    private Long id;
    private String domain;      // 域名
    private String baseUrl;     // 目标URL
    private Boolean enabled;    // 是否启用
    private String description; // 描述
    private String tdk;         // 站点ID
    private String createTime;
    private String updateTime;
} 