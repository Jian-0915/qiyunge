package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.UserFaceData;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserFaceDataRepository {

    private final DatabaseManager dbManager;

    public UserFaceDataRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Optional<UserFaceData> findByUserId(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM user_face_data WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
            return Optional.<UserFaceData>empty();
        });
    }

    public Optional<UserFaceData> findById(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM user_face_data WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
            return Optional.<UserFaceData>empty();
        });
    }

    public List<UserFaceData> findAllEnabled() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM user_face_data WHERE enabled = 1";
            List<UserFaceData> list = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        });
    }

    public UserFaceData save(UserFaceData data) {
        return dbManager.withTransaction(conn -> {
            Optional<UserFaceData> existing = findByUserId(data.getUserId());
            if (existing.isPresent()) {
                return update(conn, data);
            } else {
                return insert(conn, data);
            }
        });
    }

    private UserFaceData insert(Connection conn, UserFaceData data) throws SQLException {
        String sql = "INSERT INTO user_face_data (user_id, model_path, face_image_path, sample_count, enabled) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, data.getUserId());
            stmt.setString(2, data.getModelPath());
            stmt.setString(3, data.getFaceImagePath());
            stmt.setInt(4, data.getSampleCount());
            stmt.setInt(5, data.isEnabled() ? 1 : 0);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                data.setId(keys.getInt(1));
            }
            return data;
        }
    }

    private UserFaceData update(Connection conn, UserFaceData data) throws SQLException {
        String sql = "UPDATE user_face_data SET model_path = ?, face_image_path = ?, sample_count = ?, " +
                     "enabled = ?, updated_at = datetime('now', 'localtime') WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, data.getModelPath());
            stmt.setString(2, data.getFaceImagePath());
            stmt.setInt(3, data.getSampleCount());
            stmt.setInt(4, data.isEnabled() ? 1 : 0);
            stmt.setInt(5, data.getUserId());
            stmt.executeUpdate();
            return data;
        }
    }

    public boolean deleteByUserId(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "DELETE FROM user_face_data WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    public boolean existsByUserId(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM user_face_data WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        });
    }

    private UserFaceData mapRow(ResultSet rs) throws SQLException {
        UserFaceData data = new UserFaceData();
        data.setId(rs.getInt("id"));
        data.setUserId(rs.getInt("user_id"));
        data.setModelPath(rs.getString("model_path"));
        data.setFaceImagePath(rs.getString("face_image_path"));
        data.setSampleCount(rs.getInt("sample_count"));
        data.setEnabled(rs.getInt("enabled") == 1);
        data.setCreatedAt(DateTimeUtil.parseDateTime(rs.getString("created_at")));
        data.setUpdatedAt(DateTimeUtil.parseDateTime(rs.getString("updated_at")));
        return data;
    }
}
