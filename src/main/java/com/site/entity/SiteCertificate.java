package com.site.entity;

import lombok.Data;

@Data
public class SiteCertificate {
    private Long id;
    private Long siteId;
    private String domain;
    private String certType;
    private String certFile;
    private String keyFile;
    private String chainFile;
    private String status;
    private Boolean autoRenew;
    private String createdAt;
    private String expiresAt;
    
    // 证书状态常量
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_FAILED = "FAILED";
    
    // 证书类型常量
    public static final String TYPE_MANUAL = "MANUAL";  // 手动上传的证书
} 