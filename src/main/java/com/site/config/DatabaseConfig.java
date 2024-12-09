package com.site.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import java.io.File;

@Slf4j
@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource dataSource() {
        // 使用基本数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(jdbcUrl);
        
        // SQLite特定配置

        // 确保数据库文件存在
        try {
            String dbPath = jdbcUrl.replace("jdbc:sqlite:", "");
            File dbFile = new File(dbPath);
            
            // 如果是相对路径，转换为绝对路径
            if (!dbFile.isAbsolute()) {
                dbFile = new File(System.getProperty("user.dir"), dbPath);
            }
            
            // 创建数据库目录
            File dbDir = dbFile.getParentFile();
            if (dbDir != null && !dbDir.exists()) {
                dbDir.mkdirs();
            }

            // 如果数据库文件不存在，执行初始化脚本
            boolean needInit = !dbFile.exists();
            
            log.info("数据源配置完成: {}", dbFile.getAbsolutePath());
            
            if (needInit) {
                log.info("开始初始化数据库...");
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema.sql"));
                populator.setContinueOnError(true);
                populator.execute(dataSource);
                log.info("数据库初始化完成");
            }
        } catch (Exception e) {
            log.error("数据库初始化失败", e);
            throw new RuntimeException("数据库初始化失败", e);
        }
        
        return dataSource;
    }
} 