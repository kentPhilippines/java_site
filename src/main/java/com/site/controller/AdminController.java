package com.site.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.PostConstruct;
import java.security.SecureRandom;
import org.springframework.beans.factory.annotation.Autowired;
import com.site.service.SiteService;
import com.site.entity.Site;
import com.site.entity.SiteCertificate;
import com.site.service.CertificateService;
import java.util.List;

@Controller
@RequestMapping("/${admin.path}")  // 默认使用UUID
public class AdminController {

    @Value("${server.port:801}")
    private int serverPort;
    
    @Autowired
    private SiteService siteService;
    
    @Value("${admin.path:}")
    private String adminPath;
    
    private String managePath;
    
    @Autowired
    private CertificateService certificateService;
    
    @PostConstruct
    public void init() {
        // 如果配置文件中没有设置admin.path，生成一个随机路径
        if (adminPath == null || adminPath.isEmpty()) {
            SecureRandom random = new SecureRandom();
            managePath = String.format("%06d", random.nextInt(1000000));
        } else {
            managePath = adminPath;
        }
        
        System.out.println("管理页面访问路径: /" + managePath);
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("serverPort", serverPort);
        model.addAttribute("baseUrl", "https://sci.ncut.edu.cn");
        model.addAttribute("managePath", managePath);
        model.addAttribute("adminPath", adminPath);
        model.addAttribute("sites", siteService.getAllSites(new Site()));
        return "admin/index";
    }

    @GetMapping("/certificates/{siteId}")
    public String certificatesPage(@PathVariable Long siteId, Model model) {
        Site site = siteService.selectById(siteId);
        if (site == null) {
            return "redirect:" + adminPath;
        }
        
        List<SiteCertificate> certificates = certificateService.getCertificates(siteId);
        SiteCertificate currentCert = certificates.isEmpty() ? null : certificates.get(0);
        model.addAttribute("site", site);
        model.addAttribute("certificate", currentCert);
        model.addAttribute("certificates", certificates);
        model.addAttribute("adminPath", adminPath);
        model.addAttribute("managePath", managePath);   
        return "admin/certificate";
    }
} 