package com.site.repository;

import com.site.entity.CertificateConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CertificateConfigRepository extends JpaRepository<CertificateConfig, Long> {
    List<CertificateConfig> findByEnabled(boolean enabled);
    CertificateConfig findByDomainName(String domainName);
} 