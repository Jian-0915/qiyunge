package com.qiyunge.application.service;

import com.qiyunge.domain.entity.RegistrationRequest;
import com.qiyunge.domain.entity.User;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.repository.RegistrationRequestRepository;
import com.qiyunge.infrastructure.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

/**
 * 后台管理服务：用户管理、注册审批、禁用/启用、重置密码。
 * 所有管理员操作均记录审计日志。
 */
public class AdminService {

    private final UserRepository userRepository;
    private final RegistrationRequestRepository registrationRequestRepository;
    private final AuditLogService auditLogService;
    private final DatabaseManager dbManager;

    public AdminService(UserRepository userRepository,
                        RegistrationRequestRepository registrationRequestRepository,
                        AuditLogService auditLogService,
                        DatabaseManager dbManager) {
        this.userRepository = userRepository;
        this.registrationRequestRepository = registrationRequestRepository;
        this.auditLogService = auditLogService;
        this.dbManager = dbManager;
    }

    // ===== 用户管理 =====

    public List<User> listAllUsers() {
        return userRepository.findAll();
    }

    public int countActiveUsers() {
        return userRepository.countActive();
    }

    /**
     * 禁用用户（软删除替代方案）。禁止自禁，禁止禁用最后一个管理员。
     */
    public boolean disableUser(int adminId, int userId) {
        if (adminId == userId) {
            throw new IllegalArgumentException("不能禁用当前登录管理员");
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        User targetUser = userOpt.get();
        if (targetUser.isAdmin() && userRepository.countActiveAdmins() <= 1) {
            throw new IllegalArgumentException("不能禁用最后一个管理员");
        }

        boolean ok = userRepository.updateStatus(userId, "disabled");
        if (ok) {
            auditLogService.logUserDisabled(adminId, userId, targetUser.getUsername());
        }
        return ok;
    }

    /**
     * 启用用户
     */
    public boolean enableUser(int adminId, int userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;
        boolean ok = userRepository.updateStatus(userId, "active");
        if (ok) {
            auditLogService.logUserEnabled(adminId, userId, userOpt.get().getUsername());
        }
        return ok;
    }

    /**
     * 管理员重置密码：强制用户下次登录修改。默认密码 123456。
     */
    public boolean resetPassword(int adminId, int userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("密码至少6位");
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        boolean ok = userRepository.resetPassword(userId, hash);
        if (ok) {
            auditLogService.logPasswordReset(adminId, userId, userOpt.get().getUsername());
        }
        return ok;
    }

    // ===== 注册审批 =====

    public List<RegistrationRequest> listPendingRequests() {
        return registrationRequestRepository.findPending();
    }

    public List<RegistrationRequest> listAllRequests() {
        return registrationRequestRepository.findAll();
    }

    public int countPendingRequests() {
        return registrationRequestRepository.countPending();
    }

    /**
     * 通过注册申请：创建正式用户 + 更新申请状态，在同一事务中完成。
     */
    public boolean approveRequest(int adminId, int requestId) {
        Optional<RegistrationRequest> reqOpt = registrationRequestRepository.findById(requestId);
        if (reqOpt.isEmpty()) return false;

        RegistrationRequest req = reqOpt.get();
        if (!req.isPending()) return false;

        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 事务：创建用户 + 更新申请状态
        dbManager.withTransaction((DatabaseManager.SqlConsumer<Connection>) conn -> {
            // 创建用户
            String insertUser = "INSERT INTO users (username, password_hash, email, role, status, display_name, avatar_color, must_change_password) " +
                               "VALUES (?, ?, NULL, 'user', 'active', ?, '#5B8DEF', 0)";
            try (PreparedStatement stmt = conn.prepareStatement(insertUser)) {
                stmt.setString(1, req.getUsername());
                stmt.setString(2, req.getPasswordHash());
                stmt.setString(3, req.getDisplayName() != null ? req.getDisplayName() : req.getUsername());
                stmt.executeUpdate();
            }

            // 更新申请状态
            String updateReq = "UPDATE registration_requests SET status = 'approved', reviewed_at = datetime('now', 'localtime'), reviewed_by = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(updateReq)) {
                stmt.setInt(1, adminId);
                stmt.setInt(2, requestId);
                stmt.executeUpdate();
            }
        });

        auditLogService.logRequestApproved(adminId, requestId, req.getUsername());
        return true;
    }

    /**
     * 拒绝注册申请
     */
    public boolean rejectRequest(int adminId, int requestId) {
        Optional<RegistrationRequest> reqOpt = registrationRequestRepository.findById(requestId);
        if (reqOpt.isEmpty()) return false;
        if (!reqOpt.get().isPending()) return false;

        boolean ok = registrationRequestRepository.reject(requestId, adminId);
        if (ok) {
            auditLogService.logRequestRejected(adminId, requestId, reqOpt.get().getUsername());
        }
        return ok;
    }
}
