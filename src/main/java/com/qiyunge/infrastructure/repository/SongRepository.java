package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SongRepository {

    private final DatabaseManager dbManager;

    public SongRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Song create(Song song) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT INTO songs (title, artist, album, duration, url, cover_url, source, source_id, format, codec) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, song.getTitle());
                stmt.setString(2, song.getArtist());
                stmt.setString(3, song.getAlbum());
                stmt.setDouble(4, song.getDuration());
                stmt.setString(5, song.getUrl());
                stmt.setString(6, song.getCoverUrl());
                stmt.setString(7, song.getSource());
                stmt.setString(8, song.getSourceId());
                stmt.setString(9, song.getFormat());
                stmt.setString(10, song.getCodec());
                stmt.executeUpdate();
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    song.setId(keys.getInt(1));
                }
                return song;
            }
        });
    }

    public boolean updateFormatAndDuration(int songId, String format, String codec, double duration) {
        return dbManager.withConnection(conn -> {
            String sql = "UPDATE songs SET format = ?, codec = ?, duration = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, format);
                stmt.setString(2, codec);
                stmt.setDouble(3, duration);
                stmt.setInt(4, songId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    public List<Song> findAll() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM songs ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                return mapSongs(rs);
            }
        });
    }

    public List<Song> findByKeyword(String keyword) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM songs WHERE title LIKE ? OR artist LIKE ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String pattern = "%" + keyword + "%";
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                ResultSet rs = stmt.executeQuery();
                return mapSongs(rs);
            }
        });
    }

    public Song findById(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM songs WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return mapSong(rs);
                }
                return null;
            }
        });
    }

    public boolean delete(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "DELETE FROM songs WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /**
     * 按来源和来源 ID 查询歌曲（用于在线歌曲去重）。
     */
    public Song findBySourceAndSourceId(String source, String sourceId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM songs WHERE source = ? AND source_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, source);
                stmt.setString(2, sourceId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return mapSong(rs);
                }
                return null;
            }
        });
    }

    /**
     * 更新歌曲播放地址（下载到本地后改为本地路径）。
     */
    public boolean updateUrl(int songId, String newUrl) {
        return dbManager.withConnection(conn -> {
            String sql = "UPDATE songs SET url = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newUrl);
                stmt.setInt(2, songId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /**
     * 更新封面和歌词路径。
     */
    public boolean updateCoverAndLyric(int songId, String coverPath, String lyricPath) {
        return dbManager.withConnection(conn -> {
            String sql = "UPDATE songs SET cover_url = ?, lyric_url = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, coverPath);
                stmt.setString(2, lyricPath);
                stmt.setInt(3, songId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    public int count() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM songs";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    private List<Song> mapSongs(ResultSet rs) {
        List<Song> songs = new ArrayList<>();
        try {
            while (rs.next()) {
                songs.add(mapSong(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to map songs", e);
        }
        return songs;
    }

    private Song mapSong(ResultSet rs) {
        try {
            Song song = new Song();
            song.setId(rs.getInt("id"));
            song.setTitle(rs.getString("title"));
            song.setArtist(rs.getString("artist"));
            song.setAlbum(rs.getString("album"));
            song.setDuration(rs.getDouble("duration"));
            song.setUrl(rs.getString("url"));
            song.setCoverUrl(rs.getString("cover_url"));
            song.setLyricUrl(rs.getString("lyric_url"));
            song.setSource(rs.getString("source"));
            song.setSourceId(rs.getString("source_id"));
            // format and codec: columns may not exist in old databases
            try { song.setFormat(rs.getString("format")); } catch (Exception ignored) {}
            try { song.setCodec(rs.getString("codec")); } catch (Exception ignored) {}
            return song;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map song", e);
        }
    }
}
