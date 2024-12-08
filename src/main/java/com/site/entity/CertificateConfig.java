package com.site.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "certificate_config")
public class CertificateConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "domain_name", unique = true)
    private String domainName;
    
    @Column(name = "enabled")
    private boolean enabled = true;
    
    @Transient
    private String storeType = "PKCS12";
} 