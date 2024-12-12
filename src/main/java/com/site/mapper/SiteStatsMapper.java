package com.site.mapper;

import com.site.entity.SiteStats;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SiteStatsMapper {
    
    @Select("SELECT * FROM site_stats WHERE date = #{date} AND site_id = #{siteId}")
    SiteStats findByDateAndSite(@Param("date") String date, @Param("siteId") Long siteId);
    
    @Select("SELECT * FROM site_stats WHERE site_id = #{siteId} " +
            "AND date BETWEEN #{startDate} AND #{endDate} ORDER BY date DESC")
    List<SiteStats> findByDateRange(@Param("siteId") Long siteId, 
            @Param("startDate") String startDate, @Param("endDate") String endDate);
    
    @Insert("INSERT INTO site_stats (site_id, domain, visits, unique_visits, bandwidth, date, created_at, updated_at) " +
            "VALUES (#{siteId}, #{domain}, #{visits}, #{uniqueVisits}, #{bandwidth}, #{date}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SiteStats stats);
    
    @Update("UPDATE site_stats SET visits = #{visits}, unique_visits = #{uniqueVisits}, " +
            "bandwidth = #{bandwidth}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(SiteStats stats);
    
    @Delete("DELETE FROM site_stats WHERE site_id = #{siteId}")
    void deleteBySiteId(Long siteId);

    @Delete("DELETE FROM site_stats WHERE id = #{id}")
    void deleteById(Long id);
} 
