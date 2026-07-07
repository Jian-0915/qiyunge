package com.qiyunge.ui.profile;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.AsyncExecutor;
import com.qiyunge.application.service.StatisticsService;
import com.qiyunge.application.service.UserService;
import com.qiyunge.infrastructure.util.DateTimeUtil;
import javafx.application.Platform;
import javafx.beans.property.*;

public class ProfileViewModel {

    private final AppContext appContext;
    private final UserService userService;
    private final StatisticsService statisticsService;
    private final AsyncExecutor asyncExecutor;

    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty displayName = new SimpleStringProperty("");
    private final StringProperty role = new SimpleStringProperty("");
    private final StringProperty status = new SimpleStringProperty("");
    private final StringProperty avatarColor = new SimpleStringProperty("#5B8DEF");
    private final StringProperty loginTime = new SimpleStringProperty("--");
    private final StringProperty createdAt = new SimpleStringProperty("--");
    private final IntegerProperty favoriteCount = new SimpleIntegerProperty(0);
    private final IntegerProperty playlistCount = new SimpleIntegerProperty(0);
    private final IntegerProperty imageCount = new SimpleIntegerProperty(0);
    private final IntegerProperty playCount = new SimpleIntegerProperty(0);
    private final StringProperty changePasswordError = new SimpleStringProperty("");
    private final BooleanProperty changingPassword = new SimpleBooleanProperty(false);
    private final BooleanProperty faceLoginEnabled = new SimpleBooleanProperty(false);
    private final BooleanProperty faceDataLoading = new SimpleBooleanProperty(false);

    private Runnable onProfileUpdated;
    private Runnable onAccountDeleted;

    public ProfileViewModel(AppContext appContext) {
        this.appContext = appContext;
        this.userService = appContext.getUserService();
        this.statisticsService = appContext.getStatisticsService();
        this.asyncExecutor = appContext.getAsyncExecutor();
        loadProfile();
    }

    public void setOnProfileUpdated(Runnable callback) {
        this.onProfileUpdated = callback;
    }

    public void setOnAccountDeleted(Runnable callback) {
        this.onAccountDeleted = callback;
    }

    private void loadProfile() {
        var session = appContext.getUserSession();
        username.set(session.getUsername());
        displayName.set(session.getDisplayName());
        role.set(session.isAdmin() ? "管理员" : "普通用户");
        status.set(session.isActive() ? "正常" : "异常");
        avatarColor.set(session.getAvatarColor() != null ? session.getAvatarColor() : "#5B8DEF");
        if (session.getLoginTime() != null) {
            loginTime.set(session.getLoginTime().format(DateTimeUtil.DT_FMT));
        }
        if (session.getCreatedAt() != null) {
            createdAt.set(session.getCreatedAt().format(DateTimeUtil.DT_FMT));
        }

        int userId = session.getUserId();
        loadFaceDataStatus(userId);

        asyncExecutor.execute(() -> {
            try {
                int favs = statisticsService.countFavoritesByUser(userId);
                int playlists = statisticsService.countPlaylistsByUser(userId);
                int images = statisticsService.countImagesByUser(userId);
                int plays = statisticsService.countPlayHistoryByUser(userId);

                Platform.runLater(() -> {
                    favoriteCount.set(favs);
                    playlistCount.set(playlists);
                    imageCount.set(images);
                    playCount.set(plays);
                });
            } catch (Exception e) {
                System.err.println("Failed to load profile stats: " + e.getMessage());
            }
        });
    }

    private void loadFaceDataStatus(int userId) {
        faceDataLoading.set(true);
        asyncExecutor.execute(() -> {
            try {
                boolean hasFace = appContext.getAuthService().hasFaceLoginEnabled(userId);
                Platform.runLater(() -> {
                    faceLoginEnabled.set(hasFace);
                    faceDataLoading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> faceDataLoading.set(false));
            }
        });
    }

    public void refreshFaceDataStatus() {
        loadFaceDataStatus(appContext.getUserSession().getUserId());
    }

    public boolean deleteFaceData() {
        int userId = appContext.getUserSession().getUserId();
        boolean ok = appContext.getFaceRecognitionService().deleteFaceData(userId);
        if (ok) {
            faceLoginEnabled.set(false);
        }
        return ok;
    }

    public boolean updateDisplayName(String newName) {
        if (newName == null || newName.trim().isEmpty()) return false;
        boolean ok = userService.updateDisplayName(
            appContext.getUserSession().getUserId(), newName.trim());
        if (ok) {
            appContext.getUserSession().setDisplayName(newName.trim());
            displayName.set(newName.trim());
            if (onProfileUpdated != null) onProfileUpdated.run();
        }
        return ok;
    }

    public boolean updateAvatarColor(String color) {
        if (color == null || color.trim().isEmpty()) return false;
        boolean ok = userService.updateAvatarColor(
            appContext.getUserSession().getUserId(), color.trim());
        if (ok) {
            appContext.getUserSession().setAvatarColor(color.trim());
            avatarColor.set(color.trim());
            if (onProfileUpdated != null) onProfileUpdated.run();
        }
        return ok;
    }

    public boolean changePassword(String oldPassword, String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            changePasswordError.set("新密码至少6位");
            return false;
        }
        if (!newPassword.equals(confirmPassword)) {
            changePasswordError.set("两次输入的密码不一致");
            return false;
        }
        changingPassword.set(true);
        changePasswordError.set("");

        boolean ok = appContext.getAuthService().changePassword(
            appContext.getUserSession().getUserId(), oldPassword, newPassword);

        changingPassword.set(false);
        if (!ok) {
            changePasswordError.set("修改失败，请检查当前密码");
        }
        return ok;
    }

    public boolean deleteAccount(String password) {
        int userId = appContext.getUserSession().getUserId();
        boolean ok = appContext.getAuthService().verifyAndDeleteAccount(userId, password);
        if (ok) {
            if (onAccountDeleted != null) {
                onAccountDeleted.run();
            }
        }
        return ok;
    }

    // Properties
    public StringProperty usernameProperty() { return username; }
    public StringProperty displayNameProperty() { return displayName; }
    public StringProperty roleProperty() { return role; }
    public StringProperty statusProperty() { return status; }
    public StringProperty avatarColorProperty() { return avatarColor; }
    public StringProperty loginTimeProperty() { return loginTime; }
    public StringProperty createdAtProperty() { return createdAt; }
    public IntegerProperty favoriteCountProperty() { return favoriteCount; }
    public IntegerProperty playlistCountProperty() { return playlistCount; }
    public IntegerProperty imageCountProperty() { return imageCount; }
    public IntegerProperty playCountProperty() { return playCount; }
    public StringProperty changePasswordErrorProperty() { return changePasswordError; }
    public BooleanProperty changingPasswordProperty() { return changingPassword; }
    public BooleanProperty faceLoginEnabledProperty() { return faceLoginEnabled; }
    public BooleanProperty faceDataLoadingProperty() { return faceDataLoading; }
}
