package com.site.controller;

import com.site.entity.CertificateConfig;
import com.site.repository.CertificateConfigRepository;
import com.site.util.CertificateGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CertificateConfigRepository certificateRepository;
    private final CertificateGenerator certificateGenerator;

    @GetMapping
    public String adminPage(Model model) {
        model.addAttribute("certificates", certificateRepository.findAll());
        model.addAttribute("newCertificate", new CertificateConfig());
        return "admin/index";
    }

    @PostMapping("/certificates")
    public String addCertificate(@ModelAttribute CertificateConfig certificate) {
        // 生成证书
        certificateGenerator.generateCertificate(certificate.getDomainName());
        // 保存配置
        certificateRepository.save(certificate);
        return "redirect:/admin";
    }

    @PostMapping("/certificates/{id}/delete")
    public String deleteCertificate(@PathVariable Long id) {
        certificateRepository.deleteById(id);
        return "redirect:/admin";
    }

    @PostMapping("/certificates/{id}/toggle")
    public String toggleCertificate(@PathVariable Long id) {
        CertificateConfig cert = certificateRepository.findById(id).orElseThrow();
        cert.setEnabled(!cert.isEnabled());
        certificateRepository.save(cert);
        return "redirect:/admin";
    }
} 