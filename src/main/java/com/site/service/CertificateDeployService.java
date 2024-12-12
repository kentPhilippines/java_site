package com.site.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.SocketTimeoutException;
import org.springframework.web.client.ResourceAccessException;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateDeployService {
    
    @Value("${cert.service.url:http://localhost:8000}")
    private String certServiceUrl;
    
    @Value("${server.address:0.0.0.0}")
    private String serverAddress;
    
    @Value("${server.port:9099}")
    private int serverPort;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String API_BASE_PATH = "/api/v1";
    private static final int MAX_RETRIES = 1;
    private static final long RETRY_DELAY_MS = 20000 * 2; // 10秒
    
    /**
     * 带重试机制的HTTP请求
     */
    private <T> ResponseEntity<T> executeWithRetry(String url, HttpMethod method, 
            HttpEntity<?> request, Class<T> responseType) {
        Exception lastException = null;
        
        for (int i = 0; i <= MAX_RETRIES; i++) {
            try {
                if (i > 0) {
                    log.info("第 {} 次重试请求: {}", i, url);
                    Thread.sleep(RETRY_DELAY_MS);
                }
                
                return restTemplate.exchange(url, method, request, responseType);
            } catch (ResourceAccessException e) {
                if (e.getCause() instanceof SocketTimeoutException) {
                    log.warn("请求超时: {}", url);
                } else {
                    log.warn("请求失败: {}", e.getMessage());
                }
                lastException = e;
            } catch (Exception e) {
                log.warn("请求失败: {}", e.getMessage());
                lastException = e;
            }
        }
        
        throw new RuntimeException("请求失败，已重试 " + MAX_RETRIES + " 次", lastException);
    }
    
    /**
     * 申请SSL证书
     * @param domain 域名
     * @param email 邮箱
     * @return 证书信息，包含证书路径等
     */
    public Map<String, Object> requestCertificate(String domain, String email) {
        try {
            String url = certServiceUrl + API_BASE_PATH + "/deploy/sites";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("domain", domain);
            requestBody.put("enable_ssl", true);
            requestBody.put("ssl_email", email);
            requestBody.put("proxy_ip", serverAddress.equals("0.0.0.0") ? getLocalIp() : serverAddress);
            requestBody.put("proxy_port", serverPort);
            requestBody.put("proxy_host", domain);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<Map> response = executeWithRetry(url, HttpMethod.POST, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    log.info("域名 {} 的SSL证书申请成功", domain);
                    return (Map<String, Object>) responseBody.get("data");
                } else {
                    log.error("域名 {} 的SSL证书申请失败: {}", domain, responseBody.get("message"));
                    throw new RuntimeException(responseBody.get("message").toString());
                }
            } else {
                log.error("域名 {} 的SSL证书申请失败: {}", domain, response.getBody());
                throw new RuntimeException("SSL证书申请失败");
            }
        } catch (Exception e) {
            log.error("域名 {} 的SSL证书申请请求失败: {}", domain, e.getMessage(), e);
            throw new RuntimeException("SSL证书申请请求失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除SSL证书
     * @param domain 域名
     * @return 删除结果
     */
    public boolean removeCertificate(String domain) {
        try {
            String url = certServiceUrl + API_BASE_PATH + "/deploy/sites/" + domain;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Void> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = executeWithRetry(url, HttpMethod.DELETE, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (Boolean.TRUE.equals(responseBody.get("success"))) {
                    Map<String, Object> data = (Map<String, Object>) responseBody.get("data");
                    boolean configRemoved = Boolean.TRUE.equals(data.get("config_removed"));
                    boolean sslRemoved = Boolean.TRUE.equals(data.get("ssl_removed"));
                    log.info("域名 {} 的证书删除成功: 配置文件已删除: {}, SSL已删除: {}", 
                            domain, configRemoved, sslRemoved);
                    return true;
                } else {
                    log.error("域名 {} 的证书删除失败: {}", domain, responseBody.get("message"));
                    return false;
                }
            } else {
                log.error("域名 {} 的证书删除失败: {}", domain, response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("域名 {} 的证书删除请求失败: {}", domain, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取证书状态
     * @param domain 域名
     * @return 证书状态信息
     */
    public Map<String, Object> getCertificateStatus(String domain) {
        try {
            String url = certServiceUrl + API_BASE_PATH + "/sites/" + domain;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = executeWithRetry(url, HttpMethod.GET, request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Map<String, Object> sslInfo = (Map<String, Object>) responseBody.get("ssl_info");
                if (sslInfo != null) {
                    boolean certExists = Boolean.TRUE.equals(sslInfo.get("cert_exists"));
                    boolean keyExists = Boolean.TRUE.equals(sslInfo.get("key_exists"));
                    log.info("域名 {} 的证书状态: 证书文件存在: {}, 密钥文件存在: {}", 
                            domain, certExists, keyExists);
                }
                return responseBody;
            }
            return null;
        } catch (Exception e) {
            log.error("获取域名 {} 的证书状态失败: {}", domain, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 获取所有站点列表
     * @return 站点列表
     */
    public List<Map<String, Object>> getAllSites() {
        try {
            String url = certServiceUrl + API_BASE_PATH + "/sites";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<?> request = new HttpEntity<>(headers);
            
            ResponseEntity<List> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                request,
                List.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> sites = response.getBody();
                log.info("获取站点列表成功，共 {} 个站点", sites.size());
                return sites;
            }
            return null;
        } catch (Exception e) {
            log.error("获取站点列表失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    // 获取本机IP地址
    private String getLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
} 