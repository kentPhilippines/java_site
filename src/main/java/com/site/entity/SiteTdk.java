package com.site.entity;

import lombok.Data;

@Data
public class SiteTdk {
    private Long id;
    private Long siteId;
    private String sourceWord;    // 原关键字
    private String targetWord;    // 替换后的关键字
    private String pageUrl;       // 指定页面链接
    private Integer enabled;      // 是否启用
    private String createTime;
    private String updateTime;
} 