package com.site.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.site.entity.Site;
import com.site.service.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import java.text.SimpleDateFormat;
import java.util.Date;
@RestController
@RequestMapping("/${admin.path}")  // 默认使用UUID
public class SiteApi {

    @Autowired
    private SiteService siteService;

    @GetMapping("/sites/{id}")
    public Site getSite(@PathVariable Long id) {
        return siteService.selectById(id);
    }

    @PostMapping("/sites")
    public void addSite(@RequestBody Site site) {
        //格式化日期
        site.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        site.setUpdateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        siteService.addSite(site);
    }
    @DeleteMapping("/sites/{name}")
    public void deleteSite(@PathVariable String name) {
        siteService.deleteSite(name);
    }

    @PutMapping("/sites/{id}")
    public void updateSite(@PathVariable Long id, @RequestBody Site site) {
        siteService.updateSite(site);
    }
}
