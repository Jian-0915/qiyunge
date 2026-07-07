package com.qiyunge.application.service;

import com.qiyunge.domain.entity.PlayableSource;
import com.qiyunge.domain.entity.Song;

import java.util.List;
import java.util.Optional;

/**
 * 网易云音乐提供者：通过本地 NeteaseCloudMusicApi 获取真实音乐数据。
 */
public class NeteaseMusicProvider implements MusicProvider {

    private final NeteaseApiClient client;

    public NeteaseMusicProvider(NeteaseApiClient client) {
        this.client = client;
    }

    @Override
    public String getProviderId() { return "netease"; }

    @Override
    public String getProviderName() { return "网易云"; }

    @Override
    public List<Song> search(String keyword) {
        return client.search(keyword, 20);
    }

    @Override
    public Optional<PlayableSource> resolvePlayableSource(Song song) {
        if (!supports(song)) return Optional.empty();
        try {
            long songId = Long.parseLong(song.getSourceId());
            String url = client.getSongUrl(songId);
            if (url == null || url.isEmpty()) return Optional.empty();
            return Optional.of(new PlayableSource(url, "mp3", getProviderId()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getLyrics(Song song) {
        if (!supports(song)) return Optional.empty();
        try {
            long songId = Long.parseLong(song.getSourceId());
            String lyric = client.getLyric(songId);
            return Optional.ofNullable(lyric);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
