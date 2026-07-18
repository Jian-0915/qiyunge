package com.qiyunge.infrastructure.repository;

import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class AchievementRepository {

    private final DatabaseManager dbManager;

    public AchievementRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public boolean unlock(int userId, String achievementId) {
        return dbManager.withConnection(conn -> {
            String checkSql = "SELECT COUNT(*) FROM achievements WHERE user_id = ? AND achievement_id = ?";
            try (PreparedStatement check = conn.prepareStatement(checkSql)) {
                check.setInt(1, userId);
                check.setString(2, achievementId);
                ResultSet rs = check.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    return false;
                }
            }
            String insertSql = "INSERT INTO achievements (user_id, achievement_id, unlocked_at) VALUES (?, ?, ?)";
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                insert.setInt(1, userId);
                insert.setString(2, achievementId);
                insert.setString(3, DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now()));
                insert.executeUpdate();
                return true;
            }
        });
    }

    public boolean isUnlocked(int userId, String achievementId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM achievements WHERE user_id = ? AND achievement_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, achievementId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        });
    }

    public List<String> getUnlockedAchievements(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT achievement_id FROM achievements WHERE user_id = ? ORDER BY unlocked_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                List<String> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(rs.getString("achievement_id"));
                }
                return list;
            }
        });
    }

    public int countUnlocked(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM achievements WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }
}
