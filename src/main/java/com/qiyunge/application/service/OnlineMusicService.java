package com.qiyunge.application.service;

import com.qiyunge.domain.entity.Song;

import java.util.ArrayList;
import java.util.List;

/**
 * 在线音乐服务：统一搜索入口，整合缓存、多平台搜索、去重。
 */
public class OnlineMusicService {

    private final MusicProviderRegistry registry;
    private final MusicSearchCache cache;

    public OnlineMusicService(MusicProviderRegistry registry) {
        this.registry = registry;
        this.cache = new MusicSearchCache();
    }

    /**
     * 统一搜索：先查缓存，再调多平台搜索，最后缓存结果。
     * @param keyword 搜索关键词
     * @param filterProviderId 可选：只搜索指定平台（null 表示全部）
     * @return 合并去重后的搜索结果
     */
    public List<Song> search(String keyword, String filterProviderId) {
        if (keyword == null || keyword.trim().isEmpty()) return new ArrayList<>();

        // 1. 查缓存
        List<Song> cached = cache.get(keyword, filterProviderId);
        if (cached != null) {
            System.out.println("[OnlineMusic] 缓存命中: " + cached.size() + " 首");
            return cached;
        }

        // 2. 多平台搜索
        List<Song> results = registry.searchAll(keyword.trim(), filterProviderId);
        System.out.println("[OnlineMusic] 搜索完成: " + results.size() + " 首");

        // 3. 缓存结果
        if (!results.isEmpty()) {
            cache.put(keyword, filterProviderId, results);
        }

        return results;
    }

    /** 获取搜索缓存 */
    public MusicSearchCache getCache() { return cache; }

    /** 获取提供者注册中心 */
    public MusicProviderRegistry getRegistry() { return registry; }
}
