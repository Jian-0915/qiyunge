package com.qiyunge.infrastructure.repository;

import com.qiyunge.infrastructure.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FavoriteSongRepository {

    private final DatabaseManager dbManager;

    public FavoriteSongRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean add(int userId, int songId) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT OR IGNORE INTO favorite_songs (user_id, song_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, songId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    public boolean remove(int userId, int songId) {
        return dbManager.withConnection(conn -> {
            String sql = "DELETE FROM favorite_songs WHERE user_id = ? AND song_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, songId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    public boolean isFavorited(int userId, int songId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM favorite_songs WHERE user_id = ? AND song_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, songId);
                var rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        });
    }

    public Set<Integer> getFavoriteSongIds(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT song_id FROM favorite_songs WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                var rs = stmt.executeQuery();
                Set<Integer> ids = new HashSet<>();
                while (rs.next()) {
                    ids.add(rs.getInt("song_id"));
                }
                return ids;
            }
        });
    }

    /** 统计用户收藏歌曲数 */
    public int countByUser(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM favorite_songs WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                var rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }
}
