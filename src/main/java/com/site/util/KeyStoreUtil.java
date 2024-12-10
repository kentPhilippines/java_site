package com.site.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.List;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeyStoreUtil {

    private static final String KEYSTORE_PASSWORD = "changeit";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public void createKeyStore(String domain, String certPath, String keyPath, List<String> chainPaths) {
        try {
            // 创建新的密钥库
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);

            // 读取私钥
            PrivateKey privateKey = loadPrivateKey(keyPath);

            // 读取证书链
            Certificate[] chain = loadCertificateChain(certPath, chainPaths);

            // 将私钥和证书链存入密钥库
            keyStore.setKeyEntry(domain, privateKey, KEYSTORE_PASSWORD.toCharArray(), chain);

            // 保存密钥库到文件
            String keystorePath = new File(certPath).getParent() + "/keystore.p12";
            try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
                keyStore.store(fos, KEYSTORE_PASSWORD.toCharArray());
            }
            
            log.info("成功创建密钥库: {}", keystorePath);
            
        } catch (Exception e) {
            log.error("创建密钥库失败", e);
            throw new RuntimeException("创建密钥库失败: " + e.getMessage());
        }
    }

    private PrivateKey loadPrivateKey(String keyPath) throws Exception {
        try (FileReader fr = new FileReader(keyPath);
             BufferedReader br = new BufferedReader(fr)) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("-----")) {
                    sb.append(line);
                }
            }
            byte[] encoded = java.util.Base64.getDecoder().decode(sb.toString());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        }
    }

    private Certificate[] loadCertificateChain(String certPath, List<String> chainPaths) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate[] chain = new Certificate[chainPaths.size() + 1];

        // 加载域名证书
        try (FileInputStream fis = new FileInputStream(certPath)) {
            chain[0] = cf.generateCertificate(fis);
        }

        // 加载中间证书
        for (int i = 0; i < chainPaths.size(); i++) {
            try (FileInputStream fis = new FileInputStream(chainPaths.get(i))) {
                chain[i + 1] = cf.generateCertificate(fis);
            }
        }

        return chain;
    }
} 