package com.site.task;

import com.site.entity.Site;
import com.site.service.SiteService;
import com.site.util.CacheUtil;
import com.site.util.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheTask {
    
    private final SiteService siteService;
    private final HttpUtils httpUtils;
    private final CacheUtil cacheUtil;
    
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void updateCache() {
        log.info("开始更新缓存...");
        
        Site query = new Site();
        query.setIsCache(1);  // 只处理启用缓存的站点
        query.setEnabled(1);  // 只处理启用的站点
        
        siteService.getAllSites(query).forEach(site -> {
            try {
                String content = httpUtils.get(site.getUrl());
                cacheUtil.put(site.getUrl(), content, site);
                log.info("站点 {} 缓存更新成功", site.getName());
            } catch (Exception e) {
                log.error("站点 {} 缓存更新失败: {}", site.getName(), e.getMessage());
            }
        });
        
        log.info("缓存更新完成");
    }
} 