package com.qiyunge.application.service;

import com.qiyunge.domain.entity.PlayableSource;
import com.qiyunge.domain.entity.Song;

import java.util.List;
import java.util.Optional;

/**
 * 音乐来源提供者接口。
 * 本地音乐、在线音乐平台均需实现此接口。
 */
public interface MusicProvider {

    /** 提供者唯一标识，如 "local", "jamendo" */
    String getProviderId();

    /** 提供者显示名称 */
    String getProviderName();

    /** 搜索歌曲 */
    List<Song> search(String keyword);

    /** 解析歌曲的可播放源 */
    Optional<PlayableSource> resolvePlayableSource(Song song);

    /** 是否支持该歌曲（根据 song.source 判断），默认通过 providerId 匹配 */
    default boolean supports(Song song) {
        return getProviderId().equals(song.getSource());
    }

    /** 获取歌词（可选实现，默认返回空） */
    default Optional<String> getLyrics(Song song) {
        return Optional.empty();
    }

    /** 解析可播放源（默认实现，子类可覆盖） */
    default Optional<PlayableSource> defaultResolvePlayableSource(Song song, String format) {
        if (!supports(song)) return Optional.empty();
        String url = song.getUrl();
        if (url == null || url.isEmpty()) return Optional.empty();
        return Optional.of(new PlayableSource(url, format != null ? format : "mp3", getProviderId()));
    }
}
