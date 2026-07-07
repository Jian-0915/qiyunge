package com.qiyunge.application.service;

import com.qiyunge.domain.entity.GalleryImage;
import java.util.List;

/**
 * 图片源提供者接口。
 */
public interface ImageProvider {
    String getProviderId();
    String getProviderName();
    default boolean isConfigured() { return true; }
    List<GalleryImage> search(String keyword, int page, int pageSize);

    /** 解析下载 URL（默认返回 null，子类可覆盖） */
    default String resolveDownloadUrl(String imageId) { return null; }

    /** 是否支持该来源（默认通过 providerId 匹配） */
    default boolean supports(String source) { return getProviderId().equals(source); }
}
