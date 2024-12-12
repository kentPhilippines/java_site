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

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "sites")
public class SiteService {

    private final SiteMapper siteMapper;
    private final CertificateService certificateService;

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
                    String domain = site.getName().replaceAll("https?://", "");
                    certificateService.saveCertificate(domain, site.getId());
                } catch (Exception e) {
                    log.error("站点 {} 的证书申请失败: {}", site.getName(), e.getMessage(), e);
                }
            }).start();
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
                    String domain = site.getName().replaceAll("https?://", "");
                    certificateService.saveCertificate(domain, site.getId());
                } catch (Exception e) {
                    log.error("站点 {} 的证书申请失败: {}", site.getName(), e.getMessage(), e);
                }
            }).start();
        }
    }

    @CacheEvict(allEntries = true)
    @Transactional
    public void deleteSite(String name) {
        siteMapper.delete(name);
    }

    @Cacheable(key = "#id")
    public Site selectById(Long id) {
        return siteMapper.selectById(id);
    }
}
