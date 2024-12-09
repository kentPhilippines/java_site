package com.site.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import com.site.entity.Site;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.annotations.SelectProvider;

@Mapper
public interface SiteMapper {

    /**
     * 查询所有站点 根据创建时间排序
     * 
     * @param site
     * @return
     */
    @SelectProvider(type = SiteMapperSqlProvider.class, method = "selectList")
    List<Site> selectList(Site site);

    @Select("SELECT * FROM site WHERE id = #{id}")
    Site selectById(Long id);

    /**
     * 这里需要动态新增
     */
    @InsertProvider(type = SiteMapperSqlProvider.class, method = "insert")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Site site);
    /**
     * 更新站点
     */
    @UpdateProvider(type = SiteMapperSqlProvider.class, method = "update")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int update(Site site);

    @Delete("DELETE FROM site WHERE id = #{id}")
    void delete(Long id);

    @Select("SELECT * FROM site WHERE enabled = 1")
    List<Site> selectEnabledSites();

}
