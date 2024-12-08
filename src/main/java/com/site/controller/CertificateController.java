package com.site.controller;

import com.site.entity.CertificateConfig;
import com.site.repository.CertificateConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateConfigRepository certificateRepository;
    
    @GetMapping
    public List<CertificateConfig> getAllCertificates() {
        return certificateRepository.findAll();
    }
    
    @PostMapping
    public CertificateConfig addCertificate(@RequestBody CertificateConfig config) {
        return certificateRepository.save(config);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<CertificateConfig> updateCertificate(
            @PathVariable Long id,
            @RequestBody CertificateConfig config) {
        if (!certificateRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        config.setId(id);
        return ResponseEntity.ok(certificateRepository.save(config));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertificate(@PathVariable Long id) {
        if (!certificateRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        certificateRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
} 