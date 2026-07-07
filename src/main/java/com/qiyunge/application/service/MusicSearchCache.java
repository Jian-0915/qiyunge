package com.qiyunge.application.service;

import com.qiyunge.domain.entity.Song;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 音乐搜索缓存：减少重复请求，网络失败时可展示历史搜索结果。
 * TTL 默认 30 分钟。
 */
public class MusicSearchCache {

    private static final long DEFAULT_TTL_MS = 30 * 60 * 1000; // 30 分钟

    private static class CacheEntry {
        List<Song> songs;
        long expireAt;
        CacheEntry(List<Song> songs, long ttlMs) {
            this.songs = songs;
            this.expireAt = System.currentTimeMillis() + ttlMs;
        }
        boolean isExpired() { return System.currentTimeMillis() > expireAt; }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMs;

    public MusicSearchCache() { this(DEFAULT_TTL_MS); }

    public MusicSearchCache(long ttlMs) { this.ttlMs = ttlMs; }

    /** 生成缓存 key */
    private String key(String keyword, String providerId) {
        return (keyword + ":" + (providerId != null ? providerId : "all")).toLowerCase();
    }

    /** 获取缓存结果（过期返回 null） */
    public List<Song> get(String keyword, String providerId) {
        String cacheKey = key(keyword, providerId);
        CacheEntry entry = cache.get(cacheKey);
        if (entry == null || entry.isExpired()) {
            cache.remove(cacheKey);
            return null;
        }
        return entry.songs;
    }

    /** 存入缓存 */
    public void put(String keyword, String providerId, List<Song> songs) {
        cache.put(key(keyword, providerId), new CacheEntry(songs, ttlMs));
    }

    /** 清除所有缓存 */
    public void clear() { cache.clear(); }

    /** 清除过期缓存 */
    public void cleanup() {
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
