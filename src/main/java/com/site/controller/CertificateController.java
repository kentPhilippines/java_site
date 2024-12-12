package com.site.controller;

import com.site.entity.Site;
import com.site.entity.SiteCertificate;
import com.site.service.CertificateService;
import com.site.service.SiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/${admin.path}/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final SiteService siteService;

    @PostMapping("/request/{siteId}")
    public ResponseEntity<?> requestCertificate(@PathVariable Long siteId) {
        try {
            Site site = siteService.selectById(siteId);
            if (site == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "站点不存在"));
            }

            String domain = site.getName().replaceAll("https?://", "");
            
            // 检查是否已有活跃的证书
            List<SiteCertificate> certs = certificateService.getCertificates(siteId);
            if (!certs.isEmpty()) {
                SiteCertificate cert = certs.get(0);
                if (SiteCertificate.STATUS_ACTIVE.equals(cert.getStatus())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "已有活跃的证书，无需重新申请"));
                }
            }
            
            // 异步申请证书
            new Thread(() -> {
                try {
                    certificateService.saveCertificate(domain, siteId);
                } catch (Exception e) {
                    log.error("证书申请失败: {}", e.getMessage(), e);
                }
            }).start();
            
            return ResponseEntity.ok(Map.of("message", "证书申请已开始，请稍后查看状态"));
        } catch (Exception e) {
            log.error("证书申请失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/site/{siteId}")
    public ResponseEntity<List<SiteCertificate>> getCertificates(@PathVariable Long siteId) {
        return ResponseEntity.ok(certificateService.getCertificates(siteId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCertificate(@PathVariable Long id) {
        try {
            certificateService.deleteCertificate(id);
            return ResponseEntity.ok(Map.of("message", "证书删除成功"));
        } catch (Exception e) {
            log.error("证书删除失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{id}")
    public ResponseEntity<Map<String, Object>> getCertificateStatus(@PathVariable Long id) {
        try {
            // 同步证书状态
            SiteCertificate cert = certificateService.syncCertificateStatus(id);
            if (cert != null) {
                Map<String, Object> status = new HashMap<>();
                status.put("status", cert.getStatus());
                status.put("expiresAt", cert.getExpiresAt());
                status.put("domain", cert.getDomain());
                status.put("autoRenew", cert.getAutoRenew());
                status.put("certFile", cert.getCertFile());
                status.put("keyFile", cert.getKeyFile());
                status.put("chainFile", cert.getChainFile());
                
                // 获取Python服务的证书状态
                Map<String, Object> pyStatus = certificateService.getCertificateStatus(cert.getDomain());
                if (pyStatus != null) {
                    status.put("py_status", pyStatus);
                }
                
                return ResponseEntity.ok(status);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("获取证书状态失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


} 