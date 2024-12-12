package com.site.task;

import com.site.entity.Site;
import com.site.service.SiteService;
import com.site.util.CacheUtil;
import com.site.util.HttpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.*;
import java.util.concurrent.*;
import java.net.URI;
import java.net.URISyntaxException;
import org.springframework.beans.factory.annotation.Value;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheTask {
    
    private final SiteService siteService;
    private final HttpUtils httpUtils;
    private final CacheUtil cacheUtil;
    
    @Value("${cache.task.thread.pool.size:10}")
    private int threadPoolSize;
    
    @Value("${cache.task.queue.size:1000}")
    private int queueSize;
    
    @Value("${cache.task.batch.size:100}")
    private int batchSize;
    
    private ThreadPoolExecutor executor;
    private ScheduledExecutorService scheduledExecutor;
    private final Map<String, ConcurrentHashMap<String, Boolean>> siteVisitedUrls = new ConcurrentHashMap<>();
    private final AtomicInteger activeTaskCount = new AtomicInteger(0);
    
    @PostConstruct
    public void init() {
        // 创建有界队列的线程池
        executor = new ThreadPoolExecutor(
            threadPoolSize,
            threadPoolSize,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueSize),
            new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时，在调用线程执行任务
        );
        
        // 创建调度线程池
        scheduledExecutor = Executors.newScheduledThreadPool(1);
        
        // 启动监控任务
        scheduledExecutor.scheduleAtFixedRate(this::monitorTasks, 1, 1, TimeUnit.MINUTES);
    }
    
    @PreDestroy
    public void destroy() {
        executor.shutdown();
        scheduledExecutor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.MINUTES);
            scheduledExecutor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void monitorTasks() {
        log.info("缓存任务监控 - 活跃任务数: {}, 线程池大小: {}, 队列大小: {}", 
                activeTaskCount.get(), executor.getPoolSize(), executor.getQueue().size());
    }

  //  @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void updateCache() {
        if (activeTaskCount.get() > 0) {
            log.warn("上一轮缓存任务尚未完成，跳过本次执行");
            return;
        }
        
        log.info("开始更新缓存...");
        
        Site query = new Site();
        query.setIsCache(1);
        query.setEnabled(1);
        
        List<Site> sites = siteService.getAllSites(query);
        
        // 按批次处理站点
        for (int i = 0; i < sites.size(); i += batchSize) {
            int end = Math.min(i + batchSize, sites.size());
            List<Site> batch = sites.subList(i, end);
            
            CountDownLatch batchLatch = new CountDownLatch(batch.size());
            activeTaskCount.addAndGet(batch.size());
            
            for (Site site : batch) {
                executor.submit(() -> {
                    try {
                        processSite(site);
                    } finally {
                        batchLatch.countDown();
                        activeTaskCount.decrementAndGet();
                    }
                });
            }
            
            try {
                // 等待当前批次完成
                if (!batchLatch.await(30, TimeUnit.MINUTES)) {
                    log.warn("批次处理超时");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("批次等待被中断", e);
            }
        }
        
        log.info("缓存更新任务提交完成");
    }
    
    private void processSite(Site site) {
        try {
            // 为每个站点创建新的访问记录
            ConcurrentHashMap<String, Boolean> visitedUrls = new ConcurrentHashMap<>();
            siteVisitedUrls.put(site.getName(), visitedUrls);
            
            // 尝试https
            String baseUrl = String.format("https://%s", site.getName());
            try {
                httpUtils.get(baseUrl);
                processUrl(baseUrl, site, 0, visitedUrls);
            } catch (Exception e) {
                log.info("HTTPS访问失败，尝试HTTP: {}", site.getName());
                // https失败，尝试http
                baseUrl = String.format("http://%s", site.getName());
                processUrl(baseUrl, site, 0, visitedUrls);
            }
        } catch (Exception e) {
            log.error("站点 {} 缓存更新失败: {}", site.getName(), e.getMessage());
        } finally {
            // 清理站点的访问记录
            siteVisitedUrls.remove(site.getName());
        }
    }
    
    private void processUrl(String currentUrl, Site site, int depth, ConcurrentHashMap<String, Boolean> visitedUrls) {
        if (depth > 5) return;
        
        if (currentUrl.endsWith("/")) {
            currentUrl = currentUrl.substring(0, currentUrl.length() - 1);
        }
        
        if (visitedUrls.putIfAbsent(currentUrl, true) != null) return;
        
        try {
            String content = httpUtils.get(currentUrl);
            log.debug("已缓存页面: {}", currentUrl);
            
            Document doc = Jsoup.parse(content);
            doc.setBaseUri(currentUrl);
            Set<String> urls = Collections.newSetFromMap(new ConcurrentHashMap<>());
            
            // 并行处理所有资源链接
            CompletableFuture.allOf(
                processLinks(doc, currentUrl, site, urls),
                processImages(doc, currentUrl, site, urls),
                processStyles(doc, currentUrl, site, urls),
                processScripts(doc, currentUrl, site, urls)
            ).join();
            
            // 递归处理有HTML页面
            urls.stream()
                .filter(this::isHtmlPage)
                .forEach(url -> processUrl(url, site, depth + 1, visitedUrls));
            
            // 并行处理所有静态资源
            urls.stream()
                .filter(this::isStaticResource)
                .forEach(url -> {
                    try {
                        httpUtils.get(url);
                        log.debug("已缓存资源: {}", url);
                    } catch (Exception e) {
                        log.error("缓存资源失败: {} - {}", url, e.getMessage());
                    }
                });
            
        } catch (Exception e) {
            log.error("处理URL失败: {} - {}", currentUrl, e.getMessage());
        }
    }
    
    private CompletableFuture<Void> processLinks(Document doc, String currentUrl, Site site, Set<String> urls) {
        return CompletableFuture.runAsync(() -> {
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String href = link.attr("href");
                String absoluteUrl = resolveUrl(currentUrl, href, site);
                if (isValidUrl(absoluteUrl, site)) {
                    urls.add(absoluteUrl);
                }
            }
        }, executor);
    }
    
    private CompletableFuture<Void> processImages(Document doc, String currentUrl, Site site, Set<String> urls) {
        return CompletableFuture.runAsync(() -> {
            Elements images = doc.select("img[src]");
            for (Element img : images) {
                String src = img.attr("src");
                String absoluteUrl = resolveUrl(currentUrl, src, site);
                if (isValidUrl(absoluteUrl, site)) {
                    urls.add(absoluteUrl);
                }
            }
        }, executor);
    }
    
    private CompletableFuture<Void> processStyles(Document doc, String currentUrl, Site site, Set<String> urls) {
        return CompletableFuture.runAsync(() -> {
            Elements cssLinks = doc.select("link[rel=stylesheet]");
            for (Element css : cssLinks) {
                String href = css.attr("href");
                String absoluteUrl = resolveUrl(currentUrl, href, site);
                if (isValidUrl(absoluteUrl, site)) {
                    urls.add(absoluteUrl);
                }
            }
        }, executor);
    }
    
    private CompletableFuture<Void> processScripts(Document doc, String currentUrl, Site site, Set<String> urls) {
        return CompletableFuture.runAsync(() -> {
            Elements scripts = doc.select("script[src]");
            for (Element script : scripts) {
                String src = script.attr("src");
                String absoluteUrl = resolveUrl(currentUrl, src, site);
                if (isValidUrl(absoluteUrl, site)) {
                    urls.add(absoluteUrl);
                }
            }
        }, executor);
    }
    
    private String resolveUrl(String baseUrl, String path, Site site) {
        try {
            if (path == null || path.isEmpty()) {
                return null;
            }
            
            // 获取基础URL的协议
            String protocol = baseUrl.startsWith("https") ? "https" : "http";
            
            // 如果是完整的URL，检查是否属于当前站点
            if (path.startsWith("http://") || path.startsWith("https://")) {
                URI uri = new URI(path);
                if (!uri.getHost().equals(site.getName())) {
                    return null;
                }
                return path;
            }
            
            // 处理 // 开头的URL
            if (path.startsWith("//")) {
                return protocol + ":" + path;
            }
            
            // 处理相对路径
            if (!path.startsWith("/")) {
                URI base = new URI(baseUrl);
                String basePath = base.getPath();
                if (!basePath.endsWith("/")) {
                    basePath = basePath.substring(0, basePath.lastIndexOf('/') + 1);
                }
                path = basePath + path;
            }
            
            // 处理 ../ 和 ./
            path = normalizePath(path);
            
            return String.format("%s://%s%s", protocol, site.getName(), path);
        } catch (URISyntaxException e) {
            log.error("URL解析失败: {} - {}", path, e.getMessage());
            return null;
        }
    }
    
    private String normalizePath(String path) {
        if (path == null) return null;
        
        // 分割路径
        String[] parts = path.split("/");
        java.util.Stack<String> stack = new java.util.Stack<>();
        
        for (String part : parts) {
            if (part.equals("..")) {
                if (!stack.isEmpty()) {
                    stack.pop();
                }
            } else if (!part.equals(".") && !part.isEmpty()) {
                stack.push(part);
            }
        }
        
        // 重建路径
        StringBuilder result = new StringBuilder();
        for (String part : stack) {
            result.append("/").append(part);
        }
        
        return result.length() == 0 ? "/" : result.toString();
    }
    
    private boolean isValidUrl(String url, Site site) {
        if (url == null) return false;
        
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            
            // 检查是否为目录型URL
            boolean isDirectory = path.isEmpty() || path.endsWith("/") || !path.contains(".");
            
            return uri.getHost().equals(site.getName()) && // 同一域名
                   !url.contains("#") &&                   // 排除锚点
                   !url.contains("?") &&                   // 排除参数
                   (isDirectory || isHtmlPage(url) || isStaticResource(url)); // 只处理目录、HTML页面和静态资源
        } catch (URISyntaxException e) {
            return false;
        }
    }
    
    private boolean isHtmlPage(String url) {
        if (url == null) return false;
        String lowercaseUrl = url.toLowerCase();
        return lowercaseUrl.endsWith(".html") || 
               lowercaseUrl.endsWith(".htm") || 
               (!lowercaseUrl.contains(".") && !lowercaseUrl.endsWith("/"));
    }
    
    private boolean isStaticResource(String url) {
        if (url == null) return false;
        String lowercaseUrl = url.toLowerCase();
        return lowercaseUrl.matches(".+\\.(jpg|jpeg|png|gif|css|js|ico|svg|woff|woff2|ttf|eot|mp4|webp|pdf)$");
    }
}
