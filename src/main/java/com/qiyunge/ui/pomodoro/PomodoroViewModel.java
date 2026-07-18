package com.qiyunge.ui.pomodoro;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.AsyncExecutor;
import com.qiyunge.application.service.PomodoroService;
import com.qiyunge.application.service.PomodoroService.*;
import com.qiyunge.domain.entity.PomodoroTask;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PomodoroViewModel {

    private final AppContext appContext;
    private final PomodoroService pomodoroService;
    private final AsyncExecutor asyncExecutor;
    private final int userId;

    private final ObjectProperty<SessionType> currentSessionType = new SimpleObjectProperty<>(SessionType.FOCUS);
    private final IntegerProperty remainingSeconds = new SimpleIntegerProperty(25 * 60);
    private final IntegerProperty totalSeconds = new SimpleIntegerProperty(25 * 60);
    private final BooleanProperty isRunning = new SimpleBooleanProperty(false);
    private final IntegerProperty currentRound = new SimpleIntegerProperty(1);
    private final IntegerProperty completedFocusRounds = new SimpleIntegerProperty(0);
    private final StringProperty poeticHint = new SimpleStringProperty("心如止水，万事可期");
    private final ObjectProperty<PomodoroTask> activeTask = new SimpleObjectProperty<>(null);

    private final IntegerProperty focusMinutes = new SimpleIntegerProperty(25);
    private final IntegerProperty shortBreakMinutes = new SimpleIntegerProperty(5);
    private final IntegerProperty longBreakMinutes = new SimpleIntegerProperty(15);
    private final IntegerProperty longBreakInterval = new SimpleIntegerProperty(4);
    private final BooleanProperty autoStartNext = new SimpleBooleanProperty(false);
    private final BooleanProperty autoStartBreaks = new SimpleBooleanProperty(false);
    private final BooleanProperty autoStartFocus = new SimpleBooleanProperty(false);
    private final BooleanProperty alwaysOnTop = new SimpleBooleanProperty(false);
    private final BooleanProperty pauseMusicOnFocus = new SimpleBooleanProperty(false);
    private final BooleanProperty switchToAmbientOnFocus = new SimpleBooleanProperty(false);
    private final BooleanProperty resumeMusicOnBreak = new SimpleBooleanProperty(false);
    private final StringProperty soundType = new SimpleStringProperty("bell");
    private final ObjectProperty<BreakMusicBehavior> breakMusicBehavior = new SimpleObjectProperty<>(BreakMusicBehavior.PAUSE);
    private final StringProperty sessionTypeLabel = new SimpleStringProperty("坐忘");

    private final IntegerProperty todayFocusMinutes = new SimpleIntegerProperty(0);
    private final IntegerProperty todayCompletedPomodoros = new SimpleIntegerProperty(0);
    private final IntegerProperty todayCompletedTasks = new SimpleIntegerProperty(0);
    private final IntegerProperty todayTotalTasks = new SimpleIntegerProperty(0);
    private final IntegerProperty streakDays = new SimpleIntegerProperty(0);

    private final IntegerProperty totalFocusMinutes = new SimpleIntegerProperty(0);
    private final IntegerProperty totalCompletedPomodoros = new SimpleIntegerProperty(0);
    private final IntegerProperty longestStreak = new SimpleIntegerProperty(0);
    private final IntegerProperty currentStreak = new SimpleIntegerProperty(0);

    private final ObservableList<PomodoroTask> tasks = FXCollections.observableArrayList();
    private final ObservableList<String> weeklyLabels = FXCollections.observableArrayList();
    private final ObservableList<Number> weeklyValues = FXCollections.observableArrayList();
    private final ObservableList<Number> monthlyHeatmap = FXCollections.observableArrayList();

    private java.util.Timer tickTimer;
    private LocalDateTime sessionStartTime;

    private static final String[] POETIC_HINTS = {
        "心如止水，万事可期",
        "静坐常思己过，闲谈莫论人非",
        "千里之行，始于足下",
        "不积跬步，无以至千里",
        "锲而不舍，金石可镂",
        "言忠信，行笃敬",
        "静以修身，俭以养德",
        "淡泊明志，宁静致远"
    };

    public PomodoroViewModel(AppContext appContext) {
        this.appContext = appContext;
        this.pomodoroService = appContext.getPomodoroService();
        this.asyncExecutor = appContext.getAsyncExecutor();
        this.userId = appContext.getUserSession().getUserId();
        currentSessionType.addListener((obs, old, newVal) -> sessionTypeLabel.set(getSessionLabel()));
        loadSettings();
        loadTodayStats();
        loadTotalStats();
        loadTasks();
        updatePoeticHint();
    }

    private void updatePoeticHint() {
        int idx = (int) (System.currentTimeMillis() / 60000) % POETIC_HINTS.length;
        poeticHint.set(POETIC_HINTS[idx]);
    }

    public void loadSettings() {
        PomodoroSettings s = pomodoroService.loadSettings();
        focusMinutes.set(s.getFocusMinutes());
        shortBreakMinutes.set(s.getShortBreakMinutes());
        longBreakMinutes.set(s.getLongBreakMinutes());
        longBreakInterval.set(s.getLongBreakInterval());
        autoStartNext.set(s.isAutoStartNext());
        alwaysOnTop.set(s.isAlwaysOnTop());
        soundType.set(s.getSoundType());
        breakMusicBehavior.set(s.getBreakMusicBehavior());
        resetTimer();
    }

    public void saveSettings() {
        PomodoroSettings s = new PomodoroSettings();
        s.setFocusMinutes(focusMinutes.get());
        s.setShortBreakMinutes(shortBreakMinutes.get());
        s.setLongBreakMinutes(longBreakMinutes.get());
        s.setLongBreakInterval(longBreakInterval.get());
        s.setAutoStartNext(autoStartNext.get());
        s.setAlwaysOnTop(alwaysOnTop.get());
        s.setSoundType(soundType.get());
        s.setBreakMusicBehavior(breakMusicBehavior.get());
        pomodoroService.saveSettings(s);
        if (!isRunning.get()) {
            resetTimer();
        }
    }

    public void loadTodayStats() {
        asyncExecutor.execute(() -> {
            DailyStats stats = pomodoroService.getDailyStats(userId);
            Platform.runLater(() -> {
                todayFocusMinutes.set(stats.getFocusMinutes());
                todayCompletedPomodoros.set(stats.getCompletedPomodoros());
                todayCompletedTasks.set(stats.getCompletedTasks());
                todayTotalTasks.set(stats.getTotalTasks());
                streakDays.set(stats.getStreakDays());
            });
        });
    }

    public void loadTotalStats() {
        asyncExecutor.execute(() -> {
            TotalStats stats = pomodoroService.getTotalStats(userId);
            Platform.runLater(() -> {
                totalFocusMinutes.set(stats.getTotalFocusMinutes());
                totalCompletedPomodoros.set(stats.getTotalCompletedPomodoros());
                longestStreak.set(stats.getLongestStreak());
                currentStreak.set(stats.getCurrentStreak());
            });
        });
    }

    public void loadTasks() {
        asyncExecutor.execute(() -> {
            List<PomodoroTask> taskList = pomodoroService.getTodayTasks(userId);
            Platform.runLater(() -> {
                tasks.setAll(taskList);
                if (activeTask.get() == null && !taskList.isEmpty()) {
                    for (PomodoroTask t : taskList) {
                        if (t.getIsCompleted() == 0) {
                            activeTask.set(t);
                            break;
                        }
                    }
                }
            });
        });
    }

    public void loadWeeklyStats() {
        asyncExecutor.execute(() -> {
            Map<String, Integer> data = pomodoroService.getWeeklyStats(userId);
            Platform.runLater(() -> {
                weeklyLabels.clear();
                weeklyValues.clear();
                String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
                LocalDate today = LocalDate.now();
                for (int i = 6; i >= 0; i--) {
                    LocalDate date = today.minusDays(i);
                    int dayIdx = (date.getDayOfWeek().getValue() - 1) % 7;
                    weeklyLabels.add(dayNames[dayIdx]);
                    weeklyValues.add(data.getOrDefault(date.toString(), 0));
                }
            });
        });
    }

    public void loadMonthlyStats() {
        asyncExecutor.execute(() -> {
            Map<String, Integer> data = pomodoroService.getMonthlyStats(userId);
            Platform.runLater(() -> {
                monthlyHeatmap.clear();
                LocalDate today = LocalDate.now();
                int daysInMonth = today.lengthOfMonth();
                for (int i = 1; i <= daysInMonth; i++) {
                    String date = today.withDayOfMonth(i).toString();
                    monthlyHeatmap.add(data.getOrDefault(date, 0));
                }
            });
        });
    }

    public void startTimer() {
        if (isRunning.get()) return;
        isRunning.set(true);
        sessionStartTime = LocalDateTime.now();
        tickTimer = new java.util.Timer(true);
        tickTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (remainingSeconds.get() > 0) {
                        remainingSeconds.set(remainingSeconds.get() - 1);
                    } else {
                        onSessionComplete();
                    }
                });
            }
        }, 1000, 1000);
    }

    public void pauseTimer() {
        isRunning.set(false);
        if (tickTimer != null) {
            tickTimer.cancel();
            tickTimer = null;
        }
    }

    public void resetTimer() {
        pauseTimer();
        int minutes;
        switch (currentSessionType.get()) {
            case FOCUS -> minutes = focusMinutes.get();
            case SHORT_BREAK -> minutes = shortBreakMinutes.get();
            case LONG_BREAK -> minutes = longBreakMinutes.get();
            default -> minutes = focusMinutes.get();
        }
        totalSeconds.set(minutes * 60);
        remainingSeconds.set(minutes * 60);
    }

    public void skipSession() {
        onSessionComplete();
    }

    private void onSessionComplete() {
        pauseTimer();
        boolean isFocus = currentSessionType.get() == SessionType.FOCUS;
        if (isFocus) {
            completedFocusRounds.set(completedFocusRounds.get() + 1);
            saveCompletedSession();
        }
        loadTodayStats();
        loadTotalStats();
        updatePoeticHint();
        advanceToNextSession(isFocus);
    }

    private void saveCompletedSession() {
        try {
            int duration = totalSeconds.get() / 60;
            String startStr = sessionStartTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String endStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String tag = activeTask.get() != null ? activeTask.get().getTag() : null;
            Integer taskId = activeTask.get() != null ? activeTask.get().getId() : null;
            pomodoroService.createSession(userId, taskId, duration,
                SessionType.FOCUS, startStr, endStr, true, tag);
            loadTasks();
        } catch (Exception e) {
            System.err.println("Failed to save session: " + e.getMessage());
        }
    }

    private void advanceToNextSession(boolean justFinishedFocus) {
        if (justFinishedFocus) {
            int interval = longBreakInterval.get();
            if (completedFocusRounds.get() > 0 && completedFocusRounds.get() % interval == 0) {
                currentSessionType.set(SessionType.LONG_BREAK);
            } else {
                currentSessionType.set(SessionType.SHORT_BREAK);
            }
        } else {
            currentSessionType.set(SessionType.FOCUS);
            currentRound.set(completedFocusRounds.get() + 1);
        }
        resetTimer();
        if (autoStartNext.get()) {
            startTimer();
        }
    }

    public void addTask(String title, int estimatedPomodoros, String tag) {
        asyncExecutor.execute(() -> {
            pomodoroService.createTask(userId, title, estimatedPomodoros, tag);
            Platform.runLater(this::loadTasks);
        });
    }

    public void toggleTaskComplete(PomodoroTask task) {
        asyncExecutor.execute(() -> {
            pomodoroService.toggleTaskComplete(task.getId());
            Platform.runLater(() -> {
                loadTasks();
                loadTodayStats();
            });
        });
    }

    public void deleteTask(PomodoroTask task) {
        asyncExecutor.execute(() -> {
            pomodoroService.deleteTask(task.getId());
            Platform.runLater(() -> {
                if (activeTask.get() != null && activeTask.get().getId() == task.getId()) {
                    activeTask.set(null);
                }
                loadTasks();
                loadTodayStats();
            });
        });
    }

    public void setActiveTask(PomodoroTask task) {
        activeTask.set(task);
    }

    public String formatTimeDisplay(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    public String formatDurationDisplay(int minutes) {
        if (minutes < 60) {
            return minutes + "分钟";
        }
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (mins == 0) {
            return hours + "小时";
        }
        return hours + "小时" + mins + "分";
    }

    public String getSessionLabel() {
        switch (currentSessionType.get()) {
            case FOCUS -> { return "坐忘"; }
            case SHORT_BREAK -> { return "小憩"; }
            case LONG_BREAK -> { return "闲庭"; }
        }
        return "坐忘";
    }

    public String getSessionStateText() {
        if (isRunning.get()) {
            return currentSessionType.get() == SessionType.FOCUS ? "专注中" : "休息中";
        }
        return "待开始";
    }

    public double getProgressPercent() {
        int total = totalSeconds.get();
        int remaining = remainingSeconds.get();
        if (total <= 0) return 0;
        return (total - remaining) * 100.0 / total;
    }

    public void cleanup() {
        pauseTimer();
    }

    public ObjectProperty<SessionType> currentSessionTypeProperty() { return currentSessionType; }
    public IntegerProperty remainingSecondsProperty() { return remainingSeconds; }
    public IntegerProperty totalSecondsProperty() { return totalSeconds; }
    public BooleanProperty isRunningProperty() { return isRunning; }
    public IntegerProperty currentRoundProperty() { return currentRound; }
    public IntegerProperty completedFocusRoundsProperty() { return completedFocusRounds; }
    public StringProperty poeticHintProperty() { return poeticHint; }
    public ObjectProperty<PomodoroTask> activeTaskProperty() { return activeTask; }
    public IntegerProperty focusMinutesProperty() { return focusMinutes; }
    public IntegerProperty shortBreakMinutesProperty() { return shortBreakMinutes; }
    public IntegerProperty longBreakMinutesProperty() { return longBreakMinutes; }
    public IntegerProperty longBreakIntervalProperty() { return longBreakInterval; }
    public BooleanProperty autoStartNextProperty() { return autoStartNext; }
    public BooleanProperty alwaysOnTopProperty() { return alwaysOnTop; }
    public StringProperty soundTypeProperty() { return soundType; }
    public ObjectProperty<BreakMusicBehavior> breakMusicBehaviorProperty() { return breakMusicBehavior; }
    public IntegerProperty todayFocusMinutesProperty() { return todayFocusMinutes; }
    public IntegerProperty todayCompletedPomodorosProperty() { return todayCompletedPomodoros; }
    public IntegerProperty todayCompletedTasksProperty() { return todayCompletedTasks; }
    public IntegerProperty todayTotalTasksProperty() { return todayTotalTasks; }
    public IntegerProperty streakDaysProperty() { return streakDays; }
    public IntegerProperty totalFocusMinutesProperty() { return totalFocusMinutes; }
    public IntegerProperty totalCompletedPomodorosProperty() { return totalCompletedPomodoros; }
    public IntegerProperty longestStreakProperty() { return longestStreak; }
    public IntegerProperty currentStreakProperty() { return currentStreak; }
    public ObservableList<PomodoroTask> getTasks() { return tasks; }
    public ObservableList<String> getWeeklyLabels() { return weeklyLabels; }
    public ObservableList<Number> getWeeklyValues() { return weeklyValues; }
    public ObservableList<Number> getMonthlyHeatmap() { return monthlyHeatmap; }

    public IntegerProperty currentRoundIndexProperty() { return currentRound; }
    public StringProperty sessionTypeProperty() { return sessionTypeLabel; }
    public String getSessionTypeLabel() { return getSessionLabel(); }
    public ObservableList<PomodoroTask> getTaskList() { return tasks; }
    public IntegerProperty focusDurationProperty() { return focusMinutes; }
    public IntegerProperty shortBreakDurationProperty() { return shortBreakMinutes; }
    public IntegerProperty longBreakDurationProperty() { return longBreakMinutes; }
    public BooleanProperty autoStartBreaksProperty() { return autoStartBreaks; }
    public BooleanProperty autoStartFocusProperty() { return autoStartFocus; }
    public BooleanProperty pauseMusicOnFocusProperty() { return pauseMusicOnFocus; }
    public BooleanProperty switchToAmbientOnFocusProperty() { return switchToAmbientOnFocus; }
    public BooleanProperty resumeMusicOnBreakProperty() { return resumeMusicOnBreak; }

    public void addTask(String title, String tag, int estimatedPomodoros) {
        addTask(title, estimatedPomodoros, tag);
    }
}
