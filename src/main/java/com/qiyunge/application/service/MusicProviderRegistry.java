package com.qiyunge.application.service;

import com.qiyunge.domain.entity.PlayableSource;
import com.qiyunge.domain.entity.Song;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * 音乐提供者注册中心：管理多个 MusicProvider，支持多平台并发搜索、冷却容错。
 */
public class MusicProviderRegistry {

    private final Map<String, MusicProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, ProviderState> states = new ConcurrentHashMap<>();
    private String defaultProviderId = "local";
    private final ExecutorService searchExecutor = Executors.newFixedThreadPool(4);

    /** 提供者运行状态 */
    public static class ProviderState {
        private boolean enabled = true;
        private int priority = 100;
        private volatile Instant cooldownUntil;
        private final java.util.concurrent.atomic.AtomicInteger failureCount = new java.util.concurrent.atomic.AtomicInteger(0);
        private static final long COOLDOWN_BASE_MS = 30_000; // 基础冷却 30 秒
        private static final long COOLDOWN_MAX_MS = 3_600_000; // 最大冷却 1 小时

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public boolean isInCooldown() { return cooldownUntil != null && Instant.now().isBefore(cooldownUntil); }
        public Instant getCooldownUntil() { return cooldownUntil; }

        /** 记录失败，增加冷却时间 */
        public void recordFailure() {
            int count = failureCount.incrementAndGet();
            long cooldownMs = Math.min(COOLDOWN_BASE_MS * (1L << Math.min(count - 1, 6)), COOLDOWN_MAX_MS);
            cooldownUntil = Instant.now().plusMillis(cooldownMs);
        }

        /** 记录成功，重置失败计数 */
        public void recordSuccess() {
            failureCount.set(0);
            cooldownUntil = null;
        }
    }

    /** 注册提供者（默认启用，优先级 100） */
    public void register(MusicProvider provider) {
        providers.put(provider.getProviderId(), provider);
        states.putIfAbsent(provider.getProviderId(), new ProviderState());
    }

    /** 注册提供者（指定优先级） */
    public void register(MusicProvider provider, int priority) {
        providers.put(provider.getProviderId(), provider);
        ProviderState state = new ProviderState();
        state.priority = priority;
        states.put(provider.getProviderId(), state);
    }

    /** 获取指定提供者 */
    public Optional<MusicProvider> getProvider(String id) {
        return Optional.ofNullable(providers.get(id));
    }

    /** 获取所有已注册提供者（按优先级排序） */
    public List<MusicProvider> getAllProvidersSorted() {
        return providers.values().stream()
            .sorted(Comparator.comparingInt(p -> states.getOrDefault(p.getProviderId(), new ProviderState()).priority))
            .toList();
    }

    /** 获取所有已注册提供者 */
    public Collection<MusicProvider> getAllProviders() {
        return Collections.unmodifiableCollection(providers.values());
    }

    /** 获取提供者状态 */
    public Optional<ProviderState> getState(String providerId) {
        return Optional.ofNullable(states.get(providerId));
    }

    /** 设置默认提供者 */
    public void setDefaultProvider(String id) { this.defaultProviderId = id; }
    public String getDefaultProviderId() { return defaultProviderId; }

    /** 解析歌曲播放源 */
    public Optional<PlayableSource> resolvePlayableSource(Song song) {
        String source = song.getSource();
        if (source != null && !source.isEmpty()) {
            MusicProvider provider = providers.get(source);
            if (provider != null) {
                Optional<PlayableSource> result = provider.resolvePlayableSource(song);
                if (result.isPresent()) return result;
            }
        }
        MusicProvider defaultProvider = providers.get(defaultProviderId);
        if (defaultProvider != null) {
            return defaultProvider.resolvePlayableSource(song);
        }
        return Optional.empty();
    }

    /**
     * 多平台并发搜索：按优先级调用所有在线提供者，合并去重。
     * @param keyword 搜索关键词
     * @param filterProviderId 可选：只搜索指定提供者（null 表示全部）
     * @return 合并去重后的搜索结果
     */
    public List<Song> searchAll(String keyword, String filterProviderId) {
        List<Song> allResults = new CopyOnWriteArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (MusicProvider provider : getAllProvidersSorted()) {
            String pid = provider.getProviderId();
            // 跳过本地提供者
            if ("local".equals(pid)) continue;
            // 如果指定了筛选，跳过其他提供者
            if (filterProviderId != null && !filterProviderId.equals(pid)) continue;

            ProviderState state = states.get(pid);
            if (state == null || !state.isEnabled() || state.isInCooldown()) {
                System.out.println("[Registry] 跳过 " + pid + " (冷却中或已禁用)");
                continue;
            }

            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    long start = System.currentTimeMillis();
                    List<Song> results = provider.search(keyword);
                    long elapsed = System.currentTimeMillis() - start;
                    System.out.println("[Registry] " + pid + " 返回 " + results.size() + " 首 (" + elapsed + "ms)");
                    if (!results.isEmpty()) {
                        state.recordSuccess();
                        allResults.addAll(results);
                    }
                } catch (Exception e) {
                    System.err.println("[Registry] " + pid + " 搜索失败: " + e.getMessage());
                    state.recordFailure();
                }
            }, searchExecutor));
        }

        // 等待所有搜索完成（最多 15 秒总超时）
        if (!futures.isEmpty()) {
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(15, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
        }

        // 按 source + sourceId 去重
        return deduplicate(allResults);
    }

    /** 关闭注册中心内部线程池，避免应用退出后残留后台线程。 */
    public void shutdown() {
        searchExecutor.shutdownNow();
    }

    /** 按 source + sourceId 去重 */
    private List<Song> deduplicate(List<Song> songs) {
        Map<String, Song> seen = new LinkedHashMap<>();
        for (Song song : songs) {
            String key = song.getSource() + ":" + song.getSourceId();
            if (!seen.containsKey(key)) {
                seen.put(key, song);
            }
        }
        return new ArrayList<>(seen.values());
    }
}
