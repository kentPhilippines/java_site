package com.site.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "site_config")
public class SiteConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "config_key", unique = true)
    private String key;
    
    @Column(name = "config_value")
    private String value;
} 