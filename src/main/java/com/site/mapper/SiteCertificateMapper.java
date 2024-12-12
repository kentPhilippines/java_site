package com.site.mapper;

import com.site.entity.SiteCertificate;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface SiteCertificateMapper {
    
    @Select("SELECT * FROM site_certificates WHERE site_id = #{siteId} ORDER BY id DESC")
    List<SiteCertificate> findBySiteId(Long siteId);
    
    @Select("SELECT * FROM site_certificates WHERE domain = #{domain} and status = #{status} ORDER BY id DESC LIMIT 1")
    SiteCertificate findByDomain(String domain, String status);
    
    @Insert("INSERT INTO site_certificates (site_id, domain, cert_type, cert_file, key_file, chain_file, " +
            "status, auto_renew, created_at, expires_at) " +
            "VALUES (#{siteId}, #{domain}, #{certType}, #{certFile}, #{keyFile}, #{chainFile}, " +
            "#{status}, #{autoRenew}, #{createdAt}, #{expiresAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(SiteCertificate certificate);
    
    @Update("UPDATE site_certificates SET cert_file = #{certFile}, key_file = #{keyFile}, " +
            "chain_file = #{chainFile}, status = #{status}, auto_renew = #{autoRenew}, " +
            "expires_at = #{expiresAt} WHERE id = #{id}")
    void update(SiteCertificate certificate);
    
    @Delete("DELETE FROM site_certificates WHERE id = #{id}")
    void deleteById(Long id);
    
    @Select("SELECT * FROM site_certificates WHERE status = #{status}")
    List<SiteCertificate> findByStatus(String status);
    
    @Select("SELECT * FROM site_certificates WHERE id = #{id}")
    SiteCertificate findById(Long id);
    
    @Delete("DELETE FROM site_certificates WHERE site_id = #{siteId}")
    void deleteBySiteId(Long siteId);
} 