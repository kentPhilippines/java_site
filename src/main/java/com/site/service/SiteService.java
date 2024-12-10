package com.site.service;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.site.entity.Site;
import com.site.mapper.SiteMapper;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "sites")
public class SiteService {

    private final SiteMapper siteMapper;
    
    @Autowired
    private NginxService nginxService;
    
    private final AcmeService acmeService;

    @Cacheable(key = "#host")
    public Site getSiteByUrl(String host) {
        // 先尝试通过域名匹配
        Site site = siteMapper.findByName(host);
        if (site != null) {
            return site;
        }
        // 如果找不到，再尝试通过URL匹配
        return siteMapper.findByUrl(host);
    }

    @Cacheable(key = "#name")
    public Site getSiteByName(String name) {
        return siteMapper.findByName(name);
    }

    @Cacheable(key = "'all:' + #site.toString()")
    public List<Site> getAllSites(Site site) {
        return siteMapper.selectList(site);
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void updateSite(Site site) {
        siteMapper.update(site);
        // 如果启用了HTTPS，确保有SSL证书
        Integer ssl = site.getSsl();
        if (1 == ssl) {
            // 异步申请证书
            new Thread(() -> {
                try {
                    // 申请证书
                    acmeService.requestCertificate(site);
                    // 生成Nginx配置
                    nginxService.generateSiteConfig(site.getName());
                } catch (Exception e) {
                    log.error("站点 {} 的证书申请失败: {}", site.getName(), e.getMessage(), e);
                }
            }).start();
        } else {
            // 直接生成Nginx配置
            nginxService.generateSiteConfig(site.getName());
        }
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void addSite(Site site) {
        siteMapper.insert(site);
        // 如果启用了HTTPS，自动申请证书
        if (site.getSsl() == 1) {
            // 异步申请证书
            new Thread(() -> {
                try {
                    // 申请证书
                    acmeService.requestCertificate(site);
                    // 生成Nginx配置
                    nginxService.generateSiteConfig(site.getName());
                } catch (Exception e) {
                    log.error("站点 {} 的证书申请失败: {}", site.getName(), e.getMessage(), e);
                }
            }).start();
        } else {
            // 直接生成Nginx配置
            nginxService.generateSiteConfig(site.getName());
        }
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void deleteSite(String name) {
        Site site = getSiteByName(name);
        if (site != null) {
            String domain = site.getUrl().replaceAll("https?://", "");
            siteMapper.delete(name);
            // 删除Nginx配置
            nginxService.deleteSiteConfig(domain);
        }
    }

    @Cacheable(key = "#id")
    public Site selectById(Long id) {
        return siteMapper.selectById(id);
    }
}
