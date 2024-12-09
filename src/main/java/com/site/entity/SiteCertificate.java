package com.site.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SiteCertificate {
    private Long id;
    private Long siteId;
    private String domain;
    private String certType;  // MANUAL/AUTO
    private String certFile;
    private String keyFile;
    private String chainFile;
    private String status;    // PENDING/ACTIVE/EXPIRED
    private Boolean autoRenew;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    // 证书状态常量
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    
    // 证书类型常量
    public static final String TYPE_MANUAL = "MANUAL";
    public static final String TYPE_AUTO = "AUTO";
} 