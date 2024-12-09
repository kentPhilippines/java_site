package com.site.service;

import com.site.entity.Site;
import com.site.entity.SiteCertificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcmeService {

    private final CertificateService certificateService;
    private static final String LETS_ENCRYPT_STAGING = "acme://letsencrypt.org/staging";
    private static final String LETS_ENCRYPT_PROD = "acme://letsencrypt.org";

    @Value("${ssl.cert-path}")
    private String certBasePath;

    // 存储HTTP验证的token��内容
    private final ConcurrentHashMap<String, String> challengeMap = new ConcurrentHashMap<>();

    public String getChallengeResponse(String token) {
        return challengeMap.get(token);
    }

    public void requestCertificate(Site site) {
        try {
            String domain = site.getName();
            // 创建账户密钥对
            Path accountKeyPath = Paths.get(certBasePath, "account.key");
            KeyPair accountKeyPair;
            if (Files.exists(accountKeyPath)) {
                try (FileReader fr = new FileReader(accountKeyPath.toFile())) {
                    accountKeyPair = KeyPairUtils.readKeyPair(fr);
                }
            } else {
                accountKeyPair = KeyPairUtils.createKeyPair(2048);
                try (FileWriter fw = new FileWriter(accountKeyPath.toFile())) {
                    KeyPairUtils.writeKeyPair(accountKeyPair, fw);
                }
            }

            // 创建ACME会话
            Session session = new Session(LETS_ENCRYPT_PROD);
            Account account = new AccountBuilder()
                    .agreeToTermsOfService()
                    .useKeyPair(accountKeyPair)
                    .create(session);

            // 开始订单流程
            Order order = account.newOrder()
                    .domains(domain)
                    .create();

            // 获取HTTP验证
            for (Authorization auth : order.getAuthorizations()) {
                Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
                challengeMap.put(challenge.getToken(), challenge.getAuthorization());
                challenge.trigger();
            }

            // 等待验证完成
            while (order.getStatus() != Status.READY) {
                Thread.sleep(3000L);
                order.update();
            }

            // 生成域名密钥对和CSR
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(2048);
            CSRBuilder csrBuilder = new CSRBuilder();
            csrBuilder.addDomain(domain);
            csrBuilder.sign(domainKeyPair);

            // 完成订单
            order.execute(csrBuilder.getEncoded());
            while (order.getStatus() != Status.VALID) {
                Thread.sleep(3000L);
                order.update();
            }

            // 保存证书
            Path certPath = Paths.get(certBasePath, domain);
            Files.createDirectories(certPath);

            // 保存私钥
            try (FileWriter fw = new FileWriter(certPath.resolve("privkey.pem").toFile())) {
                KeyPairUtils.writeKeyPair(domainKeyPair, fw);
            }

            // 保存证书
            Certificate certificate = order.getCertificate();
            try (FileWriter fw = new FileWriter(certPath.resolve("cert.pem").toFile())) {
                certificate.writeCertificate(fw);
            }

            // 保存证书链
            try (FileWriter fw = new FileWriter(certPath.resolve("chain.pem").toFile())) {
                certificate.writeCertificate(fw);
            }

            // 创建证书记录
            SiteCertificate siteCert = new SiteCertificate();
            siteCert.setSiteId(site.getId());
            siteCert.setDomain(domain);
            siteCert.setCertType(SiteCertificate.TYPE_AUTO);
            siteCert.setCertFile(certPath.resolve("cert.pem").toString());
            siteCert.setKeyFile(certPath.resolve("privkey.pem").toString());
            siteCert.setChainFile(certPath.resolve("chain.pem").toString());
            siteCert.setStatus(SiteCertificate.STATUS_ACTIVE);
            siteCert.setAutoRenew(true);
            siteCert.setCreatedAt(LocalDateTime.now());
            siteCert.setExpiresAt(LocalDateTime.ofInstant(
                certificate.getCertificate().getNotAfter().toInstant(), 
                ZoneId.systemDefault()
            ));

            certificateService.saveCertificate(siteCert);

            // 清理验证信息
            challengeMap.clear();

        } catch (Exception e) {
            log.error("申请证书失败: {}", e.getMessage(), e);
            throw new RuntimeException("申请证书失败: " + e.getMessage());
        }
    }
} 