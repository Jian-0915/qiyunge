package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.AuditLog;
import com.qiyunge.domain.entity.User;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuditLogRepository {

    private final DatabaseManager dbManager;

    public AuditLogRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void log(Integer userId, String action, String targetType, Integer targetId, String detail) {
        dbManager.withConnection(conn -> {
            String sql = "INSERT INTO audit_logs (user_id, action, target_type, target_id, detail) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (userId != null) stmt.setInt(1, userId); else stmt.setNull(1, Types.INTEGER);
                stmt.setString(2, action);
                stmt.setString(3, targetType);
                if (targetId != null) stmt.setInt(4, targetId); else stmt.setNull(4, Types.INTEGER);
                stmt.setString(5, detail);
                stmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("Failed to write audit log: " + e.getMessage());
            }
        });
    }

    public void logLoginSuccess(User user) {
        log(user.getId(), "LOGIN_SUCCESS", "user", user.getId(), "用户登录成功: " + user.getUsername());
    }

    public void logLoginFailed(String username, String reason) {
        log(null, "LOGIN_FAILED", "user", null, "登录失败 [" + username + "]: " + reason);
    }

    public void logLogout(User user) {
        log(user.getId(), "LOGOUT", "user", user.getId(), "用户退出登录: " + user.getUsername());
    }

    public void logPasswordChange(User user) {
        log(user.getId(), "PASSWORD_CHANGE", "user", user.getId(), "用户修改密码: " + user.getUsername());
    }

    public void logAccountDeleted(User user) {
        log(user.getId(), "ACCOUNT_DELETED", "user", user.getId(), "用户注销账号: " + user.getUsername());
    }

    public void logAdminInit(String detail) {
        log(null, "ADMIN_INIT", "system", null, detail);
    }

    // ===== 查询方法 =====

    public List<AuditLog> findRecent(int limit) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT ?";
            List<AuditLog> logs = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) logs.add(mapRow(rs));
            }
            return logs;
        });
    }

    public List<AuditLog> findByUser(int userId, int limit) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM audit_logs WHERE user_id = ? ORDER BY created_at DESC LIMIT ?";
            List<AuditLog> logs = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) logs.add(mapRow(rs));
            }
            return logs;
        });
    }

    public List<AuditLog> findByAction(String action, int limit) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM audit_logs WHERE action = ? ORDER BY created_at DESC LIMIT ?";
            List<AuditLog> logs = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, action);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) logs.add(mapRow(rs));
            }
            return logs;
        });
    }

    private AuditLog mapRow(ResultSet rs) throws SQLException {
        AuditLog log = new AuditLog();
        log.setId(rs.getInt("id"));
        int userId = rs.getInt("user_id");
        log.setUserId(rs.wasNull() ? null : userId);
        log.setAction(rs.getString("action"));
        log.setTargetType(rs.getString("target_type"));
        int targetId = rs.getInt("target_id");
        log.setTargetId(rs.wasNull() ? null : targetId);
        log.setDetail(rs.getString("detail"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreatedAt(DateTimeUtil.parseDateTime(rs.getString("created_at")));
        return log;
    }
}
