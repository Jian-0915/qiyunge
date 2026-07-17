package com.qiyunge.app;

import com.qiyunge.domain.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class UserSession {

    private volatile int userId = -1;
    private volatile String username;
    private volatile String displayName;
    private volatile String role;
    private volatile String status;
    private volatile String avatarColor;
    private volatile LocalDateTime createdAt;
    private volatile boolean mustChangePassword;
    private volatile LocalDateTime loginTime;
    private volatile boolean loggedIn = false;

    private final List<Consumer<String>> displayNameListeners = new CopyOnWriteArrayList<>();

    public synchronized void login(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.displayName = user.getDisplayTitle();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.avatarColor = user.getAvatarColor();
        this.createdAt = user.getCreatedAt();
        this.mustChangePassword = user.isMustChangePassword();
        this.loginTime = LocalDateTime.now();
        this.loggedIn = true;
    }

    public synchronized void logout() {
        this.userId = -1;
        this.username = null;
        this.displayName = null;
        this.role = null;
        this.status = null;
        this.avatarColor = null;
        this.createdAt = null;
        this.mustChangePassword = false;
        this.loginTime = null;
        this.loggedIn = false;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getAvatarColor() { return avatarColor; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public boolean isLoggedIn() { return loggedIn; }
    public boolean isAdmin() { return loggedIn && "admin".equals(role); }
    public boolean isActive() { return loggedIn && "active".equals(status); }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        notifyDisplayNameChanged(displayName);
    }

    public void setAvatarColor(String avatarColor) { this.avatarColor = avatarColor; }

    public void addDisplayNameListener(Consumer<String> listener) {
        this.displayNameListeners.add(listener);
    }

    private void notifyDisplayNameChanged(String newName) {
        for (Consumer<String> listener : displayNameListeners) {
            listener.accept(newName);
        }
    }
}
