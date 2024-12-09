package com.site.service;

import com.site.entity.SiteCertificate;
import com.site.mapper.SiteCertificateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final SiteCertificateMapper certificateMapper;
    private static final String CERT_DIR = "certs";
    private static final DateTimeFormatter SQL_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void uploadCertificate(Long siteId, String domain, MultipartFile certFile, 
                                MultipartFile keyFile, MultipartFile chainFile) throws Exception {
        // 创建证书目录
        Path certPath = Paths.get(CERT_DIR, domain);
        Files.createDirectories(certPath);
        
        // 保存文件
        String certFilePath = certPath.resolve("cert.pem").toString();
        String keyFilePath = certPath.resolve("privkey.pem").toString();
        String chainFilePath = certPath.resolve("chain.pem").toString();
        
        certFile.transferTo(new File(certFilePath));
        keyFile.transferTo(new File(keyFilePath));
        chainFile.transferTo(new File(chainFilePath));
        
        // 读取证书信息
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(certFile.getInputStream());
        
        // 创建证书记录
        SiteCertificate certificate = new SiteCertificate();
        certificate.setSiteId(siteId);
        certificate.setDomain(domain);
        certificate.setCertType(SiteCertificate.TYPE_MANUAL);
        certificate.setCertFile(certFilePath);
        certificate.setKeyFile(keyFilePath);
        certificate.setChainFile(chainFilePath);
        certificate.setStatus(SiteCertificate.STATUS_ACTIVE);
        certificate.setAutoRenew(false);
        certificate.setCreatedAt(LocalDateTime.now().format(SQL_TIMESTAMP));
        certificate.setExpiresAt(LocalDateTime.ofInstant(
            cert.getNotAfter().toInstant(), 
            ZoneId.systemDefault()
        ).format(SQL_TIMESTAMP));
        
        certificateMapper.insert(certificate);
    }

    public List<SiteCertificate> getCertificates(Long siteId) {
        return certificateMapper.findBySiteId(siteId);
    }

    public void saveCertificate(SiteCertificate certificate) {
        if (certificate.getId() == null) {
            certificateMapper.insert(certificate);
        } else {
            certificateMapper.update(certificate);
        }
    }

    public void deleteCertificate(Long id) {
        SiteCertificate cert = certificateMapper.findById(id);
        if (cert != null) {
            // 删除文件
            if (cert.getCertFile() != null) {
                new File(cert.getCertFile()).delete();
            }
            if (cert.getKeyFile() != null) {
                new File(cert.getKeyFile()).delete();
            }
            if (cert.getChainFile() != null) {
                new File(cert.getChainFile()).delete();
            }
            
            // 删除记录
            certificateMapper.deleteById(id);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void checkCertificatesStatus() {
        List<SiteCertificate> certificates = certificateMapper.findByStatus(SiteCertificate.STATUS_ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(SQL_TIMESTAMP);
        
        for (SiteCertificate cert : certificates) {
            try {
                LocalDateTime expiresAt = LocalDateTime.parse(cert.getExpiresAt(), SQL_TIMESTAMP);
                if (expiresAt.isBefore(now)) {
                    updateCertificateStatus(cert.getId(), SiteCertificate.STATUS_EXPIRED);
                    log.warn("证书已过期: {}", cert.getDomain());
                }
            } catch (Exception e) {
                log.error("检查证书状态失败: {}", cert.getDomain(), e);
            }
        }
    }

    private void updateCertificateStatus(Long id, String status) {
        SiteCertificate cert = new SiteCertificate();
        cert.setId(id);
        cert.setStatus(status);
        certificateMapper.update(cert);
    }
} 