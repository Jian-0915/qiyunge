package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.PlayHistory;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PlayHistoryRepository {

    private final DatabaseManager dbManager;

    public PlayHistoryRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void record(int userId, int songId) {
        dbManager.withConnection(conn -> {
            String sql = "INSERT INTO play_history (user_id, song_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, songId);
                stmt.executeUpdate();
            }
        });
    }

    public List<PlayHistory> findByUser(int userId, int limit) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT ph.id, ph.user_id, ph.song_id, ph.played_at, " +
                         "s.title as song_title, s.artist as song_artist, s.duration as song_duration " +
                         "FROM play_history ph JOIN songs s ON ph.song_id = s.id " +
                         "WHERE ph.user_id = ? ORDER BY ph.played_at DESC LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();
                return mapHistoryList(rs);
            }
        });
    }

    public int countByUser(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM play_history WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    /** 删除用户的所有播放历史 */
    public void deleteByUser(int userId) {
        dbManager.withConnection(conn -> {
            String sql = "DELETE FROM play_history WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }
        });
    }

    /** 分页查询播放历史 */
    public List<PlayHistory> findByUserPaged(int userId, int limit, int offset) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT ph.id, ph.user_id, ph.song_id, ph.played_at, " +
                         "s.title as song_title, s.artist as song_artist, s.duration as song_duration " +
                         "FROM play_history ph JOIN songs s ON ph.song_id = s.id " +
                         "WHERE ph.user_id = ? ORDER BY ph.played_at DESC LIMIT ? OFFSET ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, limit);
                stmt.setInt(3, offset);
                ResultSet rs = stmt.executeQuery();
                return mapHistoryList(rs);
            }
        });
    }

    private List<PlayHistory> mapHistoryList(ResultSet rs) {
        List<PlayHistory> list = new ArrayList<>();
        try {
            while (rs.next()) {
                PlayHistory h = new PlayHistory();
                h.setId(rs.getInt("id"));
                h.setUserId(rs.getInt("user_id"));
                h.setSongId(rs.getInt("song_id"));
                h.setSongTitle(rs.getString("song_title"));
                h.setSongArtist(rs.getString("song_artist"));
                h.setSongDuration(rs.getDouble("song_duration"));
                // 读取 played_at（SQLite 存储为 TEXT 格式 "yyyy-MM-dd HH:mm:ss"）
                String playedAtStr = rs.getString("played_at");
                if (playedAtStr != null) {
                    try {
                        h.setPlayedAt(LocalDateTime.parse(playedAtStr, DateTimeUtil.DT_FMT));
                    } catch (Exception e) {
                        // fallback: try ISO format
                        try { h.setPlayedAt(LocalDateTime.parse(playedAtStr)); } catch (Exception ignored) {}
                    }
                }
                list.add(h);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to map play history", e);
        }
        return list;
    }
}
