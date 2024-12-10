package com.site.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import com.site.service.NginxService;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyStoreUtil {

    private final NginxService nginxService;
    private static final String KEYSTORE_PASSWORD = "changeit";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public void createKeyStore(String domain, String certPath, String keyPath, String chainPath) {
        try {
            // 读取私钥
            PrivateKey privateKey = readPrivateKey(keyPath);
            
            // 读取证书链
            List<X509Certificate> certificates = new ArrayList<>();
            
            // 首先读取主证书
            certificates.add(readCertificate(certPath));
            
            // 然后读取证书链
            certificates.addAll(readCertificateChain(chainPath));
            
            // 创建密钥库
            KeyStore keyStore = KeyStore.getInstance("PKCS12", "BC");
            keyStore.load(null, null);
            
            // 将证书和私钥存入密钥库
            keyStore.setKeyEntry(
                domain,
                privateKey,
                KEYSTORE_PASSWORD.toCharArray(),
                certificates.toArray(new Certificate[0])
            );
            
            // 保存密钥库到域名特定目录
            String keystorePath = "certs/" + domain + "/keystore.p12";
            File keystoreFile = new File(keystorePath);
            if (!keystoreFile.getParentFile().exists()) {
                keystoreFile.getParentFile().mkdirs();
            }
            
            try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
                keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
            }
            
            log.info("成功创建密钥库: {}", keystorePath);
            
            // 生成Nginx配置
            nginxService.generateSiteConfig(domain);
            
        } catch (Exception e) {
            log.error("创建密钥库失败", e);
            throw new RuntimeException("创建密钥库失败: " + e.getMessage());
        }
    }
    
    private X509Certificate readCertificate(String certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }
    
    private List<X509Certificate> readCertificateChain(String chainPath) throws Exception {
        List<X509Certificate> chain = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(chainPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509", "BC");
            while (fis.available() > 0) {
                chain.add((X509Certificate) cf.generateCertificate(fis));
            }
        }
        return chain;
    }
    
    private PrivateKey readPrivateKey(String keyPath) throws Exception {
        try (FileReader keyReader = new FileReader(keyPath);
             PEMParser pemParser = new PEMParser(keyReader)) {
            
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            
            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                return converter.getKeyPair((org.bouncycastle.openssl.PEMKeyPair) object).getPrivate();
            } else {
                throw new IllegalArgumentException("不支持的私钥格式");
            }
        }
    }
} 