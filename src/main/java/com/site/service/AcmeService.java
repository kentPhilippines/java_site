package com.site.service;

import com.site.entity.Site;
import com.site.entity.SiteCertificate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.util.*;
import org.shredzone.acme4j.exception.AcmeException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import java.security.cert.X509Certificate;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcmeService {

    private final CertificateService certificateService;
    private final KeyStoreUtil keyStoreUtil;
    private static final String CERT_DIR = "certs";
    private static final String ACCOUNT_KEY_FILE = "certs/account.key";
    private static final String ACME_SERVER = "acme://letsencrypt.org";
    private static final DateTimeFormatter SQL_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 存储HTTP验证的token和响应
    private final ConcurrentHashMap<String, String> challengeMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            createCertDirectory();
            ensureAccountKeyExists();
            log.info("ACME服务初始化完成");
        } catch (Exception e) {
            log.error("初始化ACME服务失败", e);
        }
    }

    private void createCertDirectory() throws IOException {
        Path certDir = Paths.get(CERT_DIR);
        if (!Files.exists(certDir)) {
            Files.createDirectories(certDir);
            log.info("创建证书目录: {}", certDir);
        }
    }

    private void ensureAccountKeyExists() throws Exception {
        Path accountKeyPath = Paths.get(ACCOUNT_KEY_FILE);
        if (!Files.exists(accountKeyPath)) {
            log.info("账户密钥不存在，正在生成新的密钥对...");
            KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
            try (FileWriter fw = new FileWriter(accountKeyPath.toFile())) {
                KeyPairUtils.writeKeyPair(keyPair, fw);
            }
            log.info("生成新的ACME账户密钥: {}", accountKeyPath);
        } else {
            log.info("使用现有的ACME账户密钥: {}", accountKeyPath);
        }
    }

    private KeyPair loadOrCreateAccountKey() throws Exception {
        File accountKeyFile = new File(ACCOUNT_KEY_FILE);
        if (accountKeyFile.exists()) {
            log.info("正在加载现有的账户密钥...");
            try (FileReader fr = new FileReader(accountKeyFile)) {
                return KeyPairUtils.readKeyPair(fr);
            }
        } else {
            log.info("正在生成新的账户密钥...");
            KeyPair keyPair = KeyPairUtils.createKeyPair(2048);
            try (FileWriter fw = new FileWriter(accountKeyFile)) {
                KeyPairUtils.writeKeyPair(keyPair, fw);
            }
            return keyPair;
        }
    }

    public String getChallengeResponse(String token) {
        String response = challengeMap.get(token);
        log.debug("收到验证请求 - Token: {}, Response: {}", token, response);
        return response;
    }

    private void checkDomainAccessibility(String domain, String token, String expectedResponse) throws Exception {
        String checkUrl = "http://" + domain + "/.well-known/acme-challenge/" + token;
        log.info("正在检查验证URL的可访问性: {}", checkUrl);
        
        try {
            URL url = new URL(checkUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            log.info("验证URL响应状态码: {}", responseCode);
            
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String actualResponse = reader.readLine();
                    log.info("验证URL返回内容: {}", actualResponse);
                    if (!expectedResponse.equals(actualResponse)) {
                        throw new Exception("验证响应内容不匹配 - 期望: " + expectedResponse + ", 实际: " + actualResponse);
                    }
                }
            } else {
                throw new Exception("验证URL返回非200状态码: " + responseCode);
            }
        } catch (Exception e) {
            log.error("域名验证检查失败: {}", e.getMessage());
            throw new Exception("域名验证检查失败: " + e.getMessage());
        }
    }

    public void requestCertificate(Site site) throws Exception {
        String domain = site.getName();
        log.info("开始为域名 {} 申请证书", domain);

        try {
            log.info("第1步: 准备证书申请环境");
            createCertDirectory();
            KeyPair accountKey = loadOrCreateAccountKey();

            log.info("第2步: 创建证书记录");
            SiteCertificate cert = new SiteCertificate();
            cert.setSiteId(site.getId());
            cert.setDomain(domain);
            cert.setStatus(SiteCertificate.STATUS_PENDING);
            cert.setAutoRenew(true);
            cert.setCertType(SiteCertificate.TYPE_ACME);
            cert.setCreatedAt(LocalDateTime.now().format(SQL_TIMESTAMP));
            certificateService.saveCertificate(cert);

            log.info("第3步: 创建ACME会话和账户");
            Session session = new Session(ACME_SERVER);
            Account account = new AccountBuilder()
                    .agreeToTermsOfService()
                    .useKeyPair(accountKey)
                    .create(session);
            log.info("ACME账户创建成功");

            log.info("��4步: 创建证书订单");
            Order order = account.newOrder()
                    .domains(domain)
                    .create();
            log.info("证书订单创建成功");

            log.info("第5步: 开始域名验证");
            for (Authorization auth : order.getAuthorizations()) {
                Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
                if (challenge == null) {
                    throw new Exception("无法获取HTTP验证挑战");
                }
                
                String token = challenge.getToken();
                String authorization = challenge.getAuthorization();
                log.info("获取到HTTP验证挑战 - Token: {}, Authorization: {}", token, authorization);
                
                // 存储验证信息
                challengeMap.put(token, authorization);
                
                // 检查验证URL是否可访问
                log.info("等待5秒让验证信息生效...");
                Thread.sleep(5000);
                checkDomainAccessibility(domain, token, authorization);
                
                log.info("触发验证...");
                challenge.trigger();
                
                log.info("等待验证完成...");
                int attempts = 0;
                while (auth.getStatus() != Status.VALID) {
                    if (auth.getStatus() == Status.INVALID) {
                        log.error("域名验证失败 - 状态: {}, 详细信息: {}", 
                            auth.getStatus(), challenge.getJSON());
                        throw new Exception("域名验证失败: " + challenge.getJSON());
                    }
                    if (++attempts > 20) { // 最多等待60秒
                        throw new Exception("域名验证超时");
                    }
                    log.debug("当前验证状态: {}, 尝试次数: {}", auth.getStatus(), attempts);
                    Thread.sleep(3000);
                    auth.update();
                }
                log.info("域名验证成功");
            }

            log.info("第6步: 生成域名密钥对和CSR");
            KeyPair domainKeyPair = KeyPairUtils.createKeyPair(2048);
            CSRBuilder csrBuilder = new CSRBuilder();
            csrBuilder.addDomain(domain);
            csrBuilder.sign(domainKeyPair);
            log.info("CSR生成成功");

            log.info("第7步: 请求证书签发");
            order.execute(csrBuilder.getEncoded());
            while (order.getStatus() != Status.VALID) {
                if (order.getStatus() == Status.INVALID) {
                    log.error("证书签发失败 - 状态: {}", order.getStatus());
                    throw new Exception("证书签发失败");
                }
                log.debug("当前订单状态: {}", order.getStatus());
                Thread.sleep(3000L);
                order.update();
            }
            log.info("证书签发成功");

            log.info("第8步: 保存证书文件");
            Path certPath = Paths.get(CERT_DIR, domain);
            Files.createDirectories(certPath);

            // 保存私钥
            log.info("保存私钥...");
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(certPath.resolve("privkey.pem").toFile()))) {
                pemWriter.writeObject(domainKeyPair.getPrivate());
            }

            // 获取并保存证书
            log.info("保存证书...");
            org.shredzone.acme4j.Certificate certificate = order.getCertificate();
            X509Certificate x509Certificate = certificate.getCertificate();
            
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(certPath.resolve("cert.pem").toFile()))) {
                pemWriter.writeObject(x509Certificate);
            }

            log.info("保存证书链...");
            try (JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter(certPath.resolve("chain.pem").toFile()))) {
                for (X509Certificate cert509 : certificate.getCertificateChain()) {
                    pemWriter.writeObject(cert509);
                }
            }

            log.info("第9步: 更新证书记录");
            cert.setStatus(SiteCertificate.STATUS_ACTIVE);
            cert.setCertFile(certPath.resolve("cert.pem").toString());
            cert.setKeyFile(certPath.resolve("privkey.pem").toString());
            cert.setChainFile(certPath.resolve("chain.pem").toString());
            cert.setExpiresAt(LocalDateTime.ofInstant(
                x509Certificate.getNotAfter().toInstant(),
                ZoneId.systemDefault()
            ).format(SQL_TIMESTAMP));
            certificateService.saveCertificate(cert);

            // 创建密钥库
            keyStoreUtil.createKeyStore(
                domain,
                cert.getCertFile(),
                cert.getKeyFile(),
                cert.getChainFile()
            );

            // 清理验证信息
            challengeMap.clear();
            
            log.info("证书申请完成: {}", domain);
            log.info("证书有效期至: {}", cert.getExpiresAt());
        } catch (Exception e) {
            log.error("申请证书失败: {} - {}", domain, e.getMessage(), e);
            throw new RuntimeException("申请证书失败: " + e.getMessage());
        }
    }
} 