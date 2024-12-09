package com.site.service;

import com.site.entity.SiteCertificate;
import com.site.mapper.SiteCertificateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final SiteCertificateMapper certificateMapper;
    private static final String CERT_BASE_PATH = "certs";

    @Transactional
    public void uploadCertificate(Long siteId, String domain, MultipartFile certFile,
                                MultipartFile keyFile, MultipartFile chainFile) throws Exception {
        // 验证证书
        X509Certificate cert = validateCertificate(certFile);
        
        // 创建证书存储目录
        String certPath = createCertificateDirectory(domain);
        
        // 保存证书文件
        String certFilePath = saveCertificateFile(certFile, certPath, "cert.pem");
        String keyFilePath = saveCertificateFile(keyFile, certPath, "privkey.pem");
        String chainFilePath = chainFile != null ? 
            saveCertificateFile(chainFile, certPath, "chain.pem") : null;
        
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
        certificate.setCreatedAt(LocalDateTime.now());
        certificate.setExpiresAt(LocalDateTime.ofInstant(
            cert.getNotAfter().toInstant(), ZoneId.systemDefault()));
        
        certificateMapper.insert(certificate);
        
        // 重新加载SSL配置
        reloadSSLConfig();
    }
    
    private X509Certificate validateCertificate(MultipartFile certFile) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(certFile.getInputStream());
        
        // 检查证书是否过期
        try {
            cert.checkValidity();
        } catch (Exception e) {
            throw new Exception("证书已过期或尚未生效");
        }
        
        return cert;
    }
    
    private String createCertificateDirectory(String domain) throws IOException {
        String certPath = CERT_BASE_PATH + File.separator + domain;
        Files.createDirectories(Paths.get(certPath));
        return certPath;
    }
    
    private String saveCertificateFile(MultipartFile file, String certPath, String fileName) throws IOException {
        if (file == null) return null;
        
        Path filePath = Paths.get(certPath, fileName);
        Files.copy(file.getInputStream(), filePath);
        return filePath.toString();
    }
    
    private void reloadSSLConfig() {
        // TODO: 实现SSL配置重新加载逻辑
        log.info("重新加载SSL配置");
    }
    
    public List<SiteCertificate> getCertificates(Long siteId) {
        return certificateMapper.findBySiteId(siteId);
    }
    
    public void deleteCertificate(Long id) throws IOException {
        SiteCertificate cert = certificateMapper.findByDomain(id.toString());
        if (cert != null) {
            // 删除证书文件
            if (cert.getCertFile() != null) Files.deleteIfExists(Paths.get(cert.getCertFile()));
            if (cert.getKeyFile() != null) Files.deleteIfExists(Paths.get(cert.getKeyFile()));
            if (cert.getChainFile() != null) Files.deleteIfExists(Paths.get(cert.getChainFile()));
            
            // 删除证书目录
            Files.deleteIfExists(Paths.get(CERT_BASE_PATH, cert.getDomain()));
            
            // 删除数据库记录
            certificateMapper.delete(id);
            
            // 重新加载SSL配置
            reloadSSLConfig();
        }
    }
    
    public void updateCertificateStatus(Long id, String status) {
        certificateMapper.updateStatus(id, status);
    }
    
    // 检查证书状态的定时任务
    @Scheduled(cron = "0 0 * * * *") // 每小时执行一次
    public void checkCertificatesStatus() {
        List<SiteCertificate> certificates = certificateMapper.findByStatus(SiteCertificate.STATUS_ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        
        for (SiteCertificate cert : certificates) {
            if (cert.getExpiresAt().isBefore(now)) {
                updateCertificateStatus(cert.getId(), SiteCertificate.STATUS_EXPIRED);
                log.warn("证书已过期: {}", cert.getDomain());
            }
        }
    }

    public void saveCertificate(SiteCertificate siteCert) {
        certificateMapper.insert(siteCert);
    }
} 