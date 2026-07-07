package com.qiyunge.ui.login;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.auth.AuthService;
import com.qiyunge.application.face.FaceRecognitionService;
import com.qiyunge.application.service.AsyncExecutor;
import com.qiyunge.domain.model.LoginResult;
import javafx.application.Platform;
import javafx.beans.property.*;

import java.util.function.Consumer;

public class LoginViewModel {

    private final AuthService authService;
    private final AppContext appContext;
    private final AsyncExecutor asyncExecutor;

    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty loggingIn = new SimpleBooleanProperty(false);
    private final BooleanProperty hasError = new SimpleBooleanProperty(false);

    private Consumer<LoginResult> onLoginResult;
    private Runnable onNavigateToChangePassword;
    private Runnable onNavigateToMain;

    /** 注册结果回调：null 表示成功，非 null 为错误消息 */
    private Consumer<String> onRegisterResult;

    /** 人脸登录结果回调 */
    private Consumer<String> onFaceLoginResult;

    public LoginViewModel(AppContext appContext) {
        this.appContext = appContext;
        this.authService = appContext.getAuthService();
        this.asyncExecutor = appContext.getAsyncExecutor();
    }

    public void setOnLoginResult(Consumer<LoginResult> callback) {
        this.onLoginResult = callback;
    }

    public void setOnNavigateToChangePassword(Runnable callback) {
        this.onNavigateToChangePassword = callback;
    }

    public void setOnNavigateToMain(Runnable callback) {
        this.onNavigateToMain = callback;
    }

    public void setOnRegisterResult(Consumer<String> callback) {
        this.onRegisterResult = callback;
    }

    public void setOnFaceLoginResult(Consumer<String> callback) {
        this.onFaceLoginResult = callback;
    }

    /**
     * 注册方法：异步提交注册申请，通过回调返回结果。
     * @param username 用户名
     * @param password 密码
     * @param displayName 昵称
     * @param reason 申请理由
     */
    public void register(String username, String password, String displayName, String reason) {
        asyncExecutor.execute(() -> {
            try {
                boolean created = authService.registerRequest(username, password, displayName, reason);
                String message = created ? null : "此用户名已存在或已有待审批申请";
                if (onRegisterResult != null) {
                    Platform.runLater(() -> onRegisterResult.accept(message));
                }
            } catch (IllegalArgumentException ex) {
                if (onRegisterResult != null) {
                    Platform.runLater(() -> onRegisterResult.accept(ex.getMessage()));
                }
            } catch (Exception ex) {
                if (onRegisterResult != null) {
                    Platform.runLater(() -> onRegisterResult.accept("叩门笺递交失败，请稍后再试"));
                }
            }
        });
    }

    public void login() {
        if (loggingIn.get()) return;

        String u = username.get();
        String p = password.get();

        if (u == null || u.trim().isEmpty()) {
            showError("请输入用户名");
            return;
        }
        if (p == null || p.isEmpty()) {
            showError("请输入密码");
            return;
        }

        loggingIn.set(true);
        clearError();

        asyncExecutor.execute(() -> {
            try {
                LoginResult result = authService.login(u.trim(), p);

                Platform.runLater(() -> {
                    try {
                        if (result.isSuccess()) {
                            if (onLoginResult != null) {
                                onLoginResult.accept(result);
                            }
                            appContext.getUserSession().login(result.getUser());
                            if (result.isRequirePasswordChange()) {
                                if (onNavigateToChangePassword != null) {
                                    onNavigateToChangePassword.run();
                                }
                            } else {
                                if (onNavigateToMain != null) {
                                    onNavigateToMain.run();
                                }
                            }
                        } else {
                            showError(mapLoginError(result));
                            if (onLoginResult != null) {
                                onLoginResult.accept(result);
                            }
                        }
                    } finally {
                        loggingIn.set(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    try {
                        showError("登录异常，请稍后重试");
                    } finally {
                        loggingIn.set(false);
                    }
                });
            }
        });
    }

    private String mapLoginError(LoginResult result) {
        return switch (result.getFailureReason()) {
            case INVALID_CREDENTIALS, USER_NOT_FOUND -> "门未开，请确认用户名或密码";
            case USER_DISABLED -> "此账号暂不可入阁，请联系管理员";
            case USER_PENDING -> "入阁申请仍在审核中";
            default -> result.getMessage();
        };
    }

    private void showError(String message) {
        errorMessage.set(message);
        hasError.set(true);
    }

    private void clearError() {
        errorMessage.set("");
        hasError.set(false);
    }

    /**
     * 人脸登录：根据识别到的用户ID进行登录。
     */
    public void loginByFace(int userId) {
        if (loggingIn.get()) return;
        loggingIn.set(true);
        clearError();

        asyncExecutor.execute(() -> {
            try {
                LoginResult result = authService.loginByFace(userId);

                Platform.runLater(() -> {
                    try {
                        if (result.isSuccess()) {
                            if (onLoginResult != null) {
                                onLoginResult.accept(result);
                            }
                            appContext.getUserSession().login(result.getUser());
                            if (result.isRequirePasswordChange()) {
                                if (onNavigateToChangePassword != null) {
                                    onNavigateToChangePassword.run();
                                }
                            } else {
                                if (onNavigateToMain != null) {
                                    onNavigateToMain.run();
                                }
                            }
                        } else {
                            String message = mapLoginError(result);
                            if (onFaceLoginResult != null) {
                                onFaceLoginResult.accept(message);
                            }
                        }
                    } finally {
                        loggingIn.set(false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    try {
                        if (onFaceLoginResult != null) {
                            onFaceLoginResult.accept("人脸登录异常，请稍后重试");
                        }
                    } finally {
                        loggingIn.set(false);
                    }
                });
            }
        });
    }

    public StringProperty usernameProperty() { return username; }
    public StringProperty passwordProperty() { return password; }
    public StringProperty errorMessageProperty() { return errorMessage; }
    public BooleanProperty loggingInProperty() { return loggingIn; }
    public BooleanProperty hasErrorProperty() { return hasError; }
}
