package com.site.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.init.ScriptException;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseInitializer {

    @Value("classpath:schema.sql")
    private Resource schemaResource;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    private final DataSource dataSource;

    @Bean
    public CommandLineRunner initDatabase() {
        return args -> {
            log.info("开始初始化数据库...");
            try {
                // 从JDBC URL中提取数据库文件路径
                String dbPath = jdbcUrl.replace("jdbc:sqlite:", "");
                File dbFile = new File(dbPath);
                
                // 确保数据库目录存在
                if (!dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                
                // 初始化数据库结构
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(schemaResource);
                populator.setContinueOnError(true);
                
                try (Connection connection = dataSource.getConnection()) {
                    populator.populate(connection);
                    log.info("数据库初始化完成: {}", dbFile.getAbsolutePath());
                }
            } catch (ScriptException | SQLException e) {
                log.error("数据库初始化失败: {}", e.getMessage(), e);
                throw new RuntimeException("数据库初始化失败", e);
            }
        };
    }
} 