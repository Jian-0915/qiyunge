package com.qiyunge.ui.settings;

import com.qiyunge.app.AppContext;
import com.qiyunge.ui.components.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

public class SettingsView extends VBox {

    public SettingsView(AppContext appContext) {
        this.setPadding(new Insets(24));
        this.setSpacing(20);
        this.getStyleClass().add("settings-view");

        PageHeader header = new PageHeader("设置", "自定义你的栖云阁");

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(0, 0, 24, 0));

        // Theme section
        VBox themeSection = createSection("外观", "主题");
        Label themePlaceholder = new Label("主题切换功能将在视图加载后可用");
        themePlaceholder.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary;");

        // Defer theme selector creation until scene is available
        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                themeSection.getChildren().clear();
                themeSection.getChildren().add(new Label("外观"));
                ((Label) themeSection.getChildren().get(0)).setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
                themeSection.getChildren().add(new ThemeSelector(appContext.getThemeService(), newScene));
            }
        });
        themeSection.getChildren().add(themePlaceholder);
        content.getChildren().add(themeSection);

        // Account section
        VBox accountSection = createSection("账户", null);
        accountSection.getChildren().add(createSettingRow("用户名", appContext.getUserSession().getUsername()));
        accountSection.getChildren().add(createSettingRow("角色", appContext.getUserSession().isAdmin() ? "管理员" : "用户"));
        content.getChildren().add(accountSection);

        // Cache section
        VBox cacheSection = createSection("缓存", null);
        cacheSection.getChildren().add(createSettingRow("缓存目录", appContext.getAppStorage().getCachePath().toString()));
        AppButton clearCacheBtn = new AppButton("清理缓存", AppButton.Style.SECONDARY);
        clearCacheBtn.setOnAction(e -> {
            appContext.getDialogService().showInfo("清理缓存", "缓存清理功能将在后续版本中完善。");
        });
        HBox cacheAction = new HBox();
        cacheAction.setAlignment(Pos.CENTER_RIGHT);
        cacheAction.getChildren().add(clearCacheBtn);
        cacheSection.getChildren().add(cacheAction);
        content.getChildren().add(cacheSection);

        // About section
        VBox aboutSection = createSection("关于", null);
        aboutSection.getChildren().add(createSettingRow("应用名称", "栖云阁"));
        aboutSection.getChildren().add(createSettingRow("版本", "1.0.0"));
        aboutSection.getChildren().add(createSettingRow("技术栈", "Java 21 + JavaFX + SQLite"));
        content.getChildren().add(aboutSection);

        scrollPane.setContent(content);
        this.getChildren().addAll(header, scrollPane);
    }

    private VBox createSection(String title, String subtitle) {
        VBox section = new VBox(12);
        section.setPadding(new Insets(16, 20, 16, 20));
        section.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        section.getChildren().add(titleLabel);
        return section;
    }

    private HBox createSettingRow(String label, String value) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 0, 8, 0));

        Label labelLabel = new Label(label);
        labelLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary; -fx-pref-width: 100px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");

        row.getChildren().addAll(labelLabel, valueLabel);
        return row;
    }
}
