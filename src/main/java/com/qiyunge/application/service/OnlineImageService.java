package com.qiyunge.application.service;

import com.qiyunge.domain.entity.GalleryImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 在线图片搜索服务：统一搜索入口，整合多平台搜索 + 去重 + 缓存。
 * 支持多源降级：当某个图源超时返回空结果时，自动尝试下一个图源。
 */
public class OnlineImageService {
    private final List<ImageProvider> providers = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<String, ImageProvider> providerMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(30);
    private static final long PROVIDER_TIMEOUT_SECONDS = 10;
    private static final ExecutorService SEARCH_EXECUTOR = Executors.newCachedThreadPool(r -> {
        var t = new Thread(r, "online-image-search");
        t.setDaemon(true);
        return t;
    });

    public void registerProvider(ImageProvider provider) {
        providers.add(provider);
        providerMap.put(provider.getProviderId(), provider);
    }

    public List<ImageProvider> getProviders() {
        return new ArrayList<>(providers);
    }

    /** 搜索在线图片（全平台搜索 + 去重） */
    public List<GalleryImage> search(String keyword) {
        return searchWithPage(keyword, 1);
    }

    /** 搜索在线图片（使用随机页码，用于刷新） */
    public List<GalleryImage> searchWithRandomPage(String keyword) {
        int randomPage = 1 + (int) (Math.random() * 20);
        return searchWithPage(keyword, randomPage);
    }

    /** 搜索在线图片（带分页） */
    public List<GalleryImage> searchWithPage(String keyword, int page) {
        if (keyword == null || keyword.trim().isEmpty()) return new ArrayList<>();
        String normalizedKeyword = keyword.trim();
        String cacheKey = "all:" + normalizedKeyword.toLowerCase() + ":page:" + page;

        // 检查缓存
        List<GalleryImage> cached = getCached(cacheKey);
        if (cached != null) return cached;

        List<GalleryImage> allResults = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        List<CompletableFuture<ProviderSearchResult>> futures = providers.stream()
            .map(provider -> CompletableFuture
                .supplyAsync(() -> searchProviderWithPage(provider, normalizedKeyword, page), SEARCH_EXECUTOR)
                .completeOnTimeout(
                    new ProviderSearchResult(provider, new ArrayList<>(), "搜索超时"),
                    PROVIDER_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
                ))
            .toList();

        for (CompletableFuture<ProviderSearchResult> future : futures) {
            ProviderSearchResult providerResult = future.join();
            List<GalleryImage> results = providerResult.results();
            if (!results.isEmpty()) {
                for (GalleryImage img : results) {
                    String dedupeKey = (img.getSource() != null ? img.getSource() : "") + "_"
                        + (img.getUrl() != null ? img.getUrl() : img.getThumbnailUrl());
                    if (seen.add(dedupeKey)) {
                        allResults.add(img);
                    }
                }
            } else if (providerResult.error() != null) {
                System.out.println("[OnlineImageService] " + providerResult.provider().getProviderName() + " " + providerResult.error());
            } else {
                System.out.println("[OnlineImageService] " + providerResult.provider().getProviderName() + " 返回空结果，可能超时或无匹配");
            }
        }

        if (!allResults.isEmpty()) {
            putCache(cacheKey, allResults);
        }
        return allResults;
    }

    /** 按平台搜索 */
    public List<GalleryImage> searchByProvider(String keyword, String providerId) {
        return searchByProviderWithPage(keyword, providerId, 1);
    }

    /** 按平台搜索（使用随机页码，用于刷新） */
    public List<GalleryImage> searchByProviderWithRandomPage(String keyword, String providerId) {
        int randomPage = 1 + (int) (Math.random() * 20);
        return searchByProviderWithPage(keyword, providerId, randomPage);
    }

    /** 按平台搜索（带分页） */
    public List<GalleryImage> searchByProviderWithPage(String keyword, String providerId, int page) {
        if (keyword == null || keyword.trim().isEmpty()) return new ArrayList<>();
        if (providerId == null || providerId.isBlank()) return searchWithPage(keyword, page);

        String normalizedKeyword = keyword.trim();
        String cacheKey = providerId + ":" + normalizedKeyword.toLowerCase() + ":page:" + page;
        List<GalleryImage> cached = getCached(cacheKey);
        if (cached != null) return cached;

        ImageProvider provider = providerMap.get(providerId);
        if (provider != null) {
            try {
                List<GalleryImage> results = CompletableFuture
                    .supplyAsync(() -> provider.search(normalizedKeyword, page, 20), SEARCH_EXECUTOR)
                    .completeOnTimeout(new ArrayList<>(), PROVIDER_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
                if (!results.isEmpty()) {
                    putCache(cacheKey, results);
                }
                return results;
            } catch (Exception e) {
                System.err.println("[OnlineImageService] " + provider.getProviderName() + " 搜索失败: " + e.getMessage());
                return new ArrayList<>();
            }
        }
        System.err.println("[OnlineImageService] 未注册图片源: " + providerId);
        return new ArrayList<>();
    }

    public void clearCache() {
        cache.clear();
    }

    private List<GalleryImage> getCached(String cacheKey) {
        CacheEntry cached = cache.get(cacheKey);
        if (cached == null) return null;

        if (System.currentTimeMillis() - cached.createdAt > CACHE_TTL_MS) {
            cache.remove(cacheKey);
            return null;
        }
        return new ArrayList<>(cached.results);
    }

    private void putCache(String cacheKey, List<GalleryImage> results) {
        cache.put(cacheKey, new CacheEntry(new ArrayList<>(results), System.currentTimeMillis()));
    }

    private ProviderSearchResult searchProvider(ImageProvider provider, String keyword) {
        return searchProviderWithPage(provider, keyword, 1);
    }

    private ProviderSearchResult searchProviderWithPage(ImageProvider provider, String keyword, int page) {
        try {
            return new ProviderSearchResult(provider, provider.search(keyword, page, 20), null);
        } catch (Exception e) {
            System.err.println("[OnlineImageService] " + provider.getProviderName() + " 搜索失败: " + e.getMessage());
            return new ProviderSearchResult(provider, new ArrayList<>(), "搜索失败: " + e.getMessage());
        }
    }

    private record CacheEntry(List<GalleryImage> results, long createdAt) {}
    private record ProviderSearchResult(ImageProvider provider, List<GalleryImage> results, String error) {}
}
