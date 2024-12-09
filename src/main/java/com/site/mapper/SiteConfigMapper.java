package com.site.mapper;

import com.site.entity.SiteConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SiteConfigMapper {
    
    @Select("SELECT * FROM site_config WHERE config_key = #{key}")
    SiteConfig findByKey(@Param("key") String key);
    
    @Select("SELECT * FROM site_config WHERE enabled = 1")
    List<SiteConfig> findAllEnabled();
    
    @Insert("INSERT INTO site_config(config_key, config_value, enabled, create_time, update_time) " +
            "VALUES(#{configKey}, #{configValue}, #{enabled}, datetime('now'), datetime('now'))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SiteConfig config);
    
    @Update("UPDATE site_config SET config_value = #{configValue}, " +
            "update_time = datetime('now') WHERE config_key = #{configKey}")
    int update(SiteConfig config);
    
    @Delete("DELETE FROM site_config WHERE config_key = #{key}")
    int deleteByKey(@Param("key") String key);
    
    @Select("SELECT COUNT(1) FROM site_config WHERE config_key = #{key}")
    int existsByKey(@Param("key") String key);
} 