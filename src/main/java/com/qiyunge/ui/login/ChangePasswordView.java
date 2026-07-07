package com.qiyunge.ui.login;

import com.qiyunge.app.AppContext;
import com.qiyunge.ui.components.AppButton;
import com.qiyunge.ui.components.WindowTitleBar;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.*;

public class ChangePasswordView extends VBox {

    private final AppContext appContext;
    private Runnable onPasswordChanged;
    private Runnable onCancel;

    public ChangePasswordView(AppContext appContext) {
        this.appContext = appContext;
        this.setAlignment(Pos.TOP_CENTER);
        this.setPadding(Insets.EMPTY);
        this.setSpacing(0);
        this.setStyle("-fx-background-color: -bg-primary;");

        // Window title bar
        WindowTitleBar titleBar = new WindowTitleBar(appContext.getPrimaryStage());

        // Content area
        VBox content = new VBox();
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));
        content.setStyle("-fx-background-color: -bg-primary;");
        VBox.setVgrow(content, Priority.ALWAYS);

        buildUI(content);

        this.getChildren().addAll(titleBar, content);
    }

    public void setOnPasswordChanged(Runnable callback) {
        this.onPasswordChanged = callback;
    }

    public void setOnCancel(Runnable callback) {
        this.onCancel = callback;
    }

    private void buildUI(VBox content) {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(40));
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 24px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 24, 0, 0, 4);");

        Label titleLabel = new Label("修改密码");
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        Label hintLabel = new Label("首次登录需要修改默认密码");
        hintLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");

        Label errorLabel = new Label("");
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -danger; -fx-min-height: 18px;");

        // Current password (skip if must_change_password)
        Label currentLabel = new Label("当前密码");
        currentLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");
        PasswordField currentField = new PasswordField();
        currentField.getStyleClass().add("app-password-field");
        currentField.setPromptText("当前密码");
        currentField.setManaged(!appContext.getUserSession().isMustChangePassword());
        currentField.setVisible(!appContext.getUserSession().isMustChangePassword());
        currentLabel.setManaged(!appContext.getUserSession().isMustChangePassword());
        currentLabel.setVisible(!appContext.getUserSession().isMustChangePassword());

        Label newLabel = new Label("新密码");
        newLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");
        PasswordField newField = new PasswordField();
        newField.getStyleClass().add("app-password-field");
        newField.setPromptText("至少6位字符");

        Label confirmLabel = new Label("确认新密码");
        confirmLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");
        PasswordField confirmField = new PasswordField();
        confirmField.getStyleClass().add("app-password-field");
        confirmField.setPromptText("再次输入新密码");

        AppButton confirmBtn = new AppButton("确认修改", AppButton.Style.PRIMARY);
        confirmBtn.setPrefWidth(Double.MAX_VALUE);
        confirmBtn.setPrefHeight(44);

        AppButton cancelBtn = new AppButton("取消", AppButton.Style.GHOST);
        cancelBtn.setPrefWidth(Double.MAX_VALUE);

        card.getChildren().addAll(
            titleLabel, hintLabel,
            new Region() {{ setPrefHeight(8); }},
            errorLabel,
            currentLabel, currentField,
            newLabel, newField,
            confirmLabel, confirmField,
            new Region() {{ setPrefHeight(8); }},
            confirmBtn, cancelBtn
        );

        confirmBtn.setOnAction(e -> {
            String current = currentField.getText();
            String newPass = newField.getText();
            String confirm = confirmField.getText();

            if (!appContext.getUserSession().isMustChangePassword() && (current == null || current.isEmpty())) {
                errorLabel.setText("请输入当前密码");
                return;
            }
            if (newPass == null || newPass.length() < 6) {
                errorLabel.setText("新密码至少6位");
                return;
            }
            if (!newPass.equals(confirm)) {
                errorLabel.setText("两次输入的密码不一致");
                return;
            }

            boolean ok = appContext.getAuthService().changePassword(
                appContext.getUserSession().getUserId(),
                appContext.getUserSession().isMustChangePassword() ? null : current,
                newPass
            );

            if (ok) {
                appContext.getUserSession().logout();
                if (onPasswordChanged != null) {
                    onPasswordChanged.run();
                }
            } else {
                errorLabel.setText("修改失败，请检查当前密码是否正确");
            }
        });

        cancelBtn.setOnAction(e -> {
            if (appContext.getUserSession().isMustChangePassword()) {
                // Cannot cancel if forced
                errorLabel.setText("首次登录必须修改密码");
                return;
            }
            if (onCancel != null) {
                onCancel.run();
            }
        });

        // Enter key
        confirmField.setOnAction(e -> confirmBtn.fire());

        content.getChildren().add(card);
    }
}
