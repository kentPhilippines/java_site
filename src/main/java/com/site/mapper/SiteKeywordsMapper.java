package com.site.mapper;

import com.site.entity.SiteTdk;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SiteKeywordsMapper {
    
    @Select("SELECT * FROM site_keywords WHERE site_id = #{siteId}")
    List<SiteTdk> findBySiteId(Long siteId);
    
    @Insert("INSERT INTO site_keywords (site_id, source_word, target_word, page_url, enabled) " +
            "VALUES (#{siteId}, #{sourceWord}, #{targetWord}, #{pageUrl}, #{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SiteTdk keyword);
    
    @Update("UPDATE site_keywords SET source_word = #{sourceWord}, target_word = #{targetWord}, " +
            "page_url = #{pageUrl}, enabled = #{enabled}, update_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    void update(SiteTdk keyword);
    
    @Delete("DELETE FROM site_keywords WHERE id = #{id}")
    void deleteById(Long id);
    
    @Delete("DELETE FROM site_keywords WHERE site_id = #{siteId}")
    void deleteBySiteId(Long siteId);
} 