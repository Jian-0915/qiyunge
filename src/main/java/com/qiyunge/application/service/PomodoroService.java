package com.qiyunge.application.service;

import com.qiyunge.domain.entity.PomodoroSession;
import com.qiyunge.domain.entity.PomodoroTask;
import com.qiyunge.domain.entity.PomodoroTaskTemplate;
import com.qiyunge.infrastructure.repository.AchievementRepository;
import com.qiyunge.infrastructure.repository.PomodoroSessionRepository;
import com.qiyunge.infrastructure.repository.PomodoroTaskRepository;
import com.qiyunge.infrastructure.repository.PomodoroTaskTemplateRepository;
import com.qiyunge.infrastructure.storage.ConfigStorage;
import com.qiyunge.infrastructure.util.DateTimeUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PomodoroService {

    public enum SessionType {
        FOCUS("focus"),
        SHORT_BREAK("short_break"),
        LONG_BREAK("long_break");

        private final String value;

        SessionType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum BreakMusicBehavior {
        PAUSE("pause"),
        CONTINUE("continue"),
        SWITCH("switch");

        private final String value;

        BreakMusicBehavior(String value) {
            this.value = value;
        }

        public String getValue() { return value; }

        public static BreakMusicBehavior fromValue(String v) {
            for (BreakMusicBehavior b : values()) {
                if (b.value.equals(v)) return b;
            }
            return PAUSE;
        }
    }

    public static class PomodoroSettings {
        private int focusMinutes = 25;
        private int shortBreakMinutes = 5;
        private int longBreakMinutes = 15;
        private int longBreakInterval = 4;
        private boolean autoStartNext = false;
        private boolean alwaysOnTop = false;
        private String soundType = "bell";
        private int soundVolume = 70;
        private Integer musicPlaylistId = null;
        private BreakMusicBehavior breakMusicBehavior = BreakMusicBehavior.PAUSE;
        private int focusVolume = 60;
        private int breakVolume = 40;

        public int getFocusMinutes() { return focusMinutes; }
        public void setFocusMinutes(int v) { this.focusMinutes = v; }
        public int getShortBreakMinutes() { return shortBreakMinutes; }
        public void setShortBreakMinutes(int v) { this.shortBreakMinutes = v; }
        public int getLongBreakMinutes() { return longBreakMinutes; }
        public void setLongBreakMinutes(int v) { this.longBreakMinutes = v; }
        public int getLongBreakInterval() { return longBreakInterval; }
        public void setLongBreakInterval(int v) { this.longBreakInterval = v; }
        public boolean isAutoStartNext() { return autoStartNext; }
        public void setAutoStartNext(boolean v) { this.autoStartNext = v; }
        public boolean isAlwaysOnTop() { return alwaysOnTop; }
        public void setAlwaysOnTop(boolean v) { this.alwaysOnTop = v; }
        public String getSoundType() { return soundType; }
        public void setSoundType(String v) { this.soundType = v; }
        public int getSoundVolume() { return soundVolume; }
        public void setSoundVolume(int v) { this.soundVolume = v; }
        public Integer getMusicPlaylistId() { return musicPlaylistId; }
        public void setMusicPlaylistId(Integer v) { this.musicPlaylistId = v; }
        public BreakMusicBehavior getBreakMusicBehavior() { return breakMusicBehavior; }
        public void setBreakMusicBehavior(BreakMusicBehavior v) { this.breakMusicBehavior = v; }
        public int getFocusVolume() { return focusVolume; }
        public void setFocusVolume(int v) { this.focusVolume = v; }
        public int getBreakVolume() { return breakVolume; }
        public void setBreakVolume(int v) { this.breakVolume = v; }
    }

    public static class DailyStats {
        private final int focusMinutes;
        private final int completedPomodoros;
        private final int completedTasks;
        private final int totalTasks;
        private final int streakDays;

        public DailyStats(int focusMinutes, int completedPomodoros, int completedTasks, int totalTasks, int streakDays) {
            this.focusMinutes = focusMinutes;
            this.completedPomodoros = completedPomodoros;
            this.completedTasks = completedTasks;
            this.totalTasks = totalTasks;
            this.streakDays = streakDays;
        }

        public int getFocusMinutes() { return focusMinutes; }
        public int getCompletedPomodoros() { return completedPomodoros; }
        public int getCompletedTasks() { return completedTasks; }
        public int getTotalTasks() { return totalTasks; }
        public int getStreakDays() { return streakDays; }
    }

    public static class TotalStats {
        private final int totalFocusMinutes;
        private final int totalCompletedPomodoros;
        private final int longestStreak;
        private final int currentStreak;

        public TotalStats(int totalFocusMinutes, int totalCompletedPomodoros, int longestStreak, int currentStreak) {
            this.totalFocusMinutes = totalFocusMinutes;
            this.totalCompletedPomodoros = totalCompletedPomodoros;
            this.longestStreak = longestStreak;
            this.currentStreak = currentStreak;
        }

        public int getTotalFocusMinutes() { return totalFocusMinutes; }
        public int getTotalCompletedPomodoros() { return totalCompletedPomodoros; }
        public int getLongestStreak() { return longestStreak; }
        public int getCurrentStreak() { return currentStreak; }
    }

    public static class UnlockResult {
        private final String achievementId;
        private final String title;
        private final boolean newlyUnlocked;

        public UnlockResult(String achievementId, String title, boolean newlyUnlocked) {
            this.achievementId = achievementId;
            this.title = title;
            this.newlyUnlocked = newlyUnlocked;
        }

        public String getAchievementId() { return achievementId; }
        public String getTitle() { return title; }
        public boolean isNewlyUnlocked() { return newlyUnlocked; }
    }

    private static final Map<String, String> ACHIEVEMENT_TITLES = new HashMap<>();
    static {
        ACHIEVEMENT_TITLES.put("pomodoro_first", "初试锋芒");
        ACHIEVEMENT_TITLES.put("pomodoro_10", "笃行不怠");
        ACHIEVEMENT_TITLES.put("pomodoro_50", "锲而不舍");
        ACHIEVEMENT_TITLES.put("pomodoro_100", "金石可镂");
        ACHIEVEMENT_TITLES.put("pomodoro_500", "磨杵成针");
        ACHIEVEMENT_TITLES.put("pomodoro_streak_7", "七日一心");
        ACHIEVEMENT_TITLES.put("pomodoro_streak_30", "月月恒一");
        ACHIEVEMENT_TITLES.put("pomodoro_streak_100", "百日筑基");
        ACHIEVEMENT_TITLES.put("pomodoro_daily_5", "一日五熟");
        ACHIEVEMENT_TITLES.put("pomodoro_daily_10", "十日并出");
        ACHIEVEMENT_TITLES.put("pomodoro_task_10", "功课圆满");
        ACHIEVEMENT_TITLES.put("pomodoro_task_50", "课业精进");
    }

    private final PomodoroSessionRepository sessionRepository;
    private final PomodoroTaskRepository taskRepository;
    private final PomodoroTaskTemplateRepository templateRepository;
    private final AchievementRepository achievementRepository;
    private final ConfigStorage configStorage;

    public PomodoroService(PomodoroSessionRepository sessionRepository,
                            PomodoroTaskRepository taskRepository,
                            PomodoroTaskTemplateRepository templateRepository,
                            AchievementRepository achievementRepository,
                            ConfigStorage configStorage) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.templateRepository = templateRepository;
        this.achievementRepository = achievementRepository;
        this.configStorage = configStorage;
    }

    public int createSession(int userId, Integer taskId, int durationMinutes, SessionType sessionType,
                              String startTime, String endTime, boolean isCompleted, String tag) {
        String today = LocalDate.now().toString();
        PomodoroSession session = new PomodoroSession();
        session.setUserId(userId);
        session.setTaskId(taskId);
        session.setDurationMinutes(durationMinutes);
        session.setSessionType(sessionType.getValue());
        session.setSessionDate(today);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setIsCompleted(isCompleted ? 1 : 0);
        session.setTag(tag);
        int sessionId = sessionRepository.create(session);

        if (isCompleted && sessionType == SessionType.FOCUS) {
            if (taskId != null) {
                taskRepository.incrementCompletedPomodoros(taskId);
            }
            checkAchievements(userId);
        }

        return sessionId;
    }

    public List<PomodoroSession> getSessionsByDate(int userId, String date) {
        return sessionRepository.findByDate(userId, date);
    }

    public List<PomodoroSession> getSessionsByDateRange(int userId, String startDate, String endDate) {
        return sessionRepository.findByDateRange(userId, startDate, endDate);
    }

    public DailyStats getDailyStats(int userId) {
        String today = LocalDate.now().toString();
        int focusMinutes = sessionRepository.getTodayFocusMinutes(userId, today);
        int completedPomodoros = sessionRepository.getTodayCompletedPomodoros(userId, today);
        int completedTasks = taskRepository.countCompleted(userId, today);
        int totalTasks = taskRepository.countTotal(userId, today);
        int streakDays = sessionRepository.getStreakDays(userId, today);
        return new DailyStats(focusMinutes, completedPomodoros, completedTasks, totalTasks, streakDays);
    }

    public TotalStats getTotalStats(int userId) {
        String today = LocalDate.now().toString();
        int totalFocusMinutes = sessionRepository.getTotalFocusMinutes(userId);
        int totalCompletedPomodoros = sessionRepository.getTotalCompletedPomodoros(userId);
        int longestStreak = sessionRepository.getLongestStreak(userId);
        int currentStreak = sessionRepository.getStreakDays(userId, today);
        return new TotalStats(totalFocusMinutes, totalCompletedPomodoros, longestStreak, currentStreak);
    }

    public Map<String, Integer> getWeeklyStats(int userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        Map<String, Integer> rawData = sessionRepository.getWeeklyStats(userId, weekStart.toString(), today.toString());
        Map<String, Integer> result = new HashMap<>();
        for (int i = 0; i < 7; i++) {
            String date = weekStart.plusDays(i).toString();
            result.put(date, rawData.getOrDefault(date, 0));
        }
        return result;
    }

    public Map<String, Integer> getMonthlyStats(int userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        Map<String, Integer> rawData = sessionRepository.getWeeklyStats(userId, monthStart.toString(), today.toString());
        int daysInMonth = today.lengthOfMonth();
        Map<String, Integer> result = new HashMap<>();
        for (int i = 1; i <= daysInMonth; i++) {
            String date = monthStart.withDayOfMonth(i).toString();
            result.put(date, rawData.getOrDefault(date, 0));
        }
        return result;
    }

    public Map<String, Integer> getTagDistribution(int userId, String startDate, String endDate) {
        return sessionRepository.getTagDistribution(userId, startDate, endDate);
    }

    public int createTask(int userId, String title, int estimatedPomodoros, String tag) {
        String today = LocalDate.now().toString();
        PomodoroTask task = new PomodoroTask();
        task.setUserId(userId);
        task.setTitle(title);
        task.setEstimatedPomodoros(estimatedPomodoros);
        task.setCompletedPomodoros(0);
        task.setTag(tag);
        task.setIsCompleted(0);
        task.setSortOrder(0);
        task.setTaskDate(today);
        return taskRepository.create(task);
    }

    public void updateTask(PomodoroTask task) {
        taskRepository.update(task);
    }

    public void deleteTask(int taskId) {
        taskRepository.delete(taskId);
    }

    public PomodoroTask getTask(int taskId) {
        return taskRepository.findById(taskId);
    }

    public List<PomodoroTask> getTodayTasks(int userId) {
        String today = LocalDate.now().toString();
        return taskRepository.findByDate(userId, today);
    }

    public List<PomodoroTask> getTasksByDate(int userId, String date) {
        return taskRepository.findByDate(userId, date);
    }

    public void toggleTaskComplete(int taskId) {
        taskRepository.toggleComplete(taskId);
    }

    public void reorderTasks(int userId, List<Integer> taskIds) {
        String today = LocalDate.now().toString();
        taskRepository.reorderTasks(userId, today, taskIds);
    }

    public int createTemplate(int userId, String title, int estimatedPomodoros, String tag) {
        PomodoroTaskTemplate template = new PomodoroTaskTemplate();
        template.setUserId(userId);
        template.setTitle(title);
        template.setEstimatedPomodoros(estimatedPomodoros);
        template.setTag(tag);
        return templateRepository.create(template);
    }

    public void updateTemplate(PomodoroTaskTemplate template) {
        templateRepository.update(template);
    }

    public void deleteTemplate(int templateId) {
        templateRepository.delete(templateId);
    }

    public List<PomodoroTaskTemplate> getTemplates(int userId) {
        return templateRepository.findAllByUserId(userId);
    }

    public PomodoroTaskTemplate getTemplate(int templateId) {
        return templateRepository.findById(templateId);
    }

    public int createTaskFromTemplate(int userId, int templateId) {
        PomodoroTaskTemplate template = templateRepository.findById(templateId);
        if (template == null) return -1;
        return createTask(userId, template.getTitle(), template.getEstimatedPomodoros(), template.getTag());
    }

    public PomodoroSettings loadSettings() {
        PomodoroSettings s = new PomodoroSettings();
        s.setFocusMinutes(configStorage.getInt("pomodoro.focus_minutes", 25));
        s.setShortBreakMinutes(configStorage.getInt("pomodoro.short_break_minutes", 5));
        s.setLongBreakMinutes(configStorage.getInt("pomodoro.long_break_minutes", 15));
        s.setLongBreakInterval(configStorage.getInt("pomodoro.long_break_interval", 4));
        s.setAutoStartNext(configStorage.getBoolean("pomodoro.auto_start_next", false));
        s.setAlwaysOnTop(configStorage.getBoolean("pomodoro.always_on_top", false));
        s.setSoundType(configStorage.get("pomodoro.sound_type", "bell"));
        s.setSoundVolume(configStorage.getInt("pomodoro.sound_volume", 70));
        String pid = configStorage.get("pomodoro.music_playlist_id", null);
        if (pid != null && !pid.isEmpty()) {
            try { s.setMusicPlaylistId(Integer.parseInt(pid)); } catch (NumberFormatException e) { s.setMusicPlaylistId(null); }
        }
        s.setBreakMusicBehavior(BreakMusicBehavior.fromValue(
            configStorage.get("pomodoro.break_music_behavior", "pause")
        ));
        s.setFocusVolume(configStorage.getInt("pomodoro.focus_volume", 60));
        s.setBreakVolume(configStorage.getInt("pomodoro.break_volume", 40));
        return s;
    }

    public void saveSettings(PomodoroSettings settings) {
        configStorage.set("pomodoro.focus_minutes", String.valueOf(settings.getFocusMinutes()));
        configStorage.set("pomodoro.short_break_minutes", String.valueOf(settings.getShortBreakMinutes()));
        configStorage.set("pomodoro.long_break_minutes", String.valueOf(settings.getLongBreakMinutes()));
        configStorage.set("pomodoro.long_break_interval", String.valueOf(settings.getLongBreakInterval()));
        configStorage.set("pomodoro.auto_start_next", String.valueOf(settings.isAutoStartNext()));
        configStorage.set("pomodoro.always_on_top", String.valueOf(settings.isAlwaysOnTop()));
        configStorage.set("pomodoro.sound_type", settings.getSoundType());
        configStorage.set("pomodoro.sound_volume", String.valueOf(settings.getSoundVolume()));
        if (settings.getMusicPlaylistId() != null) {
            configStorage.set("pomodoro.music_playlist_id", String.valueOf(settings.getMusicPlaylistId()));
        } else {
            configStorage.set("pomodoro.music_playlist_id", "");
        }
        configStorage.set("pomodoro.break_music_behavior", settings.getBreakMusicBehavior().getValue());
        configStorage.set("pomodoro.focus_volume", String.valueOf(settings.getFocusVolume()));
        configStorage.set("pomodoro.break_volume", String.valueOf(settings.getBreakVolume()));
    }

    public Integer getMusicPlaylistId() {
        return loadSettings().getMusicPlaylistId();
    }

    public BreakMusicBehavior getBreakMusicBehavior() {
        return loadSettings().getBreakMusicBehavior();
    }

    public int getFocusVolume() {
        return loadSettings().getFocusVolume();
    }

    public int getBreakVolume() {
        return loadSettings().getBreakVolume();
    }

    public String calculateEstimatedFinishTime(int userId) {
        PomodoroSettings s = loadSettings();
        return calculateEstimatedFinishTime(userId, s.getFocusMinutes(), s.getShortBreakMinutes(),
            s.getLongBreakMinutes(), s.getLongBreakInterval());
    }

    public String calculateEstimatedFinishTime(int userId, int focusMinutes, int shortBreakMinutes,
                                                  int longBreakMinutes, int longBreakInterval) {
        DailyStats stats = getDailyStats(userId);
        int remainingPomodoros = 0;
        List<PomodoroTask> tasks = getTodayTasks(userId);
        for (PomodoroTask task : tasks) {
            if (task.getIsCompleted() == 0) {
                remainingPomodoros += (task.getEstimatedPomodoros() - task.getCompletedPomodoros());
            }
        }
        if (remainingPomodoros <= 0) return null;

        int totalMinutes = 0;
        for (int i = 0; i < remainingPomodoros; i++) {
            totalMinutes += focusMinutes;
            if (i < remainingPomodoros - 1) {
                if ((i + 1) % longBreakInterval == 0) {
                    totalMinutes += longBreakMinutes;
                } else {
                    totalMinutes += shortBreakMinutes;
                }
            }
        }

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        java.time.LocalDateTime finishTime = now.plusMinutes(totalMinutes);
        return DateTimeUtil.DT_FMT.format(finishTime);
    }

    public List<UnlockResult> checkAchievements(int userId) {
        List<UnlockResult> newlyUnlocked = new ArrayList<>();
        TotalStats total = getTotalStats(userId);
        DailyStats daily = getDailyStats(userId);
        String today = LocalDate.now().toString();
        int completedTasksCount = taskRepository.countCompleted(userId, today);
        int totalCompletedTasks = 0;
        List<PomodoroTask> allTasks = taskRepository.findByDate(userId, today);
        for (PomodoroTask t : allTasks) {
            if (t.getIsCompleted() == 1) totalCompletedTasks++;
        }

        String[][] checks = {
            {"pomodoro_first", total.getTotalCompletedPomodoros() >= 1 ? "1" : "0"},
            {"pomodoro_10", total.getTotalFocusMinutes() >= 600 ? "1" : "0"},
            {"pomodoro_50", total.getTotalFocusMinutes() >= 3000 ? "1" : "0"},
            {"pomodoro_100", total.getTotalFocusMinutes() >= 6000 ? "1" : "0"},
            {"pomodoro_500", total.getTotalFocusMinutes() >= 30000 ? "1" : "0"},
            {"pomodoro_streak_7", total.getCurrentStreak() >= 7 ? "1" : "0"},
            {"pomodoro_streak_30", total.getCurrentStreak() >= 30 ? "1" : "0"},
            {"pomodoro_streak_100", total.getCurrentStreak() >= 100 ? "1" : "0"},
            {"pomodoro_daily_5", daily.getCompletedPomodoros() >= 5 ? "1" : "0"},
            {"pomodoro_daily_10", daily.getCompletedPomodoros() >= 10 ? "1" : "0"},
            {"pomodoro_task_10", totalCompletedTasks >= 10 ? "1" : "0"},
            {"pomodoro_task_50", totalCompletedTasks >= 50 ? "1" : "0"},
        };

        for (String[] check : checks) {
            String id = check[0];
            boolean shouldUnlock = "1".equals(check[1]);
            if (shouldUnlock && !achievementRepository.isUnlocked(userId, id)) {
                boolean unlocked = achievementRepository.unlock(userId, id);
                if (unlocked) {
                    newlyUnlocked.add(new UnlockResult(id, ACHIEVEMENT_TITLES.getOrDefault(id, id), true));
                }
            }
        }

        return newlyUnlocked;
    }

    public List<UnlockResult> onFocusSessionComplete(int userId) {
        return checkAchievements(userId);
    }

    public Map<String, String> getAchievementTitles() {
        return new HashMap<>(ACHIEVEMENT_TITLES);
    }
}
