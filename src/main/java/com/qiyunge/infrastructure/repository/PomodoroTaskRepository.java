package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.PomodoroTask;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PomodoroTaskRepository {

    private final DatabaseManager dbManager;

    public PomodoroTaskRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public int create(PomodoroTask task) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT INTO pomodoro_tasks (user_id, title, estimated_pomodoros, completed_pomodoros, tag, is_completed, sort_order, task_date, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, task.getUserId());
                stmt.setString(2, task.getTitle());
                stmt.setInt(3, task.getEstimatedPomodoros());
                stmt.setInt(4, task.getCompletedPomodoros());
                stmt.setString(5, task.getTag());
                stmt.setInt(6, task.getIsCompleted());
                stmt.setInt(7, task.getSortOrder());
                stmt.setString(8, task.getTaskDate());
                String now = DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now());
                stmt.setString(9, now);
                stmt.setString(10, now);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : -1;
                }
            }
        });
    }

    public void update(PomodoroTask task) {
        dbManager.withConnection(conn -> {
            String sql = "UPDATE pomodoro_tasks SET title = ?, estimated_pomodoros = ?, completed_pomodoros = ?, tag = ?, is_completed = ?, sort_order = ?, updated_at = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, task.getTitle());
                stmt.setInt(2, task.getEstimatedPomodoros());
                stmt.setInt(3, task.getCompletedPomodoros());
                stmt.setString(4, task.getTag());
                stmt.setInt(5, task.getIsCompleted());
                stmt.setInt(6, task.getSortOrder());
                stmt.setString(7, DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now()));
                stmt.setInt(8, task.getId());
                stmt.executeUpdate();
            }
        });
    }

    public void delete(int taskId) {
        dbManager.withConnection(conn -> {
            String sql = "DELETE FROM pomodoro_tasks WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, taskId);
                stmt.executeUpdate();
            }
        });
    }

    public PomodoroTask findById(int taskId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM pomodoro_tasks WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, taskId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? mapTask(rs) : null;
            }
        });
    }

    public List<PomodoroTask> findByDate(int userId, String taskDate) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM pomodoro_tasks WHERE user_id = ? AND task_date = ? ORDER BY sort_order ASC, id ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, taskDate);
                ResultSet rs = stmt.executeQuery();
                return mapTaskList(rs);
            }
        });
    }

    public void toggleComplete(int taskId) {
        dbManager.withConnection(conn -> {
            String sql = "UPDATE pomodoro_tasks SET is_completed = 1 - is_completed, updated_at = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now()));
                stmt.setInt(2, taskId);
                stmt.executeUpdate();
            }
        });
    }

    public void incrementCompletedPomodoros(int taskId) {
        dbManager.withConnection(conn -> {
            String sql = "UPDATE pomodoro_tasks SET completed_pomodoros = completed_pomodoros + 1, updated_at = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now()));
                stmt.setInt(2, taskId);
                stmt.executeUpdate();
            }
        });
    }

    public int countCompleted(int userId, String taskDate) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM pomodoro_tasks WHERE user_id = ? AND task_date = ? AND is_completed = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, taskDate);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public int countTotal(int userId, String taskDate) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM pomodoro_tasks WHERE user_id = ? AND task_date = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, taskDate);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public void reorderTasks(int userId, String taskDate, List<Integer> taskIds) {
        dbManager.withTransaction(conn -> {
            String sql = "UPDATE pomodoro_tasks SET sort_order = ?, updated_at = ? WHERE id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String now = DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now());
                for (int i = 0; i < taskIds.size(); i++) {
                    stmt.setInt(1, i);
                    stmt.setString(2, now);
                    stmt.setInt(3, taskIds.get(i));
                    stmt.setInt(4, userId);
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }
        });
    }

    private PomodoroTask mapTask(ResultSet rs) {
        try {
            PomodoroTask t = new PomodoroTask();
            t.setId(rs.getInt("id"));
            t.setUserId(rs.getInt("user_id"));
            t.setTitle(rs.getString("title"));
            t.setEstimatedPomodoros(rs.getInt("estimated_pomodoros"));
            t.setCompletedPomodoros(rs.getInt("completed_pomodoros"));
            t.setTag(rs.getString("tag"));
            t.setIsCompleted(rs.getInt("is_completed"));
            t.setSortOrder(rs.getInt("sort_order"));
            t.setTaskDate(rs.getString("task_date"));
            t.setCreatedAt(rs.getString("created_at"));
            t.setUpdatedAt(rs.getString("updated_at"));
            return t;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map pomodoro task", e);
        }
    }

    private List<PomodoroTask> mapTaskList(ResultSet rs) {
        List<PomodoroTask> list = new ArrayList<>();
        try {
            while (rs.next()) {
                list.add(mapTask(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to map pomodoro task list", e);
        }
        return list;
    }
}
