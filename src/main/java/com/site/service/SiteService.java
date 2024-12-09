package com.site.service;

import java.util.List;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.site.entity.Site;
import com.site.mapper.SiteMapper;

import org.checkerframework.checker.units.qual.s;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;



@Service
@RequiredArgsConstructor
@Slf4j
public class SiteService {

    private final SiteMapper siteMapper;

    public List<Site> getAllSites(Site site) {
        List<Site> sites = siteMapper.selectList(site); 
        return sites;
    }

    public void addSite(Site site) {
        log.info("添加站点: {}", site);
        siteMapper.insert(site);
    }

    public void updateSite(Site site) {
        siteMapper.update(site);
    }

    public void deleteSite(Long id) {
        siteMapper.delete(id);
    }

    public Site selectById(Long id) {
        return siteMapper.selectById(id);
    }


}
