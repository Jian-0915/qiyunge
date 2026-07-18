package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.PomodoroSession;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PomodoroSessionRepository {

    private final DatabaseManager dbManager;

    public PomodoroSessionRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public int create(PomodoroSession session) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT INTO pomodoro_sessions (user_id, task_id, duration_minutes, session_type, session_date, start_time, end_time, is_completed, tag, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, session.getUserId());
                if (session.getTaskId() != null) {
                    stmt.setInt(2, session.getTaskId());
                } else {
                    stmt.setNull(2, java.sql.Types.INTEGER);
                }
                stmt.setInt(3, session.getDurationMinutes());
                stmt.setString(4, session.getSessionType());
                stmt.setString(5, session.getSessionDate());
                stmt.setString(6, session.getStartTime());
                stmt.setString(7, session.getEndTime());
                stmt.setInt(8, session.getIsCompleted());
                stmt.setString(9, session.getTag());
                stmt.setString(10, DateTimeUtil.DT_FMT.format(java.time.LocalDateTime.now()));
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : -1;
                }
            }
        });
    }

    public List<PomodoroSession> findByDateRange(int userId, String startDate, String endDate) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM pomodoro_sessions WHERE user_id = ? AND session_date >= ? AND session_date <= ? AND is_completed = 1 ORDER BY session_date ASC, created_at ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, startDate);
                stmt.setString(3, endDate);
                ResultSet rs = stmt.executeQuery();
                return mapSessionList(rs);
            }
        });
    }

    public List<PomodoroSession> findByDate(int userId, String date) {
        return findByDateRange(userId, date, date);
    }

    public int getTodayFocusMinutes(int userId, String today) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM pomodoro_sessions WHERE user_id = ? AND session_date = ? AND session_type = 'focus' AND is_completed = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, today);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public int getTodayCompletedPomodoros(int userId, String today) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM pomodoro_sessions WHERE user_id = ? AND session_date = ? AND session_type = 'focus' AND is_completed = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, today);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public int getTotalFocusMinutes(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COALESCE(SUM(duration_minutes), 0) FROM pomodoro_sessions WHERE user_id = ? AND session_type = 'focus' AND is_completed = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public int getTotalCompletedPomodoros(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM pomodoro_sessions WHERE user_id = ? AND session_type = 'focus' AND is_completed = 1";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    public Map<String, Integer> getWeeklyStats(int userId, String startDate, String endDate) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT session_date, COALESCE(SUM(duration_minutes), 0) as total FROM pomodoro_sessions WHERE user_id = ? AND session_date >= ? AND session_date <= ? AND session_type = 'focus' AND is_completed = 1 GROUP BY session_date ORDER BY session_date ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, startDate);
                stmt.setString(3, endDate);
                ResultSet rs = stmt.executeQuery();
                Map<String, Integer> result = new HashMap<>();
                while (rs.next()) {
                    result.put(rs.getString("session_date"), rs.getInt("total"));
                }
                return result;
            }
        });
    }

    public Map<String, Integer> getTagDistribution(int userId, String startDate, String endDate) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COALESCE(tag, '未分类') as tag_name, COALESCE(SUM(duration_minutes), 0) as total FROM pomodoro_sessions WHERE user_id = ? AND session_date >= ? AND session_date <= ? AND session_type = 'focus' AND is_completed = 1 GROUP BY tag ORDER BY total DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, startDate);
                stmt.setString(3, endDate);
                ResultSet rs = stmt.executeQuery();
                Map<String, Integer> result = new HashMap<>();
                while (rs.next()) {
                    result.put(rs.getString("tag_name"), rs.getInt("total"));
                }
                return result;
            }
        });
    }

    public int getStreakDays(int userId, String today) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT DISTINCT session_date FROM pomodoro_sessions WHERE user_id = ? AND session_date <= ? AND session_type = 'focus' AND is_completed = 1 ORDER BY session_date DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                stmt.setString(2, today);
                ResultSet rs = stmt.executeQuery();
                List<String> dates = new ArrayList<>();
                while (rs.next()) {
                    dates.add(rs.getString("session_date"));
                }
                if (dates.isEmpty()) return 0;
                int streak = 0;
                java.time.LocalDate current = java.time.LocalDate.parse(today);
                for (String dateStr : dates) {
                    java.time.LocalDate d = java.time.LocalDate.parse(dateStr);
                    if (d.equals(current)) {
                        streak++;
                        current = current.minusDays(1);
                    } else if (d.isBefore(current)) {
                        break;
                    }
                }
                return streak;
            }
        });
    }

    public int getLongestStreak(int userId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT DISTINCT session_date FROM pomodoro_sessions WHERE user_id = ? AND session_type = 'focus' AND is_completed = 1 ORDER BY session_date ASC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                List<String> dates = new ArrayList<>();
                while (rs.next()) {
                    dates.add(rs.getString("session_date"));
                }
                if (dates.isEmpty()) return 0;
                int maxStreak = 1;
                int currentStreak = 1;
                for (int i = 1; i < dates.size(); i++) {
                    java.time.LocalDate prev = java.time.LocalDate.parse(dates.get(i - 1));
                    java.time.LocalDate curr = java.time.LocalDate.parse(dates.get(i));
                    if (prev.plusDays(1).equals(curr)) {
                        currentStreak++;
                        maxStreak = Math.max(maxStreak, currentStreak);
                    } else {
                        currentStreak = 1;
                    }
                }
                return maxStreak;
            }
        });
    }

    private PomodoroSession mapSession(ResultSet rs) {
        try {
            PomodoroSession s = new PomodoroSession();
            s.setId(rs.getInt("id"));
            s.setUserId(rs.getInt("user_id"));
            int taskId = rs.getInt("task_id");
            if (rs.wasNull()) {
                s.setTaskId(null);
            } else {
                s.setTaskId(taskId);
            }
            s.setDurationMinutes(rs.getInt("duration_minutes"));
            s.setSessionType(rs.getString("session_type"));
            s.setSessionDate(rs.getString("session_date"));
            s.setStartTime(rs.getString("start_time"));
            s.setEndTime(rs.getString("end_time"));
            s.setIsCompleted(rs.getInt("is_completed"));
            s.setTag(rs.getString("tag"));
            s.setCreatedAt(rs.getString("created_at"));
            return s;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map pomodoro session", e);
        }
    }

    private List<PomodoroSession> mapSessionList(ResultSet rs) {
        List<PomodoroSession> list = new ArrayList<>();
        try {
            while (rs.next()) {
                list.add(mapSession(rs));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to map pomodoro session list", e);
        }
        return list;
    }
}
