package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.GameRecord;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GameRecordRepository {

    private final DatabaseManager dbManager;

    public GameRecordRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(GameRecord record) {
        dbManager.withConnection(conn -> {
            String sql = "INSERT INTO game_records (user_id, game_type, difficulty, score, time_seconds, created_at) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, record.getUserId());
                stmt.setString(2, record.getGameType());
                stmt.setString(3, record.getDifficulty());
                stmt.setInt(4, record.getScore());
                if (record.getTimeSeconds() != null) {
                    stmt.setInt(5, record.getTimeSeconds());
                } else {
                    stmt.setNull(5, java.sql.Types.INTEGER);
                }
                stmt.setString(6, DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now()));
                stmt.executeUpdate();
            }
        });
    }

    public List<GameRecord> findByUserAndType(int userId, String gameType) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM game_records WHERE user_id = ? AND game_type = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, gameType);
                ResultSet rs = stmt.executeQuery();
                return mapRecordList(rs);
            }
        });
    }

    public GameRecord findBestScore(int userId, String gameType, String difficulty) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM game_records WHERE user_id = ? AND game_type = ? AND difficulty = ? ORDER BY score ASC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, gameType);
                stmt.setString(3, difficulty);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? mapRecord(rs) : null;
            }
        });
    }

    public GameRecord findBestTime(int userId, String gameType, String difficulty) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM game_records WHERE user_id = ? AND game_type = ? AND difficulty = ? AND time_seconds IS NOT NULL ORDER BY time_seconds ASC LIMIT 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, gameType);
                stmt.setString(3, difficulty);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? mapRecord(rs) : null;
            }
        });
    }

    public int countByUser(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM game_records WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public int countByUserAndType(int userId, String gameType) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM game_records WHERE user_id = ? AND game_type = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, gameType);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public List<GameRecord> findRecentByUser(int userId, int limit) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM game_records WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();
                return mapRecordList(rs);
            }
        });
    }

    private GameRecord mapRecord(ResultSet rs) {
        try {
            GameRecord r = new GameRecord();
            r.setId(rs.getInt("id"));
            r.setUserId(rs.getInt("user_id"));
            r.setGameType(rs.getString("game_type"));
            r.setDifficulty(rs.getString("difficulty"));
            r.setScore(rs.getInt("score"));
            int timeSeconds = rs.getInt("time_seconds");
            if (rs.wasNull()) {
                r.setTimeSeconds(null);
            } else {
                r.setTimeSeconds(timeSeconds);
            }
            r.setCreatedAt(rs.getString("created_at"));
            return r;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map game record", e);
        }
    }

    private List<GameRecord> mapRecordList(ResultSet rs) {
        List<GameRecord> list = new ArrayList<>();
        try {
            while (rs.next()) {
                list.add(mapRecord(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to map game record list", e);
        }
        return list;
    }
}
