package com.qiyunge.ui.dashboard;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.AdminService;
import com.qiyunge.application.service.AsyncExecutor;
import com.qiyunge.application.service.StatisticsService;
import javafx.application.Platform;
import javafx.beans.property.*;

public class DashboardViewModel {

    private final AppContext appContext;
    private final StatisticsService statisticsService;
    private final AdminService adminService;
    private final AsyncExecutor asyncExecutor;

    private final StringProperty greeting = new SimpleStringProperty("");
    private final StringProperty greetingSub = new SimpleStringProperty("");
    private final StringProperty welcomeBack = new SimpleStringProperty("");
    private final IntegerProperty pendingCount = new SimpleIntegerProperty(0);
    private final IntegerProperty totalUsers = new SimpleIntegerProperty(0);
    private final IntegerProperty totalSongs = new SimpleIntegerProperty(0);
    private final IntegerProperty totalImages = new SimpleIntegerProperty(0);
    private final IntegerProperty totalPlaylists = new SimpleIntegerProperty(0);
    private final IntegerProperty favoriteCount = new SimpleIntegerProperty(0);
    private final IntegerProperty playHistoryCount = new SimpleIntegerProperty(0);
    private final StringProperty cacheStatus = new SimpleStringProperty("--");
    private final BooleanProperty isAdmin = new SimpleBooleanProperty(false);
    private final BooleanProperty loading = new SimpleBooleanProperty(true);

    public DashboardViewModel(AppContext appContext) {
        this.appContext = appContext;
        this.statisticsService = appContext.getStatisticsService();
        this.adminService = appContext.getAdminService();
        this.asyncExecutor = appContext.getAsyncExecutor();
        this.isAdmin.set(appContext.getUserSession().isAdmin());
        computeGreeting();
        loadStatistics();
    }

    private void computeGreeting() {
        String displayName = appContext.getUserSession().getDisplayName();
        int hour = java.time.LocalTime.now().getHour();
        String timeGreeting;
        if (hour < 6) timeGreeting = "夜深了";
        else if (hour < 12) timeGreeting = "上午好";
        else if (hour < 14) timeGreeting = "中午好";
        else if (hour < 18) timeGreeting = "下午好";
        else timeGreeting = "晚上好";

        greeting.set(timeGreeting + "，" + displayName);
        welcomeBack.set("欢迎回来，" + displayName);

        String[] subs = {
            "云很轻，事情可以慢慢来。",
            "今天也是适合安静做事的一天。",
            "新的一天，从栖云阁开始。",
            "愿你今天一切顺遂。"
        };
        int idx = (int) (System.currentTimeMillis() / 86400000) % subs.length;
        greetingSub.set(subs[idx]);
    }

    public void loadStatistics() {
        loading.set(true);
        int userId = appContext.getUserSession().getUserId();

        asyncExecutor.execute(() -> {
            try {
                if (isAdmin.get()) {
                    int pending = adminService.countPendingRequests();
                    Platform.runLater(() -> pendingCount.set(pending));
                }

                int users = statisticsService.countActiveUsers();
                int songs = statisticsService.countSongs();
                int images = statisticsService.countGalleryImages();
                int playlists = statisticsService.countPlaylistsByUser(userId);
                int favs = statisticsService.countFavoritesByUser(userId);
                int history = statisticsService.countPlayHistoryByUser(userId);

                Platform.runLater(() -> {
                    totalUsers.set(users);
                    totalSongs.set(songs);
                    totalImages.set(images);
                    totalPlaylists.set(playlists);
                    favoriteCount.set(favs);
                    playHistoryCount.set(history);
                    cacheStatus.set("正常");
                });
            } catch (Exception e) {
                System.err.println("Failed to load dashboard statistics: " + e.getMessage());
            } finally {
                Platform.runLater(() -> loading.set(false));
            }
        });
    }

    // Properties
    public StringProperty greetingProperty() { return greeting; }
    public StringProperty greetingSubProperty() { return greetingSub; }
    public StringProperty welcomeBackProperty() { return welcomeBack; }
    public IntegerProperty pendingCountProperty() { return pendingCount; }
    public IntegerProperty totalUsersProperty() { return totalUsers; }
    public IntegerProperty totalSongsProperty() { return totalSongs; }
    public IntegerProperty totalImagesProperty() { return totalImages; }
    public IntegerProperty totalPlaylistsProperty() { return totalPlaylists; }
    public IntegerProperty favoriteCountProperty() { return favoriteCount; }
    public IntegerProperty playHistoryCountProperty() { return playHistoryCount; }
    public StringProperty cacheStatusProperty() { return cacheStatus; }
    public BooleanProperty isAdminProperty() { return isAdmin; }
    public BooleanProperty loadingProperty() { return loading; }
}
