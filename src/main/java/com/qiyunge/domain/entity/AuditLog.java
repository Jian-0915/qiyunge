package com.qiyunge.domain.entity;

import java.time.LocalDateTime;

public class AuditLog {
    private int id;
    private Integer userId;
    private String action;
    private String targetType;
    private Integer targetId;
    private String detail;
    private String ipAddress;
    private LocalDateTime createdAt;

    public AuditLog() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Integer getTargetId() { return targetId; }
    public void setTargetId(Integer targetId) { this.targetId = targetId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getActionDisplay() {
        return switch (action) {
            case "LOGIN_SUCCESS" -> "登录成功";
            case "LOGIN_FAILED" -> "登录失败";
            case "LOGOUT" -> "退出登录";
            case "PASSWORD_CHANGE" -> "修改密码";
            case "ADMIN_INIT" -> "系统初始化";
            case "USER_DISABLED" -> "禁用用户";
            case "USER_ENABLED" -> "启用用户";
            case "PASSWORD_RESET" -> "重置密码";
            case "REQUEST_APPROVED" -> "审批通过";
            case "REQUEST_REJECTED" -> "审批拒绝";
            default -> action;
        };
    }
}
