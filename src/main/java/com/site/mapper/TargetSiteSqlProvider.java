package com.site.mapper;

import com.site.entity.TargetSite;
import org.apache.ibatis.jdbc.SQL;

public class TargetSiteSqlProvider {
    
    public String insert(TargetSite site) {
        return new SQL() {{
            INSERT_INTO("target_site");
            
            if (site.getDomain() != null) {
                VALUES("domain", "#{domain}");
            }
            if (site.getBaseUrl() != null) {
                VALUES("base_url", "#{baseUrl}");
            }
            if (site.getEnabled() != null) {
                VALUES("enabled", "#{enabled}");
            }
            if (site.getDescription() != null) {
                VALUES("description", "#{description}");
            }
            if (site.getTdk() != null) {
                VALUES("tdk", "#{tdk}");
            }
            
            VALUES("create_time", "datetime('now')");
            VALUES("update_time", "datetime('now')");
        }}.toString();
    }
    
    public String update(TargetSite site) {
        return new SQL() {{
            UPDATE("target_site");
            
            if (site.getDomain() != null) {
                SET("domain = #{domain}");
            }
            if (site.getBaseUrl() != null) {
                SET("base_url = #{baseUrl}");
            }
            if (site.getEnabled() != null) {
                SET("enabled = #{enabled}");
            }
            if (site.getDescription() != null) {
                SET("description = #{description}");
            }
            if (site.getTdk() != null) {
                SET("tdk = #{tdk}");
            }
            
            SET("update_time = datetime('now')");
            WHERE("id = #{id}");
        }}.toString();
    }
} 