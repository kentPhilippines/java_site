package com.site.mapper;

import com.site.entity.TargetSite;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TargetSiteMapper {
    
    @Select("SELECT * FROM target_site")
    List<TargetSite> findAll();
    
    @Select("SELECT * FROM target_site WHERE enabled = 1")
    List<TargetSite> findAllEnabled();
    
    @Select("SELECT * FROM target_site WHERE domain = #{domain}")
    TargetSite findByDomain(String domain);
    
    @InsertProvider(type = TargetSiteSqlProvider.class, method = "insert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TargetSite site);
    
    @UpdateProvider(type = TargetSiteSqlProvider.class, method = "update")
    int update(TargetSite site);
    
    @Delete("DELETE FROM target_site WHERE id = #{id}")
    int delete(Long id);
} 