package com.qiyunge.application.auth;

import com.qiyunge.application.face.FaceRecognitionService;
import com.qiyunge.application.service.AuditLogService;
import com.qiyunge.application.service.UserService;
import com.qiyunge.domain.entity.User;
import com.qiyunge.domain.entity.UserFaceData;
import com.qiyunge.domain.model.LoginResult;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.repository.RegistrationRequestRepository;
import com.qiyunge.infrastructure.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

/**
 * 认证服务：负责登录、改密、注册申请。
 * 不暴露 Repository，业务操作委托给 UserService / AuditLogService。
 * 内部保留 UserRepository 仅用于密码相关操作（属于认证域）。
 */
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final RegistrationRequestRepository registrationRequestRepository;
    private final DatabaseManager databaseManager;
    private FaceRecognitionService faceRecognitionService;

    public AuthService(DatabaseManager dbManager,
                       UserRepository userRepository,
                       AuditLogService auditLogService,
                       RegistrationRequestRepository registrationRequestRepository) {
        this.userService = new UserService(userRepository);
        this.userRepository = userRepository;
        this.databaseManager = dbManager;
        this.auditLogService = auditLogService;
        this.registrationRequestRepository = registrationRequestRepository;
    }

    public void setFaceRecognitionService(FaceRecognitionService faceRecognitionService) {
        this.faceRecognitionService = faceRecognitionService;
    }

    public LoginResult login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            return LoginResult.fail("请输入用户名", LoginResult.FailureReason.INVALID_CREDENTIALS);
        }
        if (password == null || password.trim().isEmpty()) {
            return LoginResult.fail("请输入密码", LoginResult.FailureReason.INVALID_CREDENTIALS);
        }

        String trimmedUsername = username.trim();
        var userOpt = userService.findByUsername(trimmedUsername);

        if (userOpt.isEmpty()) {
            auditLogService.logLoginFailed(trimmedUsername, "用户不存在");
            return LoginResult.fail("用户名或密码错误", LoginResult.FailureReason.USER_NOT_FOUND);
        }

        User user = userOpt.get();

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            auditLogService.logLoginFailed(trimmedUsername, "密码错误");
            return LoginResult.fail("用户名或密码错误", LoginResult.FailureReason.INVALID_CREDENTIALS);
        }

        if (user.isDisabled()) {
            auditLogService.logLoginFailed(trimmedUsername, "账号已被禁用");
            return LoginResult.fail("账号已被禁用，请联系管理员", LoginResult.FailureReason.USER_DISABLED);
        }

        if (user.isPending()) {
            auditLogService.logLoginFailed(trimmedUsername, "账号待审批");
            return LoginResult.fail("账号正在审核中，请耐心等待", LoginResult.FailureReason.USER_PENDING);
        }

        userService.updateLastLoginAt(user.getId());
        var refreshed = userService.findById(user.getId());
        User freshUser = refreshed.orElse(user);

        auditLogService.logLoginSuccess(freshUser);
        return LoginResult.success(freshUser);
    }

    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码至少6位");
        }

        var userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (!user.isMustChangePassword()) {
            if (oldPassword == null || !BCrypt.checkpw(oldPassword, user.getPasswordHash())) {
                return false;
            }
        }

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        boolean updated = userRepository.updatePassword(userId, newHash);

        if (updated) {
            auditLogService.logPasswordChange(user);
        }
        return updated;
    }

    /**
     * 人脸登录：根据识别到的用户ID进行认证。
     */
    public LoginResult loginByFace(int userId) {
        var userOpt = userService.findById(userId);

        if (userOpt.isEmpty()) {
            auditLogService.logLoginFailed("face_user_" + userId, "用户不存在");
            return LoginResult.fail("用户不存在", LoginResult.FailureReason.USER_NOT_FOUND);
        }

        User user = userOpt.get();

        if (user.isDisabled()) {
            auditLogService.logLoginFailed(user.getUsername(), "账号已被禁用");
            return LoginResult.fail("账号已被禁用，请联系管理员", LoginResult.FailureReason.USER_DISABLED);
        }

        if (user.isPending()) {
            auditLogService.logLoginFailed(user.getUsername(), "账号待审批");
            return LoginResult.fail("账号正在审核中，请耐心等待", LoginResult.FailureReason.USER_PENDING);
        }

        userService.updateLastLoginAt(user.getId());
        var refreshed = userService.findById(user.getId());
        User freshUser = refreshed.orElse(user);

        auditLogService.logLoginSuccess(freshUser);
        return LoginResult.success(freshUser);
    }

    public boolean hasFaceLoginEnabled(int userId) {
        return faceRecognitionService != null && faceRecognitionService.hasFaceData(userId);
    }

    public boolean verifyAndDeleteAccount(int userId, String password) {
        var userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();

        if (!BCrypt.checkpw(password, user.getPasswordHash())) {
            return false;
        }

        if (user.isAdmin()) {
            return false;
        }

        boolean deleted = userRepository.deleteUser(userId);
        if (deleted) {
            auditLogService.logAccountDeleted(user);
            if (faceRecognitionService != null) {
                faceRecognitionService.deleteFaceData(userId);
            }
        }
        return deleted;
    }

    public Optional<UserFaceData> getFaceData(int userId) {
        if (faceRecognitionService == null) return Optional.empty();
        return faceRecognitionService.getFaceData(userId);
    }

    public boolean registerRequest(String username, String password, String displayName, String reason) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("用户名至少3位");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("密码至少6位");
        }

        if (userService.findByUsername(username.trim()).isPresent()) {
            return false;
        }

        // Check if there's already a pending request with the same username
        try {
            boolean hasPending = databaseManager.withConnection(conn -> {
                String checkPendingSql = "SELECT COUNT(*) FROM registration_requests WHERE username = ? AND status = 'pending'";
                try (var stmt = conn.prepareStatement(checkPendingSql)) {
                    stmt.setString(1, username);
                    try (var rs = stmt.executeQuery()) {
                        return rs.next() && rs.getInt(1) > 0;
                    }
                }
            });
            if (hasPending) {
                return false;
            }
        } catch (Exception e) {
            // Ignore check failure, proceed with registration
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        var req = new com.qiyunge.domain.entity.RegistrationRequest();
        req.setUsername(username.trim());
        req.setPasswordHash(hash);
        req.setDisplayName(displayName);
        req.setReason(reason);

        try {
            registrationRequestRepository.create(req);
            return true;
        } catch (Exception e) {
            // UNIQUE constraint 兜底：虽然上方已通过 findByUsername 检查用户名，
            // 但并发场景下仍可能出现冲突，此处作为防御性兜底处理。
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                return false;
            }
            throw new RuntimeException("Failed to create registration request", e);
        }
    }
}
