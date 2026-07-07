package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.Playlist;
import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * 歌单数据访问层：负责歌单的增删改查以及歌单内歌曲管理。
 */
public class PlaylistRepository {

    private final DatabaseManager dbManager;

    public PlaylistRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /** 创建歌单 */
    public Playlist create(Playlist playlist) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT INTO playlists (user_id, name, description, cover_url) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, playlist.getUserId());
                stmt.setString(2, playlist.getName());
                stmt.setString(3, playlist.getDescription());
                stmt.setString(4, playlist.getCoverUrl());
                stmt.executeUpdate();
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) playlist.setId(keys.getInt(1));
                return playlist;
            }
        });
    }

    /** 重命名歌单 */
    public boolean rename(int playlistId, String newName) {
        return dbManager.withConnection(conn -> {
            String sql = "UPDATE playlists SET name = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newName);
                stmt.setInt(2, playlistId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 删除歌单 */
    public boolean delete(int playlistId) {
        return dbManager.withConnection(conn -> {
            String sql = "DELETE FROM playlists WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, playlistId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 查询用户的所有歌单（含歌曲数量） */
    public List<Playlist> findByUser(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT p.*, COUNT(ps.song_id) as song_count " +
                         "FROM playlists p LEFT JOIN playlist_songs ps ON p.id = ps.playlist_id " +
                         "WHERE p.user_id = ? GROUP BY p.id ORDER BY p.updated_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return mapPlaylists(rs);
            }
        });
    }

    /** 根据 ID 查询歌单 */
    public Playlist findById(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM playlists WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return mapPlaylist(rs);
                return null;
            }
        });
    }

    /** 向歌单添加歌曲（已存在则忽略） */
    public void addSong(int playlistId, int songId) {
        dbManager.withConnection(conn -> {
            String sql = "INSERT OR IGNORE INTO playlist_songs (playlist_id, song_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, playlistId);
                stmt.setInt(2, songId);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    /** 从歌单移除歌曲 */
    public void removeSong(int playlistId, int songId) {
        dbManager.withConnection(conn -> {
            String sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, playlistId);
                stmt.setInt(2, songId);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    /** 获取歌单中的所有歌曲 */
    public List<Song> getPlaylistSongs(int playlistId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT s.* FROM songs s JOIN playlist_songs ps ON s.id = ps.song_id " +
                         "WHERE ps.playlist_id = ? ORDER BY ps.sort_order, ps.added_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, playlistId);
                ResultSet rs = stmt.executeQuery();
                return mapSongs(rs);
            }
        });
    }

    /** 检查歌单是否已包含某首歌曲 */
    public boolean containsSong(int playlistId, int songId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, playlistId);
                stmt.setInt(2, songId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        });
    }

    /** 统计用户歌单数量 */
    public int countByUser(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM playlists WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    /** 映射多条歌单记录 */
    private List<Playlist> mapPlaylists(ResultSet rs) {
        List<Playlist> list = new ArrayList<>();
        try {
            while (rs.next()) list.add(mapPlaylist(rs));
        } catch (Exception e) { throw new RuntimeException("Failed to map playlists", e); }
        return list;
    }

    /** 映射单条歌单记录 */
    private Playlist mapPlaylist(ResultSet rs) {
        try {
            Playlist p = new Playlist();
            p.setId(rs.getInt("id"));
            p.setUserId(rs.getInt("user_id"));
            p.setName(rs.getString("name"));
            p.setDescription(rs.getString("description"));
            p.setCoverUrl(rs.getString("cover_url"));
            try { p.setSongCount(rs.getInt("song_count")); } catch (Exception e) { p.setSongCount(0); }
            return p;
        } catch (Exception e) { throw new RuntimeException("Failed to map playlist", e); }
    }

    /** 映射歌曲列表 */
    private List<Song> mapSongs(ResultSet rs) {
        List<Song> songs = new ArrayList<>();
        try {
            while (rs.next()) {
                Song s = new Song();
                s.setId(rs.getInt("id"));
                s.setTitle(rs.getString("title"));
                s.setArtist(rs.getString("artist"));
                s.setDuration(rs.getDouble("duration"));
                s.setUrl(rs.getString("url"));
                s.setSource(rs.getString("source"));
                try { s.setFormat(rs.getString("format")); } catch (Exception ignored) {}
                try { s.setCodec(rs.getString("codec")); } catch (Exception ignored) {}
                songs.add(s);
            }
        } catch (Exception e) { throw new RuntimeException("Failed to map songs", e); }
        return songs;
    }
}
