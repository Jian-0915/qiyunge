package com.qiyunge.application.service;

import com.qiyunge.domain.entity.AuditLog;
import com.qiyunge.domain.entity.User;
import com.qiyunge.infrastructure.repository.AuditLogRepository;

import java.util.List;

/**
 * 审计日志服务：集中写入和查询审计日志，管理员操作应统一通过此服务记录。
 */
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // ===== 写入方法 =====

    public void logLoginSuccess(User user) {
        auditLogRepository.logLoginSuccess(user);
    }

    public void logLoginFailed(String username, String reason) {
        auditLogRepository.logLoginFailed(username, reason);
    }

    public void logLogout(User user) {
        auditLogRepository.logLogout(user);
    }

    public void logPasswordChange(User user) {
        auditLogRepository.logPasswordChange(user);
    }

    public void logAccountDeleted(User user) {
        auditLogRepository.logAccountDeleted(user);
    }

    public void logAdminInit(String detail) {
        auditLogRepository.logAdminInit(detail);
    }

    public void logUserDisabled(int adminId, int targetUserId, String username) {
        auditLogRepository.log(adminId, "USER_DISABLED", "user", targetUserId,
            "管理员禁用用户: " + username);
    }

    public void logUserEnabled(int adminId, int targetUserId, String username) {
        auditLogRepository.log(adminId, "USER_ENABLED", "user", targetUserId,
            "管理员启用用户: " + username);
    }

    public void logPasswordReset(int adminId, int targetUserId, String username) {
        auditLogRepository.log(adminId, "PASSWORD_RESET", "user", targetUserId,
            "管理员重置密码: " + username);
    }

    public void logRequestApproved(int adminId, int requestId, String username) {
        auditLogRepository.log(adminId, "REQUEST_APPROVED", "registration_request", requestId,
            "管理员通过注册申请: " + username);
    }

    public void logRequestRejected(int adminId, int requestId, String username) {
        auditLogRepository.log(adminId, "REQUEST_REJECTED", "registration_request", requestId,
            "管理员拒绝注册申请: " + username);
    }

    // ===== 查询方法 =====

    public List<AuditLog> findRecent(int limit) {
        return auditLogRepository.findRecent(limit);
    }

    public List<AuditLog> findByUser(int userId, int limit) {
        return auditLogRepository.findByUser(userId, limit);
    }

    public List<AuditLog> findByAction(String action, int limit) {
        return auditLogRepository.findByAction(action, limit);
    }
}
