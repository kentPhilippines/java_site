package com.site.controller;

import com.site.entity.Result;
import com.site.entity.SiteStats;
import com.site.service.SiteService;
import com.site.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/site")
public class SiteController {

    private static final Logger log = LoggerFactory.getLogger(SiteController.class);

    @Autowired
    private SiteService siteService;

    @Autowired
    private StatsService statsService;

    @PostMapping("/batch/delete")
    @ResponseBody
    public Result batchDelete(@RequestBody List<Long> siteIds) {
        try {
            siteService.batchDelete(siteIds);
            return Result.success();
        } catch (Exception e) {
            log.error("批量删除站点失败", e);
            return Result.error("批量删除站点失败");
        }
    }

    @PostMapping("/batch/enable")
    @ResponseBody
    public Result batchEnable(@RequestBody List<Long> siteIds) {
        try {
            siteService.batchUpdateStatus(siteIds, true);
            return Result.success();
        } catch (Exception e) {
            log.error("批量启用站点失败", e);
            return Result.error("批量启用站点失败");
        }
    }

    @PostMapping("/batch/disable")
    @ResponseBody
    public Result batchDisable(@RequestBody List<Long> siteIds) {
        try {
            siteService.batchUpdateStatus(siteIds, false);
            return Result.success();
        } catch (Exception e) {
            log.error("批量禁用站点失败", e);
            return Result.error("批量禁用站点失败");
        }
    }

    @GetMapping("/stats/{id}")
    @ResponseBody
    public Result getSiteStats(@PathVariable Long id, 
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            if (startDate == null) {
                startDate = LocalDateTime.now().minusDays(30).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            if (endDate == null) {
                endDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            List<SiteStats> stats = statsService.getSiteStats(id, startDate, endDate);
            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取站点统计失败", e);
            return Result.error("获取站点统计失败");
        }
    }
} 