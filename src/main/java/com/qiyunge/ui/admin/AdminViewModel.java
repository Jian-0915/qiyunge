package com.qiyunge.ui.admin;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.AdminService;
import com.qiyunge.application.service.AsyncExecutor;
import com.qiyunge.application.service.AuditLogService;
import com.qiyunge.domain.entity.AuditLog;
import com.qiyunge.domain.entity.RegistrationRequest;
import com.qiyunge.domain.entity.User;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.List;

/**
 * 阁务司后台管理 ViewModel：负责页面状态、数据加载、筛选逻辑。
 * 不直接操作 UI 控件。
 */
public class AdminViewModel {

    private final AppContext appContext;
    private final AdminService adminService;
    private final AuditLogService auditLogService;
    private final AsyncExecutor asyncExecutor;

    // 原始数据
    private final ObservableList<User> users = FXCollections.observableArrayList();
    private final ObservableList<RegistrationRequest> requests = FXCollections.observableArrayList();
    private final ObservableList<AuditLog> logs = FXCollections.observableArrayList();

    // 筛选后的用户列表
    private final FilteredList<User> filteredUsers;

    // 筛选条件
    private final StringProperty keyword = new SimpleStringProperty("");
    private final StringProperty statusFilter = new SimpleStringProperty("all"); // all, active, disabled

    // 状态
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");

    // 统计
    private final IntegerProperty totalUsers = new SimpleIntegerProperty(0);
    private final IntegerProperty activeUsers = new SimpleIntegerProperty(0);
    private final IntegerProperty disabledUsers = new SimpleIntegerProperty(0);
    private final IntegerProperty pendingRequests = new SimpleIntegerProperty(0);

    private Runnable onDataChanged;

    public AdminViewModel(AppContext appContext) {
        this.appContext = appContext;
        this.adminService = appContext.getAdminService();
        this.auditLogService = appContext.getAuditLogService();
        this.asyncExecutor = appContext.getAsyncExecutor();
        this.filteredUsers = new FilteredList<>(users, u -> true);

        // 监听筛选条件变化
        keyword.addListener((obs, old, val) -> applyFilters());
        statusFilter.addListener((obs, old, val) -> applyFilters());

        loadAll();
    }

    public void setOnDataChanged(Runnable callback) {
        this.onDataChanged = callback;
    }

    // ===== 数据加载 =====

    public void loadAll() {
        loadUsers();
        loadRequests();
        loadLogs();
        updateStatistics();
    }

    public void loadUsers() {
        asyncExecutor.execute(() -> {
            try {
                List<User> list = adminService.listAllUsers();
                Platform.runLater(() -> {
                    users.setAll(list);
                    applyFilters();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("加载用户列表失败: " + e.getMessage()));
            }
        });
    }

    public void loadRequests() {
        asyncExecutor.execute(() -> {
            try {
                List<RegistrationRequest> list = adminService.listPendingRequests();
                Platform.runLater(() -> requests.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("加载审批列表失败: " + e.getMessage()));
            }
        });
    }

    public void loadLogs() {
        asyncExecutor.execute(() -> {
            try {
                List<AuditLog> list = auditLogService.findRecent(50);
                Platform.runLater(() -> logs.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("加载日志失败: " + e.getMessage()));
            }
        });
    }

    public void updateStatistics() {
        asyncExecutor.execute(() -> {
            try {
                int total = adminService.listAllUsers().size();
                int active = adminService.countActiveUsers();
                int disabled = total - active;
                int pending = adminService.countPendingRequests();

                Platform.runLater(() -> {
                    totalUsers.set(total);
                    activeUsers.set(active);
                    disabledUsers.set(disabled);
                    pendingRequests.set(pending);
                });
            } catch (Exception e) {
                System.err.println("统计更新失败: " + e.getMessage());
            }
        });
    }

    // ===== 筛选逻辑 =====

    public void applyFilters() {
        String kw = keyword.get() != null ? keyword.get().trim().toLowerCase() : "";
        String status = statusFilter.get() != null ? statusFilter.get() : "all";

        filteredUsers.setPredicate(user -> {
            boolean matchesKeyword = kw.isEmpty()
                || user.getUsername().toLowerCase().contains(kw)
                || (user.getDisplayName() != null && user.getDisplayName().toLowerCase().contains(kw));

            boolean matchesStatus = switch (status) {
                case "active" -> "active".equals(user.getStatus());
                case "disabled" -> "disabled".equals(user.getStatus());
                default -> true;
            };

            return matchesKeyword && matchesStatus;
        });
    }

    // ===== 用户操作 =====

    public void disableUser(int userId) {
        int adminId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                boolean ok = adminService.disableUser(adminId, userId);
                Platform.runLater(() -> {
                    if (ok) {
                        loadAll();
                        if (onDataChanged != null) onDataChanged.run();
                    }
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> errorMessage.set(e.getMessage()));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("禁用用户失败: " + e.getMessage()));
            }
        });
    }

    public void enableUser(int userId) {
        int adminId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                boolean ok = adminService.enableUser(adminId, userId);
                Platform.runLater(() -> {
                    if (ok) {
                        loadAll();
                        if (onDataChanged != null) onDataChanged.run();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("启用用户失败: " + e.getMessage()));
            }
        });
    }

    public void resetPassword(int userId) {
        int adminId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                boolean ok = adminService.resetPassword(adminId, userId, "123456");
                Platform.runLater(() -> {
                    if (ok) {
                        loadAll();
                        if (onDataChanged != null) onDataChanged.run();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("重置密码失败: " + e.getMessage()));
            }
        });
    }

    // ===== 审批操作 =====

    public void approveRequest(int requestId) {
        int adminId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                boolean ok = adminService.approveRequest(adminId, requestId);
                Platform.runLater(() -> {
                    if (ok) {
                        loadAll();
                        if (onDataChanged != null) onDataChanged.run();
                    }
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> errorMessage.set(e.getMessage()));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("审批失败: " + e.getMessage()));
            }
        });
    }

    public void rejectRequest(int requestId) {
        int adminId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                boolean ok = adminService.rejectRequest(adminId, requestId);
                Platform.runLater(() -> {
                    if (ok) {
                        loadAll();
                        if (onDataChanged != null) onDataChanged.run();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("拒绝失败: " + e.getMessage()));
            }
        });
    }

    // ===== Properties / Getters =====

    public ObservableList<User> getFilteredUsers() { return filteredUsers; }
    public ObservableList<RegistrationRequest> getRequests() { return requests; }
    public ObservableList<AuditLog> getLogs() { return logs; }

    public StringProperty keywordProperty() { return keyword; }
    public StringProperty statusFilterProperty() { return statusFilter; }
    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorMessageProperty() { return errorMessage; }

    public IntegerProperty totalUsersProperty() { return totalUsers; }
    public IntegerProperty activeUsersProperty() { return activeUsers; }
    public IntegerProperty disabledUsersProperty() { return disabledUsers; }
    public IntegerProperty pendingRequestsProperty() { return pendingRequests; }
}
