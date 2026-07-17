package com.qiyunge.ui.login;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.face.FaceRecognitionService;
import com.qiyunge.ui.components.AppButton;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginView extends BorderPane {

    private static final double LOGIN_PANEL_BASE_Y = 18;

    private final AppContext appContext;
    private final LoginViewModel viewModel;

    private StackPane gateScene;
    private GateBackgroundPane gateBackground;
    private VBox loginPanel;
    private VBox registerPanel;
    private AppButton loginButton;
    private AppButton submitRegisterButton;
    private TextField usernameField;
    private PasswordField passwordField;
    private TextField registerUsernameField;
    private TextField registerDisplayNameField;
    private PasswordField registerPasswordField;
    private PasswordField registerConfirmPasswordField;
    private TextArea registerReasonArea;
    private Label errorLabel;
    private Label registerErrorLabel;
    private Label gateMessage;
    private double dragOffsetX;
    private double dragOffsetY;
    private boolean wasMaximized = false;

    // Face login panel elements
    private VBox passwordForm;
    private VBox faceForm;
    private Arc faceSpinner;
    private RotateTransition faceSpinnerAnim;
    private Label faceStatusLabel;
    private Label faceHintLabel;

    private boolean animating = false;
    private Runnable onLoginSuccess;
    private Runnable onNavigateToChangePassword;

    public LoginView(AppContext appContext) {
        this.appContext = appContext;
        this.viewModel = new LoginViewModel(appContext);
        this.getStyleClass().add("login-container");

        viewModel.setOnNavigateToMain(() -> {
            if (onLoginSuccess != null) onLoginSuccess.run();
        });
        viewModel.setOnNavigateToChangePassword(() -> {
            if (onNavigateToChangePassword != null) onNavigateToChangePassword.run();
        });
        viewModel.setOnRegisterResult(message -> {
            submitRegisterButton.setDisable(false);
            submitRegisterButton.setText("递交");
            if (message == null) {
                usernameField.setText(registerUsernameField.getText());
                clearRegisterForm();
                gateMessage.setText("叩门笺已递交，请等待阁务司审批");
                gateMessage.setOpacity(1);
                showLoginPanel();
            } else {
                showRegisterError(message);
            }
        });

        viewModel.setOnFaceLoginResult(message -> {
            stopFaceRecognition();
            animating = false;
            gateMessage.setText(message);
            gateMessage.setOpacity(1);
            loginButton.setText("入 阁");
            switchToPasswordForm(() -> playFailedRebuildAnimation());
        });

        // 无论成功失败都回调，用于动画
        viewModel.setOnLoginResult(result -> {
            if (!result.isSuccess()) {
                playFailedRebuildAnimation();
            }
        });

        gateScene = new StackPane();
        gateScene.getStyleClass().add("gate-scene");

        gateBackground = new GateBackgroundPane();
        loginPanel = createLoginPanel();
        registerPanel = createRegisterPanel();
        gateMessage = createGateMessage();
        HBox windowControls = createEmbeddedWindowControls();
        StackPane.setAlignment(loginPanel, Pos.CENTER);
        StackPane.setAlignment(registerPanel, Pos.CENTER);

        gateScene.getChildren().addAll(gateBackground, loginPanel, registerPanel, gateMessage, windowControls);
        VBox.setVgrow(gateScene, Priority.ALWAYS);

        this.setCenter(gateScene);
    }

    public void setOnLoginSuccess(Runnable callback) {
        this.onLoginSuccess = callback;
    }

    public void setOnNavigateToChangePassword(Runnable callback) {
        this.onNavigateToChangePassword = callback;
    }

    // ===== 登录面板：包含密码表单 + 人脸识别面板 =====

    private VBox createLoginPanel() {
        VBox panel = new VBox(0);
        panel.setAlignment(Pos.CENTER);
        panel.setMinWidth(300);
        panel.setPrefWidth(300);
        panel.setMaxWidth(300);
        panel.setMinHeight(Region.USE_PREF_SIZE);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setPadding(new Insets(22, 24, 20, 24));
        panel.getStyleClass().add("gate-login-panel");
        panel.setTranslateY(LOGIN_PANEL_BASE_Y);

        // 使用 StackPane 叠放两个表单
        StackPane formStack = new StackPane();
        formStack.setAlignment(Pos.CENTER);

        passwordForm = createPasswordForm();
        faceForm = createFaceForm();
        faceForm.setVisible(false);
        faceForm.setManaged(false);
        faceForm.setOpacity(0);
        faceForm.setScaleX(0.96);
        faceForm.setScaleY(0.96);

        formStack.getChildren().addAll(passwordForm, faceForm);
        panel.getChildren().add(formStack);

        return panel;
    }

    // 密码登录表单
    private VBox createPasswordForm() {
        VBox form = new VBox(12);
        form.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("入阁");
        titleLabel.getStyleClass().add("gate-title");

        Label subtitleLabel = new Label("云上有阁，归处有序");
        subtitleLabel.getStyleClass().add("gate-subtitle");

        Region spacer1 = new Region();
        spacer1.setPrefHeight(4);

        Label userLabel = new Label("用户名");
        userLabel.getStyleClass().add("gate-field-label");

        usernameField = new TextField();
        usernameField.setPromptText("请输入用户名");
        usernameField.setPrefHeight(38);
        usernameField.getStyleClass().add("gate-input");
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());

        Label passLabel = new Label("密码");
        passLabel.getStyleClass().add("gate-field-label");

        passwordField = new PasswordField();
        passwordField.setPromptText("请输入密码");
        passwordField.setPrefHeight(38);
        passwordField.getStyleClass().add("gate-input");
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());

        errorLabel = new Label();
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.getStyleClass().add("gate-error");
        errorLabel.visibleProperty().bind(viewModel.hasErrorProperty());
        errorLabel.managedProperty().bind(viewModel.hasErrorProperty());

        loginButton = new AppButton("入 阁", AppButton.Style.PRIMARY);
        loginButton.setPrefWidth(Double.MAX_VALUE);
        loginButton.setPrefHeight(40);
        loginButton.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> {
            if (passwordField.getText().isEmpty()) {
                passwordField.requestFocus();
            } else {
                handleLogin();
            }
        });

        Hyperlink registerLink = new Hyperlink("申请入阁");
        registerLink.getStyleClass().add("gate-link");
        registerLink.setOnAction(e -> showRegisterPanel());

        Hyperlink faceLoginLink = new Hyperlink("刷脸入阁");
        faceLoginLink.getStyleClass().add("gate-link");
        faceLoginLink.setOnAction(e -> handleFaceLogin());

        HBox linksRow = new HBox(16, faceLoginLink, registerLink);
        linksRow.setAlignment(Pos.CENTER);

        form.getChildren().addAll(
            titleLabel, subtitleLabel, spacer1,
            userLabel, usernameField,
            passLabel, passwordField,
            errorLabel,
            loginButton,
            linksRow
        );
        return form;
    }

    // 人脸识别表单
    private VBox createFaceForm() {
        VBox form = new VBox(16);
        form.setAlignment(Pos.CENTER);
        form.setPadding(new Insets(8, 0, 8, 0));

        Label titleLabel = new Label("刷脸入阁");
        titleLabel.getStyleClass().add("gate-title");

        Label subtitleLabel = new Label("请将面部对准摄像头");
        subtitleLabel.getStyleClass().add("gate-subtitle");

        // 旋转加载动画
        faceSpinner = new Arc(0, 0, 24, 24, 0, 270);
        faceSpinner.setType(ArcType.OPEN);
        faceSpinner.setStroke(Color.web("#5B8DEF"));
        faceSpinner.setStrokeWidth(3);
        faceSpinner.setFill(null);

        faceSpinnerAnim = new RotateTransition(Duration.millis(800), faceSpinner);
        faceSpinnerAnim.setByAngle(360);
        faceSpinnerAnim.setCycleCount(Animation.INDEFINITE);
        faceSpinnerAnim.setInterpolator(Interpolator.LINEAR);

        faceStatusLabel = new Label("正在启动摄像头...");
        faceStatusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #5B8DEF;");

        faceHintLabel = new Label("");
        faceHintLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #999;");

        Hyperlink backLink = new Hyperlink("返回密码登录");
        backLink.getStyleClass().add("gate-link");
        backLink.setOnAction(e -> {
            stopFaceRecognition();
            switchToPasswordForm(null);
        });

        form.getChildren().addAll(
            titleLabel, subtitleLabel, faceSpinner,
            faceStatusLabel, faceHintLabel, backLink
        );
        return form;
    }

    // ===== 面板切换动画 =====

    private void switchToFaceForm(Runnable afterSwitch) {
        if (animating) return;
        animating = true;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), passwordForm);
        fadeOut.setToValue(0);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), passwordForm);
        scaleOut.setToX(0.96);
        scaleOut.setToY(0.96);

        ParallelTransition out = new ParallelTransition(fadeOut, scaleOut);
        out.setOnFinished(e -> {
            passwordForm.setVisible(false);
            passwordForm.setManaged(false);

            faceForm.setVisible(true);
            faceForm.setManaged(true);
            faceForm.setOpacity(0);
            faceForm.setScaleX(0.96);
            faceForm.setScaleY(0.96);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), faceForm);
            fadeIn.setToValue(1);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), faceForm);
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            ParallelTransition in = new ParallelTransition(fadeIn, scaleIn);
            in.setOnFinished(ev -> {
                animating = false;
                if (afterSwitch != null) afterSwitch.run();
            });
            in.play();
        });
        out.play();
    }

    private void switchToPasswordForm(Runnable afterSwitch) {
        if (animating) return;
        animating = true;

        FadeTransition fadeOut = new FadeTransition(Duration.millis(180), faceForm);
        fadeOut.setToValue(0);
        ScaleTransition scaleOut = new ScaleTransition(Duration.millis(180), faceForm);
        scaleOut.setToX(0.96);
        scaleOut.setToY(0.96);

        ParallelTransition out = new ParallelTransition(fadeOut, scaleOut);
        out.setOnFinished(e -> {
            faceForm.setVisible(false);
            faceForm.setManaged(false);
            stopFaceRecognition();

            passwordForm.setVisible(true);
            passwordForm.setManaged(true);
            passwordForm.setOpacity(0);
            passwordForm.setScaleX(0.96);
            passwordForm.setScaleY(0.96);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), passwordForm);
            fadeIn.setToValue(1);
            ScaleTransition scaleIn = new ScaleTransition(Duration.millis(200), passwordForm);
            scaleIn.setToX(1);
            scaleIn.setToY(1);

            ParallelTransition in = new ParallelTransition(fadeIn, scaleIn);
            in.setOnFinished(ev -> {
                animating = false;
                if (afterSwitch != null) afterSwitch.run();
            });
            in.play();
        });
        out.play();
    }

    // ===== 人脸登录处理 =====

    private void handleFaceLogin() {
        if (animating || viewModel.loggingInProperty().get()) return;

        var faceService = appContext.getFaceRecognitionService();
        if (faceService == null) {
            gateMessage.setText("人脸识别服务未初始化");
            gateMessage.setOpacity(1);
            return;
        }

        switchToFaceForm(() -> {
            faceStatusLabel.setText("正在打开摄像头...");
            faceStatusLabel.setTextFill(Color.web("#5B8DEF"));
            faceHintLabel.setText("");
            faceSpinnerAnim.play();

            // 检查摄像头是否已复用（之前未关闭）
            if (faceService.isCameraOpen()) {
                faceStatusLabel.setText("正在识别...");
                faceRecognizing = true;
                faceService.startRecognitionLoop(
                    this::onFaceRecognitionResult,
                    null
                );
                return;
            }

            // 在后台线程打开摄像头，避免阻塞 UI
            appContext.getAsyncExecutor().execute(() -> {
                boolean opened = faceService.openCamera();
                Platform.runLater(() -> {
                    if (!faceRecognizing && !faceForm.isVisible()) return; // 用户已返回

                    if (!opened) {
                        faceSpinnerAnim.stop();
                        faceStatusLabel.setText("无法打开摄像头");
                        faceStatusLabel.setTextFill(Color.web("#E74C3C"));
                        faceHintLabel.setText("请检查设备后返回密码登录");
                        return;
                    }

                    faceStatusLabel.setText("正在识别...");
                    faceRecognizing = true;

                    faceService.startRecognitionLoop(
                        this::onFaceRecognitionResult,
                        null
                    );
                });
            });
        });
    }

    private void onFaceRecognitionResult(FaceRecognitionService.RecognitionResult result) {
        Platform.runLater(() -> {
            switch (result.status) {
                case SUCCESS -> {
                    // 立即停止识别循环和关闭摄像头
                    stopFaceRecognition();
                    faceSpinnerAnim.stop();
                    faceStatusLabel.setText("识别成功，请稍候...");
                    faceStatusLabel.setTextFill(Color.web("#27AE60"));
                    faceHintLabel.setText("");
                    viewModel.loginByFace(result.userId);
                }
                case NO_FACE -> {
                    faceHintLabel.setText("请将面部对准摄像头");
                    faceHintLabel.setTextFill(Color.web("#999"));
                }
                case NO_MODEL -> {
                    // 无模型，停止识别
                    stopFaceRecognition();
                    faceStatusLabel.setText("暂无人脸数据");
                    faceStatusLabel.setTextFill(Color.web("#E67E22"));
                    faceHintLabel.setText("请在个人中心「吾庐」录入人脸");
                }
                case UNKNOWN -> {
                    faceHintLabel.setText("识别中...");
                    faceHintLabel.setTextFill(Color.web("#999"));
                }
                case NO_CAMERA -> {
                    stopFaceRecognition();
                    faceStatusLabel.setText("摄像头异常");
                    faceStatusLabel.setTextFill(Color.web("#E74C3C"));
                }
            }
        });
    }

    private volatile boolean faceRecognizing = false;

    /** 停止人脸识别循环和动画，但保留摄像头连接（下次切换回来可复用）。 */
    private void stopFaceRecognition() {
        if (!faceRecognizing) return;
        faceRecognizing = false;
        if (faceSpinnerAnim != null) faceSpinnerAnim.stop();
        var faceService = appContext.getFaceRecognitionService();
        if (faceService != null) {
            faceService.stopRecognitionLoop();
            // 不关闭摄像头，保留连接以便快速复用
        }
    }

    /** 真正关闭摄像头，在退出登录时调用。 */
    public void closeFaceCamera() {
        stopFaceRecognition();
        var faceService = appContext.getFaceRecognitionService();
        if (faceService != null) {
            faceService.closeCamera();
        }
    }

    // ===== 注册面板 =====

    private VBox createRegisterPanel() {
        VBox panel = new VBox(10);
        panel.setAlignment(Pos.CENTER);
        panel.setMinWidth(320);
        panel.setPrefWidth(320);
        panel.setMaxWidth(320);
        panel.setMinHeight(Region.USE_PREF_SIZE);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setPadding(new Insets(20, 24, 18, 24));
        panel.getStyleClass().add("gate-login-panel");
        panel.setTranslateY(LOGIN_PANEL_BASE_Y);
        panel.setOpacity(0);
        panel.setScaleX(0.96);
        panel.setScaleY(0.96);
        panel.setVisible(false);
        panel.setManaged(false);

        Label titleLabel = new Label("叩门笺");
        titleLabel.getStyleClass().add("gate-title");

        Label subtitleLabel = new Label("云上有阁，归处有序");
        subtitleLabel.getStyleClass().add("gate-subtitle");

        registerUsernameField = createRegisterTextField("请输入用户名");
        registerDisplayNameField = createRegisterTextField("请输入昵称");

        registerPasswordField = new PasswordField();
        registerPasswordField.setPromptText("请输入密码");
        registerPasswordField.setPrefHeight(36);
        registerPasswordField.getStyleClass().add("gate-input");

        registerConfirmPasswordField = new PasswordField();
        registerConfirmPasswordField.setPromptText("请再次输入密码");
        registerConfirmPasswordField.setPrefHeight(36);
        registerConfirmPasswordField.getStyleClass().add("gate-input");

        registerReasonArea = new TextArea();
        registerReasonArea.setPromptText("写下申请理由");
        registerReasonArea.setPrefRowCount(3);
        registerReasonArea.setWrapText(true);
        registerReasonArea.getStyleClass().add("gate-input");

        registerErrorLabel = new Label();
        registerErrorLabel.getStyleClass().add("gate-error");
        registerErrorLabel.setMinHeight(18);
        registerErrorLabel.setVisible(false);
        registerErrorLabel.setManaged(false);

        AppButton backButton = new AppButton("返回", AppButton.Style.GHOST);
        backButton.setPrefWidth(96);
        backButton.setOnAction(e -> showLoginPanel());

        submitRegisterButton = new AppButton("递交", AppButton.Style.PRIMARY);
        submitRegisterButton.setPrefWidth(140);
        submitRegisterButton.setOnAction(e -> submitRegisterRequest());

        HBox actions = new HBox(10, backButton, submitRegisterButton);
        actions.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(
            titleLabel, subtitleLabel,
            createFieldLabel("用户名"), registerUsernameField,
            createFieldLabel("昵称"), registerDisplayNameField,
            createFieldLabel("密码"), registerPasswordField,
            createFieldLabel("确认密码"), registerConfirmPasswordField,
            createFieldLabel("申请理由"), registerReasonArea,
            registerErrorLabel,
            actions
        );

        return panel;
    }

    private TextField createRegisterTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefHeight(36);
        field.getStyleClass().add("gate-input");
        return field;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("gate-field-label");
        return label;
    }

    private Label createGateMessage() {
        Label msg = new Label();
        msg.getStyleClass().add("gate-message");
        msg.setOpacity(0);
        StackPane.setAlignment(msg, Pos.BOTTOM_CENTER);
        msg.setPadding(new Insets(0, 0, 40, 0));
        return msg;
    }

    private HBox createEmbeddedWindowControls() {
        HBox bar = new HBox(8);
        bar.getStyleClass().add("gate-window-controls");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 12, 0, 14));
        bar.setPickOnBounds(false);
        StackPane.setAlignment(bar, Pos.TOP_CENTER);

        Label title = new Label("栖云阁");
        title.getStyleClass().add("gate-window-title");

        Region dragArea = new Region();
        HBox.setHgrow(dragArea, Priority.ALWAYS);
        dragArea.setMinHeight(32);
        dragArea.setOnMousePressed(this::onWindowDragPressed);
        dragArea.setOnMouseDragged(this::onWindowDragged);

        Button minimizeBtn = createWindowButton("\u2212");
        minimizeBtn.setOnAction(e -> appContext.getPrimaryStage().setIconified(true));

        Button closeBtn = createWindowButton("\u00D7");
        closeBtn.getStyleClass().add("gate-window-close");
        closeBtn.setOnAction(e -> appContext.getPrimaryStage().close());

        bar.getChildren().addAll(title, dragArea, minimizeBtn, closeBtn);
        return bar;
    }

    private Button createWindowButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("gate-window-button");
        button.setFocusTraversable(false);
        return button;
    }

    // ===== 面板显示切换 =====

    private void showRegisterPanel() {
        if (animating || viewModel.loggingInProperty().get()) return;

        clearRegisterError();
        resetLoginPanelTransform();
        registerPanel.setManaged(true);
        registerPanel.setVisible(true);
        registerPanel.setOpacity(0);
        registerPanel.setScaleX(0.96);
        registerPanel.setScaleY(0.96);
        registerPanel.setTranslateY(LOGIN_PANEL_BASE_Y + 8);

        FadeTransition loginFade = new FadeTransition(Duration.millis(160), loginPanel);
        loginFade.setToValue(0);

        FadeTransition registerFade = new FadeTransition(Duration.millis(220), registerPanel);
        registerFade.setToValue(1);
        ScaleTransition registerScale = new ScaleTransition(Duration.millis(220), registerPanel);
        registerScale.setToX(1);
        registerScale.setToY(1);
        TranslateTransition registerMove = new TranslateTransition(Duration.millis(220), registerPanel);
        registerMove.setToY(LOGIN_PANEL_BASE_Y);

        ParallelTransition transition = new ParallelTransition(loginFade, registerFade, registerScale, registerMove);
        transition.setOnFinished(e -> {
            loginPanel.setVisible(false);
            loginPanel.setManaged(false);
            registerUsernameField.requestFocus();
        });
        transition.play();
    }

    private void showLoginPanel() {
        clearRegisterError();
        loginPanel.setManaged(true);
        loginPanel.setVisible(true);
        loginPanel.setOpacity(0);
        loginPanel.setScaleX(0.96);
        loginPanel.setScaleY(0.96);
        loginPanel.setTranslateY(LOGIN_PANEL_BASE_Y + 8);

        FadeTransition registerFade = new FadeTransition(Duration.millis(160), registerPanel);
        registerFade.setToValue(0);

        FadeTransition loginFade = new FadeTransition(Duration.millis(220), loginPanel);
        loginFade.setToValue(1);
        ScaleTransition loginScale = new ScaleTransition(Duration.millis(220), loginPanel);
        loginScale.setToX(1);
        loginScale.setToY(1);
        TranslateTransition loginMove = new TranslateTransition(Duration.millis(220), loginPanel);
        loginMove.setToY(LOGIN_PANEL_BASE_Y);

        ParallelTransition transition = new ParallelTransition(registerFade, loginFade, loginScale, loginMove);
        transition.setOnFinished(e -> {
            registerPanel.setVisible(false);
            registerPanel.setManaged(false);
            passwordForm.setVisible(true);
            passwordForm.setManaged(true);
            passwordForm.setOpacity(1);
            faceForm.setVisible(false);
            faceForm.setManaged(false);
            passwordField.requestFocus();
        });
        transition.play();
    }

    private void submitRegisterRequest() {
        String username = registerUsernameField.getText() == null ? "" : registerUsernameField.getText().trim();
        String displayName = registerDisplayNameField.getText() == null ? "" : registerDisplayNameField.getText().trim();
        String password = registerPasswordField.getText() == null ? "" : registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText() == null ? "" : registerConfirmPasswordField.getText();
        String reason = registerReasonArea.getText() == null ? "" : registerReasonArea.getText().trim();

        if (username.length() < 3) {
            showRegisterError("用户名至少 3 位");
            return;
        }
        if (password.length() < 6) {
            showRegisterError("密码至少 6 位");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showRegisterError("两次输入的密码不一致");
            return;
        }

        submitRegisterButton.setDisable(true);
        submitRegisterButton.setText("递交中...");
        clearRegisterError();

        String finalDisplayName = displayName.isBlank() ? username : displayName;
        viewModel.register(username, password, finalDisplayName, reason);
    }

    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
        registerErrorLabel.setManaged(true);
    }

    private void clearRegisterError() {
        registerErrorLabel.setText("");
        registerErrorLabel.setVisible(false);
        registerErrorLabel.setManaged(false);
    }

    private void clearRegisterForm() {
        registerUsernameField.clear();
        registerDisplayNameField.clear();
        registerPasswordField.clear();
        registerConfirmPasswordField.clear();
        registerReasonArea.clear();
    }

    private void onWindowDragPressed(MouseEvent event) {
        Stage stage = appContext.getPrimaryStage();
        wasMaximized = stage.isMaximized();
        dragOffsetX = event.getScreenX() - stage.getX();
        dragOffsetY = event.getScreenY() - stage.getY();
    }

    private void onWindowDragged(MouseEvent event) {
        Stage stage = appContext.getPrimaryStage();
        if (wasMaximized) {
            stage.setMaximized(false);
            // Center the window on the cursor position
            double newX = event.getScreenX() - stage.getWidth() / 2;
            double newY = event.getScreenY() - dragOffsetY;
            stage.setX(newX);
            stage.setY(newY);
            wasMaximized = false;
            return;
        }
        stage.setX(event.getScreenX() - dragOffsetX);
        stage.setY(event.getScreenY() - dragOffsetY);
    }

    // ===== 登录处理 =====

    private void handleLogin() {
        if (animating || viewModel.loggingInProperty().get()) return;

        if (usernameField.getText().isBlank() || passwordField.getText().isBlank()) {
            resetLoginPanelTransform();
            viewModel.login();
            return;
        }

        animating = true;
        resetLoginPanelTransform();
        playKnockAndFoldForm(() -> viewModel.login());
    }

    private void resetLoginPanelTransform() {
        loginPanel.setOpacity(1);
        loginPanel.setScaleX(1);
        loginPanel.setScaleY(1);
        loginPanel.setTranslateX(0);
        loginPanel.setTranslateY(LOGIN_PANEL_BASE_Y);
        loginButton.setScaleX(1);
        loginButton.setScaleY(1);
        gateBackground.setTranslateX(0);
        gateMessage.setOpacity(0);
        loginButton.setText("入 阁");
    }

    // ===== 叩门 + 表单收起动画 =====

    private void playKnockAndFoldForm(Runnable afterFold) {
        loginButton.setText("叩门中...");

        // 按钮叩门 scale
        Timeline knock = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(loginButton.scaleXProperty(), 1.0),
                new KeyValue(loginButton.scaleYProperty(), 1.0)),
            new KeyFrame(Duration.millis(70),
                new KeyValue(loginButton.scaleXProperty(), 0.97),
                new KeyValue(loginButton.scaleYProperty(), 0.97)),
            new KeyFrame(Duration.millis(190),
                new KeyValue(loginButton.scaleXProperty(), 1.0),
                new KeyValue(loginButton.scaleYProperty(), 1.0))
        );

        Timeline ringKnock = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(gateBackground.getLeftRing().rotateProperty(), 0),
                new KeyValue(gateBackground.getRightRing().rotateProperty(), 0),
                new KeyValue(gateBackground.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(70),
                new KeyValue(gateBackground.getLeftRing().rotateProperty(), -8),
                new KeyValue(gateBackground.getRightRing().rotateProperty(), 8),
                new KeyValue(gateBackground.translateXProperty(), -2)),
            new KeyFrame(Duration.millis(150),
                new KeyValue(gateBackground.getLeftRing().rotateProperty(), 6),
                new KeyValue(gateBackground.getRightRing().rotateProperty(), -6),
                new KeyValue(gateBackground.translateXProperty(), 2)),
            new KeyFrame(Duration.millis(260),
                new KeyValue(gateBackground.getLeftRing().rotateProperty(), 0),
                new KeyValue(gateBackground.getRightRing().rotateProperty(), 0),
                new KeyValue(gateBackground.translateXProperty(), 0))
        );

        // 表单收起
        FadeTransition fade = new FadeTransition(Duration.millis(220), loginPanel);
        fade.setFromValue(1);
        fade.setToValue(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(220), loginPanel);
        scale.setFromX(1);
        scale.setFromY(1);
        scale.setToX(0.92);
        scale.setToY(0.92);

        TranslateTransition move = new TranslateTransition(Duration.millis(220), loginPanel);
        move.setFromY(LOGIN_PANEL_BASE_Y);
        move.setToY(LOGIN_PANEL_BASE_Y - 12);

        ParallelTransition fold = new ParallelTransition(fade, scale, move);

        // 显示"叩门中..."
        Timeline showMsg = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(gateMessage.opacityProperty(), 0),
                new KeyValue(gateMessage.textProperty(), "叩门中...")),
            new KeyFrame(Duration.millis(200),
                new KeyValue(gateMessage.opacityProperty(), 1))
        );

        SequentialTransition seq = new SequentialTransition(new ParallelTransition(knock, ringKnock), fold, showMsg);
        seq.setOnFinished(e -> afterFold.run());
        seq.play();
    }

    // ===== 失败重组动画 =====

    private void playFailedRebuildAnimation() {
        // 门震动
        Timeline doorShake = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(gateBackground.translateXProperty(), 0)),
            new KeyFrame(Duration.millis(50), new KeyValue(gateBackground.translateXProperty(), -6)),
            new KeyFrame(Duration.millis(100), new KeyValue(gateBackground.translateXProperty(), 6)),
            new KeyFrame(Duration.millis(160), new KeyValue(gateBackground.translateXProperty(), -3)),
            new KeyFrame(Duration.millis(220), new KeyValue(gateBackground.translateXProperty(), 0))
        );

        // 门缝金光闪一下
        Timeline lightFlash = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(gateBackground.centerLightWidthProperty(), 2),
                new KeyValue(gateBackground.getGlowLayer().fillProperty(), javafx.scene.paint.Color.web("#F0D99A", 0.0))),
            new KeyFrame(Duration.millis(80),
                new KeyValue(gateBackground.centerLightWidthProperty(), 22),
                new KeyValue(gateBackground.getGlowLayer().fillProperty(), javafx.scene.paint.Color.web("#F0D99A", 0.18))),
            new KeyFrame(Duration.millis(200),
                new KeyValue(gateBackground.centerLightWidthProperty(), 2),
                new KeyValue(gateBackground.getGlowLayer().fillProperty(), javafx.scene.paint.Color.web("#F0D99A", 0.0)))
        );

        // 表单重新出现
        FadeTransition fadeIn = new FadeTransition(Duration.millis(220), loginPanel);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(220), loginPanel);
        scaleIn.setToX(1);
        scaleIn.setToY(1);

        TranslateTransition moveBack = new TranslateTransition(Duration.millis(220), loginPanel);
        moveBack.setToY(LOGIN_PANEL_BASE_Y);

        ParallelTransition rebuild = new ParallelTransition(fadeIn, scaleIn, moveBack);

        // 隐藏消息
        Timeline hideMsg = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(gateMessage.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(150),
                new KeyValue(gateMessage.opacityProperty(), 0))
        );

        SequentialTransition seq = new SequentialTransition(
            new ParallelTransition(doorShake, lightFlash),
            rebuild,
            hideMsg
        );
        seq.setOnFinished(e -> {
            animating = false;
            loginButton.setText("入 阁");
        });
        seq.play();
    }

    // ===== 成功开门动画 =====

    public void playGateOpenAnimation(Runnable onFinished) {
        gateMessage.setText("门启，云开");
        gateMessage.setOpacity(1);

        // 金光增强
        Timeline light = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(gateBackground.centerLightWidthProperty(), 2),
                new KeyValue(gateBackground.getGlowLayer().fillProperty(), javafx.scene.paint.Color.web("#F0D99A", 0.0))),
            new KeyFrame(Duration.millis(260),
                new KeyValue(gateBackground.centerLightWidthProperty(), 110),
                new KeyValue(gateBackground.getGlowLayer().fillProperty(), javafx.scene.paint.Color.web("#F0D99A", 0.24))),
            new KeyFrame(Duration.millis(620),
                new KeyValue(gateBackground.getGlowLayer().fillProperty(), javafx.scene.paint.Color.web("#F0D99A", 0.0)))
        );

        // 文案淡出
        Timeline textFade = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(gateMessage.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(200),
                new KeyValue(gateMessage.opacityProperty(), 0))
        );
        textFade.setDelay(Duration.millis(300));

        // 左门打开
        TranslateTransition leftOpen = new TranslateTransition(Duration.millis(560), gateBackground.getLeftDoor());
        leftOpen.setToX(-gateBackground.getLeftDoor().getWidth() - 28);

        // 右门打开
        TranslateTransition rightOpen = new TranslateTransition(Duration.millis(560), gateBackground.getRightDoor());
        rightOpen.setToX(gateBackground.getRightDoor().getWidth() + 28);

        ParallelTransition open = new ParallelTransition(light, leftOpen, rightOpen, textFade);
        open.setOnFinished(e -> onFinished.run());
        open.play();
    }
}
