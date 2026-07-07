package com.qiyunge.domain.model;

import com.qiyunge.domain.entity.User;

public class LoginResult {

    private final boolean success;
    private final User user;
    private final String message;
    private final boolean requirePasswordChange;
    private final FailureReason failureReason;

    public enum FailureReason {
        NONE, INVALID_CREDENTIALS, USER_DISABLED, USER_PENDING, USER_NOT_FOUND, SYSTEM_ERROR
    }

    private LoginResult(boolean success, User user, String message, boolean requirePasswordChange, FailureReason failureReason) {
        this.success = success;
        this.user = user;
        this.message = message;
        this.requirePasswordChange = requirePasswordChange;
        this.failureReason = failureReason;
    }

    public static LoginResult success(User user) {
        return new LoginResult(true, user, "登录成功", user.isMustChangePassword(), FailureReason.NONE);
    }

    public static LoginResult fail(String message, FailureReason reason) {
        return new LoginResult(false, null, message, false, reason);
    }

    public boolean isSuccess() { return success; }
    public User getUser() { return user; }
    public String getMessage() { return message; }
    public boolean isRequirePasswordChange() { return requirePasswordChange; }
    public FailureReason getFailureReason() { return failureReason; }
}
