package com.site.mapper;

import com.site.entity.Site;
import org.apache.ibatis.jdbc.SQL;

public class SiteMapperSqlProvider {
    
    public String insert(Site site) {
        return new SQL() {{
            INSERT_INTO("site");
            
            if (site.getName() != null) {
                VALUES("name", "#{name}");
            }
            if (site.getUrl() != null) {
                VALUES("url", "#{url}");
            }
            if (site.getEnabled() != null) {
                VALUES("enabled", "#{enabled}");
            }
            if (site.getDescription() != null) {
                VALUES("description", "#{description}");
            }
            if (site.getIsCache() != null) {
                VALUES("is_cache", "#{isCache}");
            }
            if (site.getSyncSource() != null) {
                VALUES("sync_source", "#{syncSource}");
            }
            if (site.getSitemap() != null) {
                VALUES("sitemap", "#{sitemap}");
            }
            if (site.getSsl() != null) {
                VALUES("is_ssl", "#{ssl}");
            }
            VALUES("create_time", "datetime('now')");
            VALUES("update_time", "datetime('now')");
        }}.toString();
    }
    
    public String update(Site site) {
        return new SQL() {{
            UPDATE("site");
            if (site.getName() != null) {
                SET("name = #{name}");
            }
            if (site.getUrl() != null) {
                SET("url = #{url}");
            }
            if (site.getEnabled() != null) {
                SET("enabled = #{enabled}");
            }
            if (site.getDescription() != null) {
                SET("description = #{description}");
            }
            if (site.getIsCache() != null) {
                SET("is_cache = #{isCache}");
            }
            if (site.getSitemap() != null) {
                SET("sitemap = #{sitemap}");
            }
            if (site.getSyncSource() != null) {
                SET("sync_source = #{syncSource}");
            }
            if (site.getSsl() != null) {
                SET("ssl = #{ssl}");
            }
            SET("update_time = datetime('now')");
            WHERE("id = #{id}");
        }}.toString();
    }




    public String selectList(Site site){
        return new SQL() {{
            SELECT("id, name, url, enabled, description, is_cache as isCache, sitemap, sync_source as syncSource, is_ssl as  ssl, create_time as createTime, update_time as updateTime");
            FROM("site");
            if (site.getName() != null) {
                WHERE("name = #{name}");
            }
            if (site.getUrl() != null) {
                WHERE("url = #{url}");
            }
            if (site.getEnabled() != null) {
                WHERE("enabled = #{enabled}");
            }
            if (site.getDescription() != null) {
                WHERE("description = #{description}");
            }
            if (site.getIsCache() != null) {
                WHERE("is_cache = #{isCache}");
            }
            if (site.getSitemap() != null) {
                WHERE("sitemap = #{sitemap}");
            }
            if (site.getSyncSource() != null) {
                WHERE("sync_source = #{syncSource}");
            }
            if (site.getSsl() != null) {
                WHERE("ssl = #{ssl}");
            }
            ORDER_BY("create_time DESC");
        }}.toString();
    }   
} 