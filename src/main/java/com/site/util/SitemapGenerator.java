package com.site.util;

import com.site.entity.Site;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.HashSet;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Slf4j
@Component
@RequiredArgsConstructor
public class SitemapGenerator {
    
    private final HttpUtils httpUtils;
    private static final String SITEMAP_DIR = "sitemaps";
    private static final String SITEMAP_FILE = "sitemap.xml";
    public void generateSitemap(Site site) {
        if (site.getSitemap() == 0) {
            log.info("站点 {} 未启用网站地图功能", site.getName());
            return;
        }
        
        try {
            String syncSource = site.getSyncSource();
            if(syncSource == null || syncSource.isEmpty()) {
                //如果没有生成本地文件，则不生成网站地图
                return;
            }
            // 创建sitemap目录
            File dir = new File(syncSource + File.separator + SITEMAP_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // 获取所有URL
            Set<String> urls = crawlUrls(site.getUrl());
            

            // 生成sitemap文件
            String filename = syncSource + File.separator + SITEMAP_DIR + File.separator + SITEMAP_FILE;
            writeSitemapXml(urls, filename, site.getUrl());
            
            log.info("站点 {} 的网站地图生成完成: {}", site.getName(), filename);
            
        } catch (Exception e) {
            log.error("生成网站地图失败: " + site.getName(), e);
        }
    }
    
    private Set<String> crawlUrls(String baseUrl) throws IOException {
        Set<String> urls = new HashSet<>();
        urls.add(baseUrl); // 添加首页
        
        try {
            String html = httpUtils.get(baseUrl);
            Document doc = Jsoup.parse(html);
            Elements links = doc.select("a[href]");
            
            for (Element link : links) {
                String url = link.attr("abs:href");
                if (isValidUrl(url, baseUrl)) {
                    urls.add(url);
                }
            }
        } catch (Exception e) {
            log.error("抓取URL失败: " + baseUrl, e);
        }
        
        return urls;
    }
    
    private boolean isValidUrl(String url, String baseUrl) {
        return url != null && 
               url.startsWith(baseUrl) && 
               !url.contains("#") && 
               !url.endsWith(".jpg") && 
               !url.endsWith(".png") && 
               !url.endsWith(".gif") && 
               !url.endsWith(".css") && 
               !url.endsWith(".js");
    }
    
    private void writeSitemapXml(Set<String> urls, String filename, String baseUrl) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
            
            String lastmod = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE);
            
            for (String url : urls) {
                writer.write("  <url>\n");
                writer.write("    <loc>" + url + "</loc>\n");
                writer.write("    <lastmod>" + lastmod + "</lastmod>\n");
                writer.write("    <changefreq>daily</changefreq>\n");
                writer.write("    <priority>" + getPriority(url, baseUrl) + "</priority>\n");
                writer.write("  </url>\n");
            }
            
            writer.write("</urlset>");
        }
    }
    
    private double getPriority(String url, String baseUrl) {
        if (url.equals(baseUrl)) {
            return 1.0; // 首页优先级最高
        }
        int depth = url.split("/").length - baseUrl.split("/").length;
        return Math.max(0.1, 1.0 - (depth * 0.2)); // 层级越深优先级越低
    }
} 