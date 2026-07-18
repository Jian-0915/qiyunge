package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.PomodoroTaskTemplate;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PomodoroTaskTemplateRepository {

    private final DatabaseManager dbManager;

    public PomodoroTaskTemplateRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public int create(PomodoroTaskTemplate template) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT INTO pomodoro_task_templates (user_id, title, estimated_pomodoros, tag, created_at) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, template.getUserId());
                stmt.setString(2, template.getTitle());
                stmt.setInt(3, template.getEstimatedPomodoros());
                stmt.setString(4, template.getTag());
                stmt.setString(5, DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now()));
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : -1;
                }
            }
        });
    }

    public void update(PomodoroTaskTemplate template) {
        dbManager.withConnection(conn -> {
            String sql = "UPDATE pomodoro_task_templates SET title = ?, estimated_pomodoros = ?, tag = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, template.getTitle());
                stmt.setInt(2, template.getEstimatedPomodoros());
                stmt.setString(3, template.getTag());
                stmt.setInt(4, template.getId());
                stmt.executeUpdate();
            }
        });
    }

    public void delete(int templateId) {
        dbManager.withConnection(conn -> {
            String sql = "DELETE FROM pomodoro_task_templates WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, templateId);
                stmt.executeUpdate();
            }
        });
    }

    public List<PomodoroTaskTemplate> findAllByUserId(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM pomodoro_task_templates WHERE user_id = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return mapTemplateList(rs);
            }
        });
    }

    public PomodoroTaskTemplate findById(int templateId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM pomodoro_task_templates WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, templateId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? mapTemplate(rs) : null;
            }
        });
    }

    private PomodoroTaskTemplate mapTemplate(ResultSet rs) {
        try {
            PomodoroTaskTemplate t = new PomodoroTaskTemplate();
            t.setId(rs.getInt("id"));
            t.setUserId(rs.getInt("user_id"));
            t.setTitle(rs.getString("title"));
            t.setEstimatedPomodoros(rs.getInt("estimated_pomodoros"));
            t.setTag(rs.getString("tag"));
            t.setCreatedAt(rs.getString("created_at"));
            return t;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map pomodoro task template", e);
        }
    }

    private List<PomodoroTaskTemplate> mapTemplateList(ResultSet rs) {
        List<PomodoroTaskTemplate> list = new ArrayList<>();
        try {
            while (rs.next()) {
                list.add(mapTemplate(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to map pomodoro task template list", e);
        }
        return list;
    }
}
