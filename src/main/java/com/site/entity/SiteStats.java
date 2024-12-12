package com.site.entity;

import lombok.Data;

@Data
public class SiteStats {
    private Long id;
    private Long siteId;
    private String domain;
    private Long visits;      // 总访问量
    private Long uniqueVisits;// 独立访问量
    private Long bandwidth;   // 带宽使用量(bytes)
    private String date;      // 统计日期 yyyy-MM-dd
    private String createdAt;
    private String updatedAt;
} 