package com.site.repository;

import com.site.entity.SiteConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteConfigRepository extends JpaRepository<SiteConfig, Long> {
    SiteConfig findByKey(String key);
} 