package com.qiyunge.application.service;

import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.util.LyricParser;
import com.qiyunge.infrastructure.util.LyricParser.LyricLine;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 歌词服务：协调本地查找、在线获取、缓存管理。
 * 获取优先级：本地 .lrc 文件 > 本地缓存 > 在线 API 获取 > 空
 */
public class LyricService {

    private final MusicProviderRegistry providerRegistry;
    private final AsyncExecutor asyncExecutor;
    private final Path cacheDir;

    public LyricService(MusicProviderRegistry providerRegistry, AsyncExecutor asyncExecutor, Path cacheDir) {
        this.providerRegistry = providerRegistry;
        this.asyncExecutor = asyncExecutor;
        this.cacheDir = cacheDir;
        try { Files.createDirectories(cacheDir); } catch (IOException ignored) {}
    }

    /**
     * 异步获取歌曲歌词。
     * @param song     当前播放歌曲
     * @param callback 歌词加载完成后的回调（在 JavaFX 线程中执行）
     */
    public void getLyricsAsync(Song song, Consumer<List<LyricLine>> callback) {
        if (song == null) {
            callback.accept(Collections.emptyList());
            return;
        }
        asyncExecutor.execute(() -> {
            Optional<String> lrc = getLyrics(song);
            List<LyricLine> lines = lrc.map(LyricParser::parseContent).orElse(new ArrayList<>());
            javafx.application.Platform.runLater(() -> callback.accept(lines));
        });
    }

    /**
     * 同步获取歌词文本。
     * 优先级：本地 .lrc > 缓存 > 在线 API > 空
     */
    public Optional<String> getLyrics(Song song) {
        if (song == null) return Optional.empty();

        String source = song.getSource();
        String sourceId = song.getSourceId();

        // 1. 本地同名 .lrc 文件（仅本地歌曲）
        if ("local".equals(source) || source == null || source.isEmpty()) {
            String url = song.getUrl();
            if (url != null) {
                Path audioPath = toLocalPath(url);
                if (audioPath != null) {
                    Path lrcPath = LyricParser.findLyricFile(audioPath);
                    if (lrcPath != null) {
                        try {
                            String content = Files.readString(lrcPath, StandardCharsets.UTF_8);
                            System.out.println("[LyricService] 本地 .lrc 文件命中: " + lrcPath);
                            return Optional.of(content);
                        } catch (IOException ignored) {}
                    }
                }
            }
        }

        // 2. 本地缓存文件
        if (source != null && sourceId != null && !sourceId.isEmpty()) {
            Path cacheFile = getCacheFile(source, sourceId);
            if (Files.exists(cacheFile) && cacheFile.toFile().length() > 0) {
                try {
                    String content = Files.readString(cacheFile, StandardCharsets.UTF_8);
                    System.out.println("[LyricService] 缓存命中: " + cacheFile);
                    return Optional.of(content);
                } catch (IOException ignored) {}
            }
        }

        // 3. 在线获取
        if (source != null && !source.isEmpty()) {
            MusicProvider provider = providerRegistry.getProvider(source).orElse(null);
            if (provider != null) {
                try {
                    Optional<String> onlineLyrics = provider.getLyrics(song);
                    if (onlineLyrics.isPresent() && !onlineLyrics.get().isEmpty()) {
                        String lrc = onlineLyrics.get();
                        System.out.println("[LyricService] 在线获取成功: " + source + " " + sourceId);
                        // 写入缓存
                        if (sourceId != null && !sourceId.isEmpty()) {
                            Path cacheFile = getCacheFile(source, sourceId);
                            try {
                                Files.writeString(cacheFile, lrc, StandardCharsets.UTF_8);
                                System.out.println("[LyricService] 歌词已缓存: " + cacheFile);
                            } catch (IOException e) {
                                System.err.println("[LyricService] 缓存写入失败: " + e.getMessage());
                            }
                        }
                        return Optional.of(lrc);
                    }
                } catch (Exception e) {
                    System.err.println("[LyricService] 在线获取失败: " + e.getMessage());
                }
            }
        }

        System.out.println("[LyricService] 未找到歌词: " + song.getDisplayTitle());
        return Optional.empty();
    }

    /** 获取缓存文件路径 */
    private Path getCacheFile(String source, String sourceId) {
        return cacheDir.resolve(source + "_" + sourceId + ".lrc");
    }

    /** 清除指定歌曲缓存 */
    public void clearCache(String source, String sourceId) {
        try {
            Files.deleteIfExists(getCacheFile(source, sourceId));
        } catch (IOException ignored) {}
    }

    /** 将歌曲 URL 解析为本地文件路径 */
    private Path toLocalPath(String url) {
        if (url == null) return null;
        try {
            if (url.startsWith("file://")) {
                String path = url.substring("file://".length());
                while (path.startsWith("/")) path = path.substring(1);
                return Path.of(URLDecoder.decode(path, StandardCharsets.UTF_8));
            }
            if (url.startsWith("file:")) {
                String path = url.substring("file:".length());
                while (path.startsWith("/")) path = path.substring(1);
                return Path.of(URLDecoder.decode(path, StandardCharsets.UTF_8));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}