package com.site.task;

import com.site.entity.Site;
import com.site.service.SiteService;
import com.site.util.SitemapGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SitemapTask {
    
    private final SiteService siteService;
    private final SitemapGenerator sitemapGenerator;
    
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    public void generateSitemaps() {
        log.info("开始生成网站地图...");
        
        Site query = new Site();
        query.setSitemap(1);
        siteService.getAllSites(query).forEach(site -> {
            try {
                sitemapGenerator.generateSitemap(site);
            } catch (Exception e) {
                log.error("生成网站地图失败: " + site.getName(), e);
            }
        });
        
        log.info("网站地图生成完成");
    }
} 