package com.qiyunge.application.service;

import com.qiyunge.domain.entity.Playlist;
import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.repository.PlaylistRepository;

import java.util.List;

/**
 * 歌单业务服务：封装歌单的创建、管理及歌曲操作。
 */
public class PlaylistService {

    private final PlaylistRepository playlistRepository;

    public PlaylistService(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    /** 创建歌单 */
    public Playlist createPlaylist(int userId, String name, String description) {
        Playlist p = new Playlist();
        p.setUserId(userId);
        p.setName(name);
        p.setDescription(description);
        return playlistRepository.create(p);
    }

    /** 重命名歌单 */
    public boolean renamePlaylist(int playlistId, String newName) {
        return playlistRepository.rename(playlistId, newName);
    }

    /** 删除歌单 */
    public boolean deletePlaylist(int playlistId) {
        return playlistRepository.delete(playlistId);
    }

    /** 获取用户的所有歌单 */
    public List<Playlist> getUserPlaylists(int userId) {
        return playlistRepository.findByUser(userId);
    }

    /** 向歌单添加歌曲 */
    public void addSongToPlaylist(int playlistId, int songId) {
        playlistRepository.addSong(playlistId, songId);
    }

    /** 从歌单移除歌曲 */
    public void removeSongFromPlaylist(int playlistId, int songId) {
        playlistRepository.removeSong(playlistId, songId);
    }

    /** 获取歌单中的所有歌曲 */
    public List<Song> getPlaylistSongs(int playlistId) {
        return playlistRepository.getPlaylistSongs(playlistId);
    }

    /** 检查歌单是否已包含某首歌曲 */
    public boolean containsSong(int playlistId, int songId) {
        return playlistRepository.containsSong(playlistId, songId);
    }
}
