package com.qiyunge.ui.profile;

import com.qiyunge.app.AppContext;
import com.qiyunge.ui.components.*;
import com.qiyunge.ui.face.FaceCaptureDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

public class ProfileView extends VBox {

    private final AppContext appContext;
    private final ProfileViewModel viewModel;
    private Runnable onAccountDeleted;

    public ProfileView(AppContext appContext) {
        this.appContext = appContext;
        this.viewModel = new ProfileViewModel(appContext);
        this.setPadding(new Insets(24));
        this.setSpacing(20);
        this.getStyleClass().add("profile-view");

        viewModel.setOnProfileUpdated(() -> {
            // Trigger refresh of top bar user info
            appContext.getNavigationService().navigateTo(
                appContext.getNavigationService().getCurrentPage()
            );
        });

        viewModel.setOnAccountDeleted(() -> {
            if (onAccountDeleted != null) {
                onAccountDeleted.run();
            }
        });

        // Top: Page header
        PageHeader header = new PageHeader("吾庐", "个人专属中心，管理你的资料与偏好");

        // Two column layout
        HBox mainContent = new HBox(20);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // Left column: Avatar + Info + Stats
        VBox leftCol = new VBox(16);
        leftCol.setPrefWidth(380);
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        leftCol.getChildren().addAll(
            createAvatarCard(),
            createStatsCard()
        );

        // Right column: Account security + Face login
        VBox rightCol = new VBox(16);
        rightCol.setPrefWidth(380);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.getChildren().addAll(
            createAccountCard(),
            createFaceLoginCard(),
            createPasswordCard()
        );

        // 管理员不允许注销账号
        if (!appContext.getUserSession().isAdmin()) {
            rightCol.getChildren().add(createDeleteAccountCard());
        }

        mainContent.getChildren().addAll(leftCol, rightCol);

        this.getChildren().addAll(header, mainContent);
    }

    public void setOnAccountDeleted(Runnable callback) {
        this.onAccountDeleted = callback;
    }

    // ========== 头像个性化区 ==========
    private VBox createAvatarCard() {
        VBox card = new VBox(18);
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16px; -fx-padding: 32px; -fx-alignment: center; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        // Avatar
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(80, 80);
        UserAvatar avatar = new UserAvatar(
            viewModel.displayNameProperty().get(),
            80
        );
        avatar.setStyle("-fx-background-color: " + viewModel.avatarColorProperty().get() + "25; -fx-background-radius: 50%;");
        avatarContainer.getChildren().add(avatar);

        // Bind avatar color changes
        viewModel.avatarColorProperty().addListener((o, old, val) -> {
            avatar.setStyle("-fx-background-color: " + val + "25; -fx-background-radius: 50%;");
        });

        // Display name
        Label nameLabel = new Label();
        nameLabel.textProperty().bind(viewModel.displayNameProperty());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        // Role badge
        HBox roleRow = new HBox(8);
        roleRow.setAlignment(Pos.CENTER);
        StatusBadge roleBadge = new StatusBadge(
            viewModel.roleProperty().get(),
            appContext.getUserSession().isAdmin() ? "admin" : "active"
        );
        Label statusLabel = new Label("· " + viewModel.statusProperty().get());
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");
        roleRow.getChildren().addAll(roleBadge, statusLabel);

        // Edit display name
        HBox editNameRow = new HBox(8);
        editNameRow.setAlignment(Pos.CENTER);
        editNameRow.setPadding(new Insets(8, 0, 0, 0));

        Label editHint = new Label("昵称");
        editHint.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");

        TextField nameField = new TextField();
        nameField.getStyleClass().add("app-text-field");
        nameField.setPrefWidth(160);
        nameField.setPromptText("修改昵称");
        nameField.textProperty().bindBidirectional(viewModel.displayNameProperty());

        AppButton saveNameBtn = new AppButton("保存", AppButton.Style.PRIMARY);
        saveNameBtn.setOnMouseClicked(e -> {
            String newName = nameField.getText();
            if (viewModel.updateDisplayName(newName)) {
                appContext.getDialogService().showInfo("保存成功", "昵称已更新为：" + newName);
            }
        });

        editNameRow.getChildren().addAll(editHint, nameField, saveNameBtn);

        // Avatar color picker
        HBox colorRow = new HBox(8);
        colorRow.setAlignment(Pos.CENTER);
        colorRow.setPadding(new Insets(8, 0, 0, 0));

        Label colorHint = new Label("头像色");
        colorHint.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");

        String[] colors = {"#5B8DEF", "#14B8A6", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#10B981", "#6366F1"};
        for (String color : colors) {
            Region dot = new Region();
            dot.setPrefSize(24, 24);
            dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 50%; -fx-cursor: hand; -fx-border-color: transparent; -fx-border-width: 2px;");
            dot.setOnMouseClicked(e -> {
                viewModel.updateAvatarColor(color);
                // Highlight selected
                colorRow.getChildren().forEach(child -> {
                    if (child instanceof Region r && r != dot) {
                        r.setStyle(r.getStyle().replace("-fx-border-color: -primary;", "-fx-border-color: transparent;"));
                    }
                });
                dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 50%; -fx-cursor: hand; -fx-border-color: -primary; -fx-border-width: 2px;");
            });
            colorRow.getChildren().add(dot);
        }

        card.getChildren().addAll(avatarContainer, nameLabel, roleRow, editNameRow, colorRow);
        return card;
    }

    // ========== 个人数据统计 ==========
    private VBox createStatsCard() {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16px; -fx-padding: 24px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label titleLabel = new Label("个人数据");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        HBox statsRow = new HBox(12);

        VBox favBox = createMiniStat("收藏", "0", "\u2665");
        viewModel.favoriteCountProperty().addListener((o, old, val) -> {
            ((Label) favBox.getChildren().get(1)).setText(String.valueOf(val));
        });

        VBox playBox = createMiniStat("播放", "0", "\u25B6");
        viewModel.playCountProperty().addListener((o, old, val) -> {
            ((Label) playBox.getChildren().get(1)).setText(String.valueOf(val));
        });

        VBox listBox = createMiniStat("曲笺", "0", "\u266B");
        viewModel.playlistCountProperty().addListener((o, old, val) -> {
            ((Label) listBox.getChildren().get(1)).setText(String.valueOf(val));
        });

        VBox imgBox = createMiniStat("图片", "0", "\u25A6");
        viewModel.imageCountProperty().addListener((o, old, val) -> {
            ((Label) imgBox.getChildren().get(1)).setText(String.valueOf(val));
        });

        HBox.setHgrow(favBox, Priority.ALWAYS);
        HBox.setHgrow(playBox, Priority.ALWAYS);
        HBox.setHgrow(listBox, Priority.ALWAYS);
        HBox.setHgrow(imgBox, Priority.ALWAYS);

        statsRow.getChildren().addAll(favBox, playBox, listBox, imgBox);

        // Info rows
        VBox infoSection = new VBox(0);
        infoSection.setStyle("-fx-border-color: -border-light; -fx-border-width: 0.5px 0 0 0;");

        // Login time
        HBox loginRow = new HBox(12);
        loginRow.setPadding(new Insets(12, 0, 8, 0));
        loginRow.setStyle("-fx-border-color: -border-light; -fx-border-width: 0 0 0.5px 0;");
        Label loginLabel = new Label("本次登录");
        loginLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-pref-width: 70px;");
        Label loginTime = new Label();
        loginTime.textProperty().bind(viewModel.loginTimeProperty());
        loginTime.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        loginRow.getChildren().addAll(loginLabel, loginTime);

        // Registration time
        HBox regRow = new HBox(12);
        regRow.setPadding(new Insets(8, 0, 0, 0));
        Label regLabel = new Label("注册时间");
        regLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-pref-width: 70px;");
        Label regTime = new Label();
        regTime.textProperty().bind(viewModel.createdAtProperty());
        regTime.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        regRow.getChildren().addAll(regLabel, regTime);

        infoSection.getChildren().addAll(loginRow, regRow);

        card.getChildren().addAll(titleLabel, statsRow, infoSection);
        return card;
    }

    private VBox createMiniStat(String label, String value, String icon) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: -bg-tertiary; -fx-background-radius: 10px;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 14px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");

        box.getChildren().addAll(iconLabel, valueLabel, nameLabel);
        return box;
    }

    // ========== 账号信息 ==========
    private VBox createAccountCard() {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16px; -fx-padding: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label titleLabel = new Label("账号信息");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        VBox fields = new VBox(0);
        fields.getChildren().addAll(
            createInfoRow("用户名", viewModel.usernameProperty().get()),
            createInfoRow("角色", viewModel.roleProperty().get()),
            createInfoRow("状态", viewModel.statusProperty().get())
        );

        card.getChildren().addAll(titleLabel, fields);
        return card;
    }

    private HBox createInfoRow(String label, String value) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 8, 0));
        row.setStyle("-fx-border-color: -border-light; -fx-border-width: 0 0 0.5px 0;");

        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary; -fx-pref-width: 70px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");

        row.getChildren().addAll(labelLabel, valueLabel);
        return row;
    }

    // ========== 人脸登录 ==========
    private VBox createFaceLoginCard() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16px; -fx-padding: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label titleLabel = new Label("人脸登录");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Label statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
        statusLabel.textProperty().bind(
            javafx.beans.binding.Bindings.when(viewModel.faceLoginEnabledProperty())
                .then("当前状态：已启用")
                .otherwise("当前状态：未启用")
        );

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);
        buttonRow.setPadding(new Insets(4, 0, 0, 0));

        AppButton setupBtn = new AppButton("录入人脸", AppButton.Style.PRIMARY);
        setupBtn.setOnMouseClicked(e -> openFaceCaptureDialog());

        AppButton deleteBtn = new AppButton("删除人脸", AppButton.Style.GHOST);
        deleteBtn.setOnMouseClicked(e -> {
            boolean confirmed = appContext.getDialogService().showConfirm(
                "确认删除", "确定要删除人脸数据吗？删除后将无法使用人脸登录。");
            if (confirmed) {
                if (viewModel.deleteFaceData()) {
                    appContext.getDialogService().showInfo("删除成功", "人脸数据已删除。");
                }
            }
        });

        // 根据是否已录入显示不同按钮
        setupBtn.visibleProperty().bind(viewModel.faceLoginEnabledProperty().not());
        setupBtn.managedProperty().bind(viewModel.faceLoginEnabledProperty().not());
        deleteBtn.visibleProperty().bind(viewModel.faceLoginEnabledProperty());
        deleteBtn.managedProperty().bind(viewModel.faceLoginEnabledProperty());

        buttonRow.getChildren().addAll(setupBtn, deleteBtn);

        Label hintLabel = new Label("录入后可在登录页使用「刷脸入阁」快速登录");
        hintLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-wrap-text: true;");

        card.getChildren().addAll(titleLabel, statusLabel, buttonRow, hintLabel);
        return card;
    }

    private void openFaceCaptureDialog() {
        int userId = appContext.getUserSession().getUserId();
        var faceService = appContext.getFaceRecognitionService();
        if (faceService == null) {
            appContext.getDialogService().showError("服务异常", "人脸识别服务未初始化");
            return;
        }

        FaceCaptureDialog dialog = new FaceCaptureDialog(
            appContext.getPrimaryStage(), faceService, userId,
            samples -> {
                // 采集完成，异步训练模型
                faceService.trainModelAsync(userId, samples)
                    .thenAccept(success -> {
                        javafx.application.Platform.runLater(() -> {
                            if (success) {
                                appContext.getDialogService().showInfo(
                                    "录入成功", "人脸数据已保存，现在可以使用刷脸登录了。");
                                viewModel.refreshFaceDataStatus();
                            } else {
                                appContext.getDialogService().showError(
                                    "录入失败", "模型训练失败，请重试。");
                            }
                        });
                    });
            }
        );
        dialog.show();
        dialog.startPreview();
    }

    // ========== 修改密码 ==========
    private VBox createPasswordCard() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16px; -fx-padding: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label titleLabel = new Label("修改密码");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Label oldLabel = new Label("当前密码");
        oldLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");
        PasswordField oldField = new PasswordField();
        oldField.getStyleClass().add("app-password-field");
        oldField.setPrefHeight(32);
        oldField.setPromptText("输入当前密码");

        Label newLabel = new Label("新密码");
        newLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");
        PasswordField newField = new PasswordField();
        newField.getStyleClass().add("app-password-field");
        newField.setPrefHeight(32);
        newField.setPromptText("至少6位字符");

        Label confirmLabel = new Label("确认新密码");
        confirmLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");
        PasswordField confirmField = new PasswordField();
        confirmField.getStyleClass().add("app-password-field");
        confirmField.setPrefHeight(32);
        confirmField.setPromptText("再次输入新密码");

        Label errorLabel = new Label();
        errorLabel.textProperty().bind(viewModel.changePasswordErrorProperty());
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -danger; -fx-min-height: 16px;");
        errorLabel.visibleProperty().bind(viewModel.changePasswordErrorProperty().isNotEmpty());

        AppButton changeBtn = new AppButton("确认修改", AppButton.Style.PRIMARY);
        changeBtn.disableProperty().bind(viewModel.changingPasswordProperty());
        changeBtn.setOnMouseClicked(e -> {
            boolean ok = viewModel.changePassword(
                oldField.getText(), newField.getText(), confirmField.getText()
            );
            if (ok) {
                appContext.getDialogService().showInfo("修改成功", "密码已修改，下次登录时生效。");
                oldField.clear();
                newField.clear();
                confirmField.clear();
            }
        });

        card.getChildren().addAll(
            titleLabel, oldLabel, oldField, newLabel, newField,
            confirmLabel, confirmField, errorLabel, changeBtn
        );
        return card;
    }

    private VBox createDeleteAccountCard() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16px; -fx-padding: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label titleLabel = new Label("注销账号");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -danger;");

        Label descLabel = new Label("此操作将永久删除您的账号及所有数据，无法恢复。");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");

        Label pwLabel = new Label("请输入密码确认");
        pwLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");
        PasswordField pwField = new PasswordField();
        pwField.getStyleClass().add("app-password-field");
        pwField.setPrefHeight(32);
        pwField.setPromptText("输入当前密码");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -danger; -fx-min-height: 16px;");

        AppButton deleteBtn = new AppButton("注销账号", AppButton.Style.DANGER);
        deleteBtn.setOnMouseClicked(e -> {
            String password = pwField.getText();
            if (password == null || password.isEmpty()) {
                errorLabel.setText("请输入密码");
                return;
            }
            errorLabel.setText("");

            boolean confirmed = appContext.getDialogService().showConfirm(
                "确认注销", "确定要永久注销账号吗？此操作无法撤销，所有数据将被删除。");
            if (!confirmed) return;

            boolean ok = viewModel.deleteAccount(password);
            if (ok) {
                appContext.getDialogService().showInfo("注销成功", "账号已删除，感谢使用栖云阁。");
                // 通过 onAccountDeleted 回调 -> MainShell.onLogout -> AppLauncher.showLoginView
            } else {
                errorLabel.setText("注销失败，请检查密码是否正确");
            }
        });

        card.getChildren().addAll(titleLabel, descLabel, pwLabel, pwField, errorLabel, deleteBtn);
        return card;
    }
}
