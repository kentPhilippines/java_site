package com.site.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class KeyStoreUtil {

    private static final String KEYSTORE_PASSWORD = "changeit";

    public void createKeyStore(String domain, String certPath, String keyPath, String chainPath) {
        try {
            // 读取证书文件
            X509Certificate cert = readCertificate(certPath);
            
            // 读取证书链
            List<X509Certificate> chain = readCertificateChain(chainPath);
            chain.add(0, cert); // 将主证书添加到链的开头
            
            // 读取私钥
            PrivateKey privateKey = readPrivateKey(keyPath);
            
            // 创建密钥库
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            
            // 将证书和私钥存入密钥库
            keyStore.setKeyEntry(
                domain,
                privateKey,
                KEYSTORE_PASSWORD.toCharArray(),
                chain.toArray(new Certificate[0])
            );
            
            // 保存密钥库
            String keystorePath = "certs/" + domain + "/keystore.p12";
            try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
                keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
            }
            
            log.info("成功创建密钥库: {}", keystorePath);
            
        } catch (Exception e) {
            log.error("创建密钥库失败", e);
            throw new RuntimeException("创建密钥库失败: " + e.getMessage());
        }
    }
    
    private X509Certificate readCertificate(String certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }
    
    private List<X509Certificate> readCertificateChain(String chainPath) throws Exception {
        List<X509Certificate> chain = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(chainPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            while (fis.available() > 0) {
                chain.add((X509Certificate) cf.generateCertificate(fis));
            }
        }
        return chain;
    }
    
    private PrivateKey readPrivateKey(String keyPath) throws Exception {
        // 这里需要根据私钥文件的格式来实现具体的读取逻辑
        // 通常需要使用 BouncyCastle 来解析 PEM 格式的私钥
        // 这里仅作示例
        throw new UnsupportedOperationException("需要实现私钥读取逻辑");
    }
} 