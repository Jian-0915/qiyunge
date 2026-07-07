package com.qiyunge.infrastructure.repository;

import com.qiyunge.infrastructure.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户图片偏好（收藏）数据访问层。
 */
public class UserImagePreferenceRepository {

    private final DatabaseManager dbManager;

    public UserImagePreferenceRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /** 添加收藏 */
    public boolean addPreference(int userId, int imageId) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT OR IGNORE INTO user_image_preferences (user_id, image_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, imageId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 取消收藏 */
    public boolean removePreference(int userId, int imageId) {
        return dbManager.withConnection(conn -> {
            String sql = "DELETE FROM user_image_preferences WHERE user_id = ? AND image_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, imageId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 判断是否已收藏 */
    public boolean isPreferenced(int userId, int imageId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT 1 FROM user_image_preferences WHERE user_id = ? AND image_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, imageId);
                ResultSet rs = stmt.executeQuery();
                return rs.next();
            }
        });
    }

    /** 查询用户收藏的图片 ID 集合 */
    public Set<Integer> findFavoriteImageIds(int userId) {
        return dbManager.withConnection(conn -> {
            Set<Integer> ids = new HashSet<>();
            String sql = "SELECT image_id FROM user_image_preferences WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
            return ids;
        });
    }

    /** 统计用户收藏的图片数量 */
    public int countByUser(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM user_image_preferences WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    /** 批量添加收藏 */
    public void batchAddPreference(int userId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;
        dbManager.withConnection(conn -> {
            String sql = "INSERT OR IGNORE INTO user_image_preferences (user_id, image_id) VALUES " +
                imageIds.stream().map(id -> "(?, ?)").collect(java.util.stream.Collectors.joining(", "));
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < imageIds.size(); i++) {
                    stmt.setInt(i * 2 + 1, userId);
                    stmt.setInt(i * 2 + 2, imageIds.get(i));
                }
                stmt.executeUpdate();
            }
        });
    }

    /** 批量取消收藏 */
    public void batchRemovePreference(int userId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;
        dbManager.withConnection(conn -> {
            String placeholders = imageIds.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
            String sql = "DELETE FROM user_image_preferences WHERE user_id = ? AND image_id IN (" + placeholders + ")";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                for (int i = 0; i < imageIds.size(); i++) {
                    stmt.setInt(i + 2, imageIds.get(i));
                }
                stmt.executeUpdate();
            }
        });
    }

    /** 删除某图片的所有用户收藏记录（图片被删除时调用） */
    public void removeAllImagePreferences(int imageId) {
        dbManager.withConnection(conn -> {
            String sql = "DELETE FROM user_image_preferences WHERE image_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, imageId);
                stmt.executeUpdate();
            }
            return null;
        });
    }
}
