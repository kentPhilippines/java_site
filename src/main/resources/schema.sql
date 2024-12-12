-- 站点配置表
CREATE TABLE IF NOT EXISTS site_config (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_key VARCHAR(50) NOT NULL UNIQUE,
    config_value TEXT,
    enabled BOOLEAN DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 站点表
CREATE TABLE IF NOT EXISTS site (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    url VARCHAR(255) NOT NULL,
    description TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    enabled INTEGER DEFAULT 1,
    is_cache INTEGER DEFAULT 1,
    is_ssl INTEGER DEFAULT 1,
    sitemap INTEGER DEFAULT 1,
    sync_source VARCHAR(255)
);
 

-- 证书表
CREATE TABLE IF NOT EXISTS site_certificates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    site_id INTEGER NOT NULL,
    domain VARCHAR(255) NOT NULL,
    cert_type VARCHAR(10) NOT NULL,
    cert_file VARCHAR(255),
    key_file VARCHAR(255),
    chain_file VARCHAR(255),
    status VARCHAR(10) NOT NULL,
    auto_renew BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME,
    FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE
);

-- 站点访问统计表
CREATE TABLE IF NOT EXISTS site_stats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    site_id INTEGER NOT NULL,
    domain TEXT NOT NULL,
    visits INTEGER DEFAULT 0,
    unique_visits INTEGER DEFAULT 0,
    bandwidth INTEGER DEFAULT 0,
    date TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (site_id) REFERENCES sites(id) ON DELETE CASCADE
);

-- 关键字替换规则表
CREATE TABLE IF NOT EXISTS site_keywords (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    site_id INTEGER NOT NULL,
    source_word TEXT NOT NULL,
    target_word TEXT NOT NULL,
    page_url TEXT,
    enabled INTEGER DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_config_key ON site_config(config_key);
CREATE INDEX IF NOT EXISTS idx_site_name ON site(name);
CREATE INDEX IF NOT EXISTS idx_target_domain ON target_site(domain);
CREATE INDEX IF NOT EXISTS idx_cert_domain ON site_certificates(domain);
CREATE INDEX IF NOT EXISTS idx_cert_status ON site_certificates(status);
CREATE INDEX IF NOT EXISTS idx_site_stats_site_id ON site_stats(site_id);
CREATE INDEX IF NOT EXISTS idx_site_stats_date ON site_stats(date);
CREATE UNIQUE INDEX IF NOT EXISTS idx_site_stats_site_date ON site_stats(site_id, date);
CREATE INDEX IF NOT EXISTS idx_keywords_site_id ON site_keywords(site_id);
CREATE INDEX IF NOT EXISTS idx_keywords_source ON site_keywords(source_word);

-- 插入默认配置
INSERT OR IGNORE INTO site_config (config_key, config_value, enabled) 
VALUES ('base_url', 'https://sci.ncut.edu.cn', 1); 