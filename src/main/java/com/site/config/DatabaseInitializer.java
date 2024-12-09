package com.site.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private static final String DB_FILE = "site.db";

    @Override
    public void run(String... args) {
        File dbFile = new File(DB_FILE);
        
        if (!dbFile.exists()) {
            log.info("数据库文件不存在，开始初始化...");
            
            // 创建配置表
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS site_config (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "config_key VARCHAR(50) NOT NULL UNIQUE," +
                "config_value TEXT," +
                "enabled BOOLEAN DEFAULT 1," +
                "create_time DATETIME," +
                "update_time DATETIME" +
                ")"
            );
            
            // 创建索引
            jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_config_key ON site_config(config_key)"
            );
            
            // 插入默认配置
            jdbcTemplate.update(
                "INSERT INTO site_config " +
                "(config_key, config_value, enabled, create_time, update_time) " +
                "VALUES (?, ?, ?, datetime('now'), datetime('now'))",
                "base_url", "https://sci.ncut.edu.cn", 1
            );
            
            log.info("数据库初始化完成");
        } else {
            log.info("数据库文件已存在，跳过初始化");
        }
    }
} 