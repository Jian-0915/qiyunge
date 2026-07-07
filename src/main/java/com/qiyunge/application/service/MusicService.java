package com.qiyunge.application.service;

import com.qiyunge.domain.entity.PlayHistory;
import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.repository.FavoriteSongRepository;
import com.qiyunge.infrastructure.repository.PlayHistoryRepository;
import com.qiyunge.infrastructure.repository.SongRepository;
import com.qiyunge.infrastructure.util.AudioFormatDetector;
import com.qiyunge.infrastructure.util.FileDownloader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 音乐业务服务：歌曲查询、本地导入、收藏、播放历史。
 */
public class MusicService {

    private final SongRepository songRepository;
    private final FavoriteSongRepository favoriteSongRepository;
    private final PlayHistoryRepository playHistoryRepository;
    private final MusicProviderRegistry providerRegistry;
    private final Path audioDir;
    private final Path coverDir;
    private final Path lyricDir;

    /**
     * 构造函数（兼容旧调用，不支持在线下载）。
     */
    public MusicService(SongRepository songRepository,
                        FavoriteSongRepository favoriteSongRepository,
                        PlayHistoryRepository playHistoryRepository) {
        this.songRepository = songRepository;
        this.favoriteSongRepository = favoriteSongRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.providerRegistry = null;
        this.audioDir = null;
        this.coverDir = null;
        this.lyricDir = null;
    }

    /**
     * 构造函数（支持在线歌曲下载）。
     */
    public MusicService(SongRepository songRepository,
                        FavoriteSongRepository favoriteSongRepository,
                        PlayHistoryRepository playHistoryRepository,
                        MusicProviderRegistry providerRegistry,
                        Path audioDir, Path coverDir, Path lyricDir) {
        this.songRepository = songRepository;
        this.favoriteSongRepository = favoriteSongRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.providerRegistry = providerRegistry;
        this.audioDir = audioDir;
        this.coverDir = coverDir;
        this.lyricDir = lyricDir;
    }

    public List<Song> findAllSongs() {
        return songRepository.findAll();
    }

    public List<Song> searchSongs(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return findAllSongs();
        return songRepository.findByKeyword(keyword.trim());
    }

    public Song findSongById(int id) {
        return songRepository.findById(id);
    }

    /**
     * 导入本地音乐文件，自动检测真实格式。
     */
    public Song importLocalFile(File file) {
        String fileName = file.getName();
        // Remove extension
        String title = fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

        // Use Path.toUri() for reliable URI encoding on Windows
        String fileUrl = file.toPath().toUri().toString();

        // Detect real audio format from file header
        AudioFormatDetector.AudioFormat detected = AudioFormatDetector.detect(file);
        if (detected == null) {
            detected = AudioFormatDetector.fromExtension(fileName);
        }

        // Try to parse "artist - title" pattern
        Song song = new Song();
        if (title.contains(" - ")) {
            String[] parts = title.split(" - ", 2);
            song.setArtist(parts[0].trim());
            song.setTitle(parts[1].trim());
        } else {
            song.setTitle(title);
        }
        song.setUrl(fileUrl);
        song.setSource("local");
        song.setDuration(0);
        if (detected != null) {
            song.setFormat(detected.format());
            song.setCodec(detected.codec());
        }

        return songRepository.create(song);
    }

    /**
     * 更新歌曲的格式信息和时长（播放成功后回调）。
     */
    public void updateSongFormatAndDuration(int songId, String format, String codec, double duration) {
        songRepository.updateFormatAndDuration(songId, format, codec, duration);
    }

    public boolean deleteSong(int songId) {
        return songRepository.delete(songId);
    }

    public int countSongs() {
        return songRepository.count();
    }

    // ===== 收藏 =====

    public void addFavorite(int userId, int songId) {
        favoriteSongRepository.add(userId, songId);
    }

    public void removeFavorite(int userId, int songId) {
        favoriteSongRepository.remove(userId, songId);
    }

    public boolean isFavorited(int userId, int songId) {
        return favoriteSongRepository.isFavorited(userId, songId);
    }

    public Set<Integer> getFavoriteSongIds(int userId) {
        return favoriteSongRepository.getFavoriteSongIds(userId);
    }

    // ===== 播放历史 =====

    private volatile int lastRecordedSongId = -1;
    private volatile long lastRecordTime = 0;
    private static final long PLAY_RECORD_DEBOUNCE_MS = 5000; // 5 秒内同一首歌只记录一次

    public void recordPlay(int userId, int songId) {
        // 去重：同一首歌在 5 秒内只记录一次播放历史
        long now = System.currentTimeMillis();
        if (songId == lastRecordedSongId && (now - lastRecordTime) < PLAY_RECORD_DEBOUNCE_MS) {
            return;
        }
        lastRecordedSongId = songId;
        lastRecordTime = now;
        playHistoryRepository.record(userId, songId);
    }

    public int countPlayHistory(int userId) {
        return playHistoryRepository.countByUser(userId);
    }

    public List<PlayHistory> getPlayHistory(int userId, int limit) {
        return playHistoryRepository.findByUser(userId, limit);
    }

    /** 清除用户的所有播放历史 */
    public void clearPlayHistory(int userId) {
        playHistoryRepository.deleteByUser(userId);
    }

    // ===== 标记收藏状态 =====

    public void markFavoriteStatus(int userId, List<Song> songs) {
        Set<Integer> favIds = getFavoriteSongIds(userId);
        for (Song song : songs) {
            song.setFavorited(favIds.contains(song.getId()));
        }
    }

    // ===== 在线歌曲收藏与下载 =====

    /**
     * 查找或创建在线歌曲记录（去重入库）。
     * @return 本地数据库中的 Song（含 id）
     */
    public Song findOrCreateOnlineSong(Song onlineSong) {
        if (onlineSong.getSource() == null || onlineSong.getSourceId() == null) {
            return songRepository.create(onlineSong);
        }
        Song existing = songRepository.findBySourceAndSourceId(onlineSong.getSource(), onlineSong.getSourceId());
        if (existing != null) {
            return existing;
        }
        return songRepository.create(onlineSong);
    }

    /**
     * 收藏在线歌曲：先入库（去重），再添加收藏关系。
     * @return 入库后的 Song（含本地 id）
     */
    public Song favoriteOnlineSong(int userId, Song onlineSong) {
        Song localSong = findOrCreateOnlineSong(onlineSong);
        favoriteSongRepository.add(userId, localSong.getId());
        localSong.setFavorited(true);
        return localSong;
    }

    /**
     * 下载在线歌曲到本地曲库并收藏。
     * 下载音频文件，可选下载封面和歌词。
     * @return 下载后的 Song（url 已更新为本地路径）
     */
    public Song downloadAndFavorite(int userId, Song onlineSong) throws Exception {
        if (providerRegistry == null) {
            throw new IllegalStateException("MusicProviderRegistry 未配置，不支持下载");
        }

        // 1. 查找或入库
        Song localSong = findOrCreateOnlineSong(onlineSong);
        int songId = localSong.getId();

        // 2. 获取播放地址
        var playableOpt = providerRegistry.resolvePlayableSource(localSong);
        if (playableOpt.isEmpty()) {
            throw new RuntimeException("无法获取播放地址");
        }
        String audioUrl = playableOpt.get().getUrl();

        // 3. 生成文件名
        String source = localSong.getSource() != null ? localSong.getSource() : "online";
        String sourceId = localSong.getSourceId() != null ? localSong.getSourceId() : String.valueOf(songId);
        String format = localSong.getFormat() != null ? localSong.getFormat() : "mp3";
        String baseName = source + "_" + sourceId;

        // 4. 检查文件是否已存在（文件层去重）
        Path audioPath = audioDir.resolve(baseName + "." + format);
        if (!Files.exists(audioPath)) {
            FileDownloader.download(audioUrl, audioPath);
        }

        // 5. 更新播放地址为本地路径
        songRepository.updateUrl(songId, audioPath.toUri().toString());

        // 6. 下载封面（可选，失败不中断）
        if (localSong.getCoverUrl() != null && !localSong.getCoverUrl().isBlank()) {
            try {
                Path coverPath = coverDir.resolve(baseName + ".jpg");
                if (!Files.exists(coverPath)) {
                    FileDownloader.download(localSong.getCoverUrl(), coverPath);
                }
                songRepository.updateCoverAndLyric(songId, coverPath.toUri().toString(), localSong.getLyricUrl());
            } catch (Exception e) {
                System.err.println("[MusicService] 封面下载失败（不影响主流程）: " + e.getMessage());
            }
        }

        // 7. 获取歌词（可选，失败不中断）
        try {
            Path lyricPath = lyricDir.resolve(baseName + ".lrc");
            if (!Files.exists(lyricPath)) {
                var provider = providerRegistry.getProvider(source);
                if (provider.isPresent()) {
                    var lyricOpt = provider.get().getLyrics(localSong);
                    if (lyricOpt.isPresent() && !lyricOpt.get().isBlank()) {
                        Files.writeString(lyricPath, lyricOpt.get());
                        songRepository.updateCoverAndLyric(songId, localSong.getCoverUrl(), lyricPath.toUri().toString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[MusicService] 歌词获取失败（不影响主流程）: " + e.getMessage());
        }

        // 8. 更新本地对象
        localSong.setUrl(audioPath.toUri().toString());

        // 9. 添加收藏
        favoriteSongRepository.add(userId, songId);
        localSong.setFavorited(true);

        return localSong;
    }

    /**
     * 检查在线歌曲是否已被当前用户收藏。
     */
    public boolean isOnlineSongFavorited(int userId, Song onlineSong) {
        if (onlineSong.getSource() == null || onlineSong.getSourceId() == null) {
            return false;
        }
        Song existing = songRepository.findBySourceAndSourceId(onlineSong.getSource(), onlineSong.getSourceId());
        if (existing == null) {
            return false;
        }
        return favoriteSongRepository.isFavorited(userId, existing.getId());
    }
}
