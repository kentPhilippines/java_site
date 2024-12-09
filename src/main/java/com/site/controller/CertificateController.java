package com.site.controller;

import com.site.entity.Site;
import com.site.entity.SiteCertificate;
import com.site.service.AcmeService;
import com.site.service.CertificateService;
import com.site.service.SiteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/${admin.path}/api/certificates")
public class CertificateController {

    private final CertificateService certificateService;
    private final AcmeService acmeService;
    private final SiteService siteService;

    @PostMapping("/request/{siteId}")
    public ResponseEntity<?> requestCertificate(@PathVariable Long siteId) {
        try {
            Site site = siteService.selectById(siteId);
            if (site == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "站点不存在"));
            }
            
            // 异步申请证书
            new Thread(() -> {
                try {
                    acmeService.requestCertificate(site);
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
            List<SiteCertificate> certs = certificateService.getCertificates(id);
            if (!certs.isEmpty()) {
                SiteCertificate cert = certs.get(0);
                Map<String, Object> status = new HashMap<>();
                status.put("status", cert.getStatus());
                status.put("expiresAt", cert.getExpiresAt());
                status.put("domain", cert.getDomain());
                status.put("autoRenew", cert.getAutoRenew());
                return ResponseEntity.ok(status);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("取证书状态失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/test/{siteId}")
    public ResponseEntity<?> testCertificate(@PathVariable Long siteId, 
                                           @RequestHeader(value = "X-Forwarded-Proto", required = false) String forwardedProto,
                                           @RequestHeader(value = "X-Forwarded-Host", required = false) String forwardedHost,
                                           HttpServletRequest request) {
        try {
            Site site = siteService.selectById(siteId);
            if (site == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "站点不存在"));
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("site", site);
            result.put("protocol", request.getScheme());
            result.put("secure", request.isSecure());
            result.put("serverPort", request.getServerPort());
            result.put("localPort", request.getLocalPort());
            result.put("remotePort", request.getRemotePort());
            result.put("serverName", request.getServerName());
            result.put("forwardedProto", forwardedProto);
            result.put("forwardedHost", forwardedHost);
            result.put("cipherSuite", request.getAttribute("javax.servlet.request.cipher_suite"));
            result.put("keySize", request.getAttribute("javax.servlet.request.key_size"));
            result.put("sslSessionId", request.getAttribute("javax.servlet.request.ssl_session_id"));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("测试证书失败", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ACME Challenge 验证接口
   
} 