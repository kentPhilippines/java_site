package com.site.service;

import com.site.entity.SiteTdk;
import com.site.mapper.SiteKeywordsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteKeywordsService {

    private final SiteKeywordsMapper keywordsMapper;

    public List<SiteTdk> getKeywordsBySiteId(Long siteId) {
        return keywordsMapper.findBySiteId(siteId);
    }

    @Transactional
    public void saveKeyword(SiteTdk keyword) {
        if (keyword.getId() == null) {
            keywordsMapper.insert(keyword);
        } else {
            keywordsMapper.update(keyword);
        }
    }

    @Transactional
    public void deleteKeyword(Long id) {
        keywordsMapper.deleteById(id);
    }

    @Transactional
    public void saveKeywords(Long siteId, List<SiteTdk> keywords) {
        // 先删除原有的关键字
        keywordsMapper.deleteBySiteId(siteId);
        // 保存新的关键字
        for (SiteTdk keyword : keywords) {
            keyword.setSiteId(siteId);
            keywordsMapper.insert(keyword);
        }
    }

    public String replaceKeywords(String content, Long siteId) {
        List<SiteTdk> keywords = keywordsMapper.findBySiteId(siteId);
        String result = content;
        for (SiteTdk keyword : keywords) {
            if (keyword.getEnabled() == 1) {
                result = result.replace(keyword.getSourceWord(), keyword.getTargetWord());
            }
        }
        return result;
    }
} 