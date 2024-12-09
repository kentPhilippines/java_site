package com.site.util;

import com.site.config.SSLConfigManager;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyStoreUtil {

    private final SSLConfigManager sslConfigManager;
    private static final String KEYSTORE_PASSWORD = "changeit";

    public void createKeyStore(String domain, String certPath, String keyPath, String chainPath) {
        try {
            // 读取证书文件
            X509Certificate cert = readCertificate(certPath);
            
            // 读取证书链
            List<X509Certificate> chain = readCertificateChain(chainPath);
            
            // 组合完整的证书链
            List<Certificate> fullChain = new ArrayList<>();
            fullChain.add(cert); // 添加主证书
            fullChain.addAll(chain); // 添加证书链
            
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
                fullChain.toArray(new Certificate[0])
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
            
            // 配置SSL
            sslConfigManager.addCertificate(domain, keystorePath);
            
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
        try (FileReader keyReader = new FileReader(keyPath);
             PEMParser pemParser = new PEMParser(keyReader)) {
            
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            
            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                return converter.getKeyPair((org.bouncycastle.openssl.PEMKeyPair) object).getPrivate();
            } else {
                throw new IllegalArgumentException("不支持的私钥格式");
            }
        }
    }
} 