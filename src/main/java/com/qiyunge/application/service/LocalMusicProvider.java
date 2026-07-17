package com.qiyunge.application.service;

import com.qiyunge.domain.entity.PlayableSource;
import com.qiyunge.domain.entity.Song;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 本地音乐提供者。
 */
public class LocalMusicProvider implements MusicProvider {

    public LocalMusicProvider(MusicService musicService) {
    }

    @Override
    public String getProviderId() { return "local"; }

    @Override
    public String getProviderName() { return "本地音乐"; }

    @Override
    public List<Song> search(String keyword) {
        // 本地搜索通过 MusicService 实现
        return Collections.emptyList();
    }

    @Override
    public Optional<PlayableSource> resolvePlayableSource(Song song) {
        return defaultResolvePlayableSource(song, song.getFormat());
    }

    @Override
    public boolean supports(Song song) {
        String source = song.getSource();
        return source == null || source.isEmpty() || "local".equals(source);
    }

    @Override
    public Optional<String> getLyrics(Song song) {
        // 本地歌曲的歌词由 LyricService 的首层（本地文件查找）处理
        return Optional.empty();
    }
}
