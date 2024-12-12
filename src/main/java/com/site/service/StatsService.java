package com.site.service;

import com.site.entity.SiteStats;
import com.site.mapper.SiteStatsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {
    
    private final SiteStatsMapper statsMapper;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 用于记录独立访客
    private final Map<String, Map<String, Boolean>> dailyVisitors = new ConcurrentHashMap<>();
    
    /**
     * 记录访问
     */
    @Transactional
    public void recordVisit(Long siteId, String domain, HttpServletRequest request) {
        String today = LocalDateTime.now().format(DATE_FORMAT);
        String ip = getClientIp(request);
        long bandwidth = request.getContentLengthLong();
        
        // 获取或创建今日统计
        SiteStats stats = statsMapper.findByDateAndSite(today, siteId);
        if (stats == null) {
            stats = new SiteStats();
            stats.setSiteId(siteId);
            stats.setDomain(domain);
            stats.setDate(today);
            stats.setVisits(0L);
            stats.setUniqueVisits(0L);
            stats.setBandwidth(0L);
            stats.setCreatedAt(LocalDateTime.now().format(DATETIME_FORMAT));
            stats.setUpdatedAt(stats.getCreatedAt());
            statsMapper.insert(stats);
        }
        
        // 更新访问量
        stats.setVisits(stats.getVisits() + 1);
        stats.setBandwidth(stats.getBandwidth() + bandwidth);
        
        // 检查是否是新的独立访客
        String key = domain + "_" + today;
        Map<String, Boolean> visitors = dailyVisitors.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        if (!visitors.containsKey(ip)) {
            visitors.put(ip, true);
            stats.setUniqueVisits(stats.getUniqueVisits() + 1);
        }
        
        stats.setUpdatedAt(LocalDateTime.now().format(DATETIME_FORMAT));
        statsMapper.update(stats);
    }
    
    /**
     * 获取站点统计
     */
    public List<SiteStats> getSiteStats(Long siteId, String startDate, String endDate) {
        return statsMapper.findByDateRange(siteId, startDate, endDate);
    }
    
    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
    
    /**
     * 清理过期的访客记录
     */
    public void cleanupOldVisitors() {
        String yesterday = LocalDateTime.now().minusDays(1).format(DATE_FORMAT);
        dailyVisitors.keySet().removeIf(key -> key.endsWith(yesterday));
    }
} 