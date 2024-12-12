package com.site.controller;

import com.site.entity.Result;
import com.site.entity.Site;
import com.site.entity.SiteTdk;
import com.site.service.SiteKeywordsService;
import com.site.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/${admin.path}/keywords")
@RequiredArgsConstructor
public class KeywordsController {

    private final SiteKeywordsService keywordsService;
    private final SiteService siteService;
    @Value("${admin.path}")
    private String adminPath;

    @GetMapping("/manage/{siteId}")
    public String managePage(@PathVariable Long siteId, Model model) {
        Site site = siteService.selectById(siteId);
        model.addAttribute("site", site);
        model.addAttribute("adminPath", adminPath);
        return "admin/keywords";
    }

    @GetMapping("/{siteId}")
    @ResponseBody
    public Result getKeywords(@PathVariable Long siteId) {
        try {
            List<SiteTdk> keywords = keywordsService.getKeywordsBySiteId(siteId);
            return Result.success(keywords);
        } catch (Exception e) {
            return Result.error("获取关键字列表失败");
        }
    }

    @PostMapping("/{siteId}")
    @ResponseBody
    public Result saveKeywords(@PathVariable Long siteId, @RequestBody List<SiteTdk> keywords) {
        try {
            keywordsService.saveKeywords(siteId, keywords);
            return Result.success();
        } catch (Exception e) {
            return Result.error("保存关键字失败");
        }
    }

    @DeleteMapping("/{id}")
    @ResponseBody
    public Result deleteKeyword(@PathVariable Long id) {
        try {
            keywordsService.deleteKeyword(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除关键字失败");
        }
    }
} 