package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.User;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    private final DatabaseManager dbManager;

    public UserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public Optional<User> findByUsername(String username) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM users WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
            return Optional.<User>empty();
        });
    }

    public Optional<User> findById(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM users WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
            return Optional.<User>empty();
        });
    }

    public User createUser(User user) {
        return dbManager.withTransaction(conn -> {
            String sql = "INSERT INTO users (username, password_hash, email, role, status, display_name, avatar_color, must_change_password) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPasswordHash());
                stmt.setString(3, user.getEmail());
                stmt.setString(4, user.getRole());
                stmt.setString(5, user.getStatus());
                stmt.setString(6, user.getDisplayName());
                stmt.setString(7, user.getAvatarColor());
                stmt.setInt(8, user.isMustChangePassword() ? 1 : 0);
                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
                return user;
            }
        });
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, must_change_password = 0, updated_at = datetime('now', 'localtime') WHERE id = ?";
        return updateField(userId, sql, stmt -> stmt.setString(1, newPasswordHash));
    }

    /** 管理员重置密码：强制用户下次登录改密 */
    public boolean resetPassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, must_change_password = 1, updated_at = datetime('now', 'localtime') WHERE id = ?";
        return updateField(userId, sql, stmt -> stmt.setString(1, newPasswordHash));
    }

    public boolean updateStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";
        return updateField(userId, sql, stmt -> stmt.setString(1, status));
    }

    public boolean updateLastLoginAt(int userId) {
        String sql = "UPDATE users SET last_login_at = datetime('now', 'localtime') WHERE id = ?";
        return updateField(userId, sql, stmt -> {});
    }

    public boolean updateDisplayName(int userId, String displayName) {
        String sql = "UPDATE users SET display_name = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";
        return updateField(userId, sql, stmt -> stmt.setString(1, displayName));
    }

    public boolean updateAvatarColor(int userId, String avatarColor) {
        String sql = "UPDATE users SET avatar_color = ?, updated_at = datetime('now', 'localtime') WHERE id = ?";
        return updateField(userId, sql, stmt -> stmt.setString(1, avatarColor));
    }

    /**
     * 通用字段更新辅助方法。
     */
    private boolean updateField(int userId, String sql, DatabaseManager.SqlConsumer<PreparedStatement> paramBinder) {
        return dbManager.withConnection(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int paramCount = stmt.getParameterMetaData().getParameterCount();
                stmt.setInt(paramCount, userId);
                paramBinder.accept(stmt);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    public boolean existsAdmin() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM users WHERE role = 'admin'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return rs.getInt(1) > 0;
            }
            return false;
        });
    }

    public List<User> findAll() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM users ORDER BY created_at DESC";
            List<User> users = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) users.add(mapRow(rs));
            }
            return users;
        });
    }

    public int countActive() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM users WHERE status = 'active'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public int countActiveAdmins() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM users WHERE role = 'admin' AND status = 'active'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public boolean deleteUser(int userId) {
        return dbManager.withTransaction(conn -> {
            String username;
            try (PreparedStatement stmt = conn.prepareStatement(
                    "SELECT username FROM users WHERE id = ?")) {
                stmt.setInt(1, userId);
                try (var rs = stmt.executeQuery()) {
                    if (!rs.next()) return false;
                    username = rs.getString("username");
                }
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM audit_logs WHERE user_id = ?")) {
                stmt.setInt(1, userId);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM registration_requests WHERE username = ?")) {
                stmt.setString(1, username);
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM users WHERE id = ?")) {
                stmt.setInt(1, userId);
                int rows = stmt.executeUpdate();
                if (rows == 0) return false;
                return true;
            }
        });
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setRole(rs.getString("role"));
        user.setStatus(rs.getString("status"));
        user.setDisplayName(rs.getString("display_name"));
        user.setAvatarColor(rs.getString("avatar_color"));
        user.setMustChangePassword(rs.getInt("must_change_password") == 1);
        user.setCreatedAt(DateTimeUtil.parseDateTime(rs.getString("created_at")));
        user.setUpdatedAt(DateTimeUtil.parseDateTime(rs.getString("updated_at")));
        user.setLastLoginAt(DateTimeUtil.parseDateTime(rs.getString("last_login_at")));
        return user;
    }
}
