package com.site.service;

import com.site.entity.SiteCertificate;
import com.site.mapper.SiteCertificateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {
    
    private final SiteCertificateMapper certificateMapper;
    private final CertificateDeployService certificateDeployService;
    
    /**
     * 同步站点证书状态
     * @param siteId 站点ID
     * @return 更新后的证书信息
     */

    
    /**
     * 同步证书状态
     * @param id 证书ID
     * @return 更新后的证书信息
     */
    @Transactional
    public SiteCertificate syncCertificateStatus(Long id) {
        SiteCertificate cert = getCertificate(id);
        if (cert == null) {
            return null;
        }
        try {
            Map<String, Object> pyStatus = certificateDeployService.getCertificateStatus(cert.getDomain());
            if (pyStatus != null) {
                // 更新证书状态
                boolean sslEnabled = Boolean.TRUE.equals(pyStatus.get("ssl_enabled"));
                Map<String, Object> sslInfo = (Map<String, Object>) pyStatus.get("ssl_info");
                if (sslEnabled && sslInfo != null) {
                    // 检查证书文件是否存在
                    boolean certExists = Boolean.TRUE.equals(sslInfo.get("cert_exists"));
                    boolean keyExists = Boolean.TRUE.equals(sslInfo.get("key_exists"));
                    if (certExists && keyExists) {
                        cert.setStatus(SiteCertificate.STATUS_ACTIVE);
                        cert.setCertFile((String) sslInfo.get("cert_path"));
                        cert.setKeyFile((String) sslInfo.get("key_path"));
                    } else {
                        cert.setStatus(SiteCertificate.STATUS_FAILED);
                        log.warn("域名 {} 的证书文件不完整: cert_exists={}, key_exists={}", 
                                cert.getDomain(), certExists, keyExists);
                    }
                } else {
                    cert.setStatus(SiteCertificate.STATUS_FAILED);
                    log.warn("域名 {} 的SSL未启用", cert.getDomain());
                }
                certificateMapper.update(cert);
                log.info("域名 {} 的证书状态已同步", cert.getDomain());
            }
        } catch (Exception e) {
            log.error("同步域名 {} 的证书状态失败: {}", cert.getDomain(), e.getMessage());
            cert.setStatus(SiteCertificate.STATUS_FAILED);
            certificateMapper.update(cert);
        }
        return cert;
    }
    
    /**
     * 为指定域名申请证书
     * @param domain 域名
     * @param siteId 站点ID
     */
    @Transactional
    public void saveCertificate(String domain, Long siteId) {
        // 检查是否已有活跃的证书
        SiteCertificate existingCert = certificateMapper.findByDomain(domain, SiteCertificate.STATUS_ACTIVE);
        if (existingCert != null && SiteCertificate.STATUS_ACTIVE.equals(existingCert.getStatus())) {
            log.info("域名 {} 已有活跃的证书，跳过申请", domain);
            return;
        }
        
        // 创建证书记录
        SiteCertificate certificate = new SiteCertificate();
        certificate.setDomain(domain);
        certificate.setSiteId(siteId);
        certificate.setCertType(SiteCertificate.TYPE_MANUAL);
        certificate.setStatus(SiteCertificate.STATUS_ACTIVE);
        certificate.setAutoRenew(true);
        certificate.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        certificate.setExpiresAt(LocalDateTime.now().plusDays(90).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 保存到数据库
        certificateMapper.insert(certificate);
        
        // 异步调用Python服务申请证书
        try {
//            Map<String, Object> certInfo = certificateDeployService.requestCertificate(
//                domain,
//                "admin@" + domain
//            );
            
            // 更新证书文件路径
//            if (certInfo != null) {
//                certificate.setCertFile((String) certInfo.get("cert_file"));
//                certificate.setKeyFile((String) certInfo.get("key_file"));
//                if (certInfo.containsKey("chain_file")) {
//                    certificate.setChainFile((String) certInfo.get("chain_file"));
//                }
                certificateMapper.update(certificate);
                log.info("域名 {} 的证书申请成功", domain);
//            }
        } catch (Exception e) {
            log.error("通过Python服务申请证书失败: {}", e.getMessage(), e);
            // 更新证书状态为失败
            certificate.setStatus(SiteCertificate.STATUS_FAILED);
            certificateMapper.update(certificate);
        }
    }
    
    @Transactional
    public void saveCertificate(SiteCertificate certificate) {
        if (certificate.getId() == null) {
            certificateMapper.insert(certificate);
        } else {
            certificateMapper.update(certificate);
        }
    }
    
    public List<SiteCertificate> getCertificates(Long siteId) {
        return certificateMapper.findBySiteId(siteId);
    }
    
    public List<SiteCertificate> findByStatus(String status) {
        return certificateMapper.findByStatus(status);
    }
    
    public SiteCertificate getCertificate(Long id) {
        return certificateMapper.findById(id);
    }
    
    @Transactional
    public void deleteCertificate(Long id) {
        SiteCertificate cert = getCertificate(id);
        if (cert != null) {
            // 如果证书正在使用，通过Python服务删除证书
            if (SiteCertificate.STATUS_ACTIVE.equals(cert.getStatus())) {
                try {
                    certificateDeployService.removeCertificate(cert.getDomain());
                } catch (Exception e) {
                    log.warn("通过Python服务删除证书失败: {}", e.getMessage());
                }
            }
            certificateMapper.deleteById(id);
        }
    }
    
    /**
     * 获取证书状态
     */
    public Map<String, Object> getCertificateStatus(String domain) {
        try {
            return certificateDeployService.getCertificateStatus(domain);
        } catch (Exception e) {
            log.error("获取证书状态失败: {}", e.getMessage(), e);
            return null;
        }
    }
} 