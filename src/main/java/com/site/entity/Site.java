package com.site.entity;
import lombok.Data;
@Data
public class Site {
    /**
     * 主键
     */
    private Long id;
    /**
     * 名称
     */
    private String name;
    /**
     * 域名
     */
    private String url;
    /**
     * 描述
     */
    private String description;
    /**
     * 创建时间
     */
    private String createTime;
    /**
     * 更新时间
     */
    private String updateTime;
    /**
     * 是否启用
     */
    private Integer enabled;
    /**
     * 开启或关闭缓存
     */
    private Integer isCache;
    /**
     * 网站地图
     */
    private Integer sitemap;
    /**
     * 同步源文件地址
     */
    private String syncSource;



}
