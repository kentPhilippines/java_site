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

    @Cacheable(key = "#url")
    public Site getSiteByUrl(String url) {
        return siteMapper.findByUrl(url);
    }

    @Cacheable(key = "#name")
    public Site getSiteByName(String name) {
        return siteMapper.findByName(name);
    }

    public List<Site> getAllSites(Site site ) {
        return siteMapper.selectList(site);
    }

    @Transactional
    @CacheEvict(key = "#site.name")
    public void updateSite(Site site) {
        siteMapper.update(site);
    }

    @Transactional
    @CacheEvict(key = "#site.name")
    public void addSite(Site site) {
        siteMapper.insert(site);
    }

    @Transactional
    @CacheEvict(key = "#name")
    public void deleteSite(String name) {
        siteMapper.delete(name);
    }

    public Site selectById(Long id) {
        return siteMapper.selectById(id);
    }

}
