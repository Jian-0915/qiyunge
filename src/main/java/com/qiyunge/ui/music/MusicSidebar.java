package com.qiyunge.ui.music;

import com.qiyunge.app.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.function.Consumer;

/**
 * 音乐模块侧边导航栏。
 * 提供全部歌曲、在线寻音、藏音、余音、曲笺等导航项，
 * 以及导入音乐和音乐设置入口。
 */
public class MusicSidebar extends VBox {

    private final Consumer<String> onNavigate;
    private HBox activeItem;

    public MusicSidebar(AppContext appContext, Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;

        this.setPrefWidth(180);
        this.setMinWidth(180);
        this.setMaxWidth(180);
        this.setPadding(new Insets(12, 8, 12, 8));
        this.setSpacing(4);
        this.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border-light; -fx-border-width: 0 1px 0 0;");

        // 导航标题
        Label navTitle = new Label("听雨轩");
        navTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -text-primary; -fx-padding: 4 8 12 8;");

        // 主功能导航项
        HBox allSongsItem = createNavItem("全部歌曲", "\u266B", "allSongs");      // ♫
        HBox onlineItem = createNavItem("在线寻音", "\u2601", "online");        // ☁
        HBox favoritesItem = createNavItem("藏音", "\u2665", "favorites");      // ♥
        HBox historyItem = createNavItem("余音", "\u23F3", "history");         // ⏳
        HBox playlistsItem = createNavItem("曲笺", "\u266A", "playlists");      // ♪

        // 分隔线
        Region divider = new Region();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: -border-light;");
        divider.setPadding(new Insets(8, 0, 8, 0));

        // 辅助功能导航项
        HBox importItem = createNavItem("导入音乐", "\u2191", "import");        // ↑
        HBox settingsItem = createNavItem("音乐设置", "\u2699", "settings");    // ⚙

        // 底部用户信息
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox userInfo = new VBox(2);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        userInfo.setPadding(new Insets(8));
        userInfo.setStyle("-fx-background-color: -bg-tertiary; -fx-background-radius: 8px;");

        String displayName = appContext.getUserSession().getDisplayName();
        Label userNameLabel = new Label(displayName != null ? displayName : "访客");
        userNameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Label userRoleLabel = new Label(appContext.getUserSession().isAdmin() ? "管理员" : "用户");
        userRoleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");

        userInfo.getChildren().addAll(userNameLabel, userRoleLabel);

        this.getChildren().addAll(
            navTitle,
            allSongsItem,
            onlineItem,
            favoritesItem,
            historyItem,
            playlistsItem,
            divider,
            importItem,
            settingsItem,
            spacer,
            userInfo
        );

        // 默认选中全部歌曲
        setActiveItem(allSongsItem);
    }

    /**
     * 创建单个导航项。
     */
    private HBox createNavItem(String text, String icon, String pageKey) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(8, 12, 8, 12));
        item.setStyle("-fx-background-radius: 6px; -fx-cursor: hand;");
        item.setPrefWidth(164);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        iconLabel.setPrefWidth(20);
        iconLabel.setAlignment(Pos.CENTER);

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");

        item.getChildren().addAll(iconLabel, textLabel);

        // 鼠标悬停效果
        item.setOnMouseEntered(e -> {
            if (item != activeItem) {
                item.setStyle("-fx-background-radius: 6px; -fx-cursor: hand; -fx-background-color: -bg-tertiary;");
            }
        });
        item.setOnMouseExited(e -> {
            if (item != activeItem) {
                item.setStyle("-fx-background-radius: 6px; -fx-cursor: hand;");
            }
        });

        // 点击导航
        item.setOnMouseClicked(e -> {
            setActiveItem(item);
            if (onNavigate != null) {
                onNavigate.accept(pageKey);
            }
        });

        item.setUserData(pageKey);
        return item;
    }

    /**
     * 设置当前激活的导航项高亮。
     */
    private void setActiveItem(HBox item) {
        if (activeItem != null) {
            // 恢复之前选中项的默认样式
            for (javafx.scene.Node node : activeItem.getChildren()) {
                if (node instanceof Label label) {
                    label.setStyle(label.getStyle().replace("-primary", "-text-secondary"));
                }
            }
            activeItem.setStyle("-fx-background-radius: 6px; -fx-cursor: hand;");
        }
        activeItem = item;
        // 设置新选中项的高亮样式（蓝色背景）
        activeItem.setStyle("-fx-background-radius: 6px; -fx-cursor: hand; -fx-background-color: rgba(91,141,239,0.12);");
        for (javafx.scene.Node node : activeItem.getChildren()) {
            if (node instanceof Label label) {
                label.setStyle(label.getStyle().replace("-text-secondary", "-primary"));
            }
        }
    }
}
