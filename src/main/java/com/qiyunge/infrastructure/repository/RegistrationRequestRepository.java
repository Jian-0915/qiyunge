package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.RegistrationRequest;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegistrationRequestRepository {

    private final DatabaseManager dbManager;

    public RegistrationRequestRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public RegistrationRequest create(RegistrationRequest req) {
        return dbManager.withTransaction(conn -> {
            String sql = "INSERT INTO registration_requests (username, password_hash, display_name, reason) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, req.getUsername());
                stmt.setString(2, req.getPasswordHash());
                stmt.setString(3, req.getDisplayName());
                stmt.setString(4, req.getReason());
                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) req.setId(keys.getInt(1));
                return req;
            }
        });
    }

    public Optional<RegistrationRequest> findById(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM registration_requests WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return Optional.of(mapRow(rs));
            }
            return Optional.<RegistrationRequest>empty();
        });
    }

    public List<RegistrationRequest> findPending() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM registration_requests WHERE status = 'pending' ORDER BY created_at DESC";
            List<RegistrationRequest> list = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        });
    }

    public List<RegistrationRequest> findAll() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM registration_requests ORDER BY created_at DESC";
            List<RegistrationRequest> list = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        });
    }

    public int countPending() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM registration_requests WHERE status = 'pending'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public boolean approve(int requestId, int reviewedBy) {
        return dbManager.withConnection(conn -> {
            String sql = "UPDATE registration_requests SET status = 'approved', reviewed_at = datetime('now', 'localtime'), reviewed_by = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reviewedBy);
                stmt.setInt(2, requestId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    public boolean reject(int requestId, int reviewedBy) {
        return dbManager.withConnection(conn -> {
            String sql = "UPDATE registration_requests SET status = 'rejected', reviewed_at = datetime('now', 'localtime'), reviewed_by = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reviewedBy);
                stmt.setInt(2, requestId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    private RegistrationRequest mapRow(ResultSet rs) throws SQLException {
        RegistrationRequest req = new RegistrationRequest();
        req.setId(rs.getInt("id"));
        req.setUsername(rs.getString("username"));
        req.setPasswordHash(rs.getString("password_hash"));
        req.setDisplayName(rs.getString("display_name"));
        req.setReason(rs.getString("reason"));
        req.setStatus(rs.getString("status"));
        req.setCreatedAt(DateTimeUtil.parseDateTime(rs.getString("created_at")));
        req.setReviewedAt(DateTimeUtil.parseDateTime(rs.getString("reviewed_at")));
        int reviewedBy = rs.getInt("reviewed_by");
        req.setReviewedBy(rs.wasNull() ? null : reviewedBy);
        return req;
    }
}
