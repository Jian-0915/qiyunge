package com.qiyunge.ui.entertainment;

import com.qiyunge.app.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * 百趣园模块侧边导航栏。
 * 遵循闲云馆设计规范：shell-sidebar 样式。
 */
public class EntertainmentSidebar extends VBox {

    private final Consumer<String> onNavigate;
    private HBox activeItem;

    public EntertainmentSidebar(AppContext appContext, Consumer<String> onNavigate) {
        this.onNavigate = onNavigate;

        this.setPrefWidth(200);
        this.setMinWidth(200);
        this.setMaxWidth(200);
        this.setPadding(new Insets(20, 12, 20, 12));
        this.setSpacing(0);
        this.setStyle("-fx-background-color: -bg-nav; -fx-border-color: -border-light; -fx-border-width: 0 1px 0 0;");

        // ===== 品牌区 =====
        VBox brand = new VBox();
        brand.setPadding(new Insets(4, 12, 20, 12));
        brand.setStyle("-fx-border-color: -border-light; -fx-border-width: 0 0 1px 0;");

        Label brandName = new Label("闲云馆");
        brandName.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary; -fx-line-spacing: 1.25;");

        Label brandSubtitle = new Label("放松一下，享受乐趣");
        brandSubtitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: -text-tertiary; -fx-letter-spacing: 0.05em;");
        brandSubtitle.setPadding(new Insets(2, 0, 0, 0));

        brand.getChildren().addAll(brandName, brandSubtitle);

        // ===== 导航项容器 =====
        VBox navContainer = new VBox(4);
        navContainer.setPadding(new Insets(16, 4, 0, 4));

        HBox gamesItem = createNavItem("游乐场", "\u2666", "games");
        HBox toolsItem = createNavItem("趣味坊", "\u2601", "tools");
        HBox recordsItem = createNavItem("趣迹", "\u2665", "records");

        navContainer.getChildren().addAll(gamesItem, toolsItem, recordsItem);

        // ===== 底部弹簧 =====
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // ===== 底部用户信息 =====
        VBox userInfo = new VBox(2);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        userInfo.setPadding(new Insets(8, 12, 8, 12));
        userInfo.setStyle("-fx-background-color: -bg-input; -fx-background-radius: 10px;");

        String displayName = appContext.getUserSession().getDisplayName();
        Label userNameLabel = new Label(displayName != null ? displayName : "访客");
        userNameLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Label userRoleLabel = new Label(appContext.getUserSession().isAdmin() ? "管理员" : "用户");
        userRoleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");

        userInfo.getChildren().addAll(userNameLabel, userRoleLabel);

        this.getChildren().addAll(brand, navContainer, spacer, userInfo);

        // 默认选中游乐场
        setActiveItem(gamesItem);
    }

    /**
     * 创建单个导航项，遵循 shell-sidebar__nav-item 样式。
     */
    private HBox createNavItem(String text, String icon, String pageKey) {
        HBox item = new HBox(10);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10, 12, 10, 12));
        item.setStyle("-fx-background-radius: 10px; -fx-cursor: hand;");
        item.setPrefWidth(176);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: -text-secondary;");
        iconLabel.setPrefWidth(20);
        iconLabel.setAlignment(Pos.CENTER);

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: -text-secondary;");

        item.getChildren().addAll(iconLabel, textLabel);

        // 鼠标悬停效果
        item.setOnMouseEntered(e -> {
            if (item != activeItem) {
                item.setStyle("-fx-background-radius: 10px; -fx-cursor: hand; -fx-background-color: -bg-hover;");
                textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: -text-primary;");
            }
        });
        item.setOnMouseExited(e -> {
            if (item != activeItem) {
                item.setStyle("-fx-background-radius: 10px; -fx-cursor: hand;");
                textLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: -text-secondary;");
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
     * 设置当前激活的导航项高亮，遵循 shell-sidebar__nav-item[data-active="true"] 样式。
     */
    private void setActiveItem(HBox item) {
        // 恢复之前选中项
        if (activeItem != null) {
            applyInactiveStyle(activeItem);
        }
        activeItem = item;
        applyActiveStyle(activeItem);
    }

    private void applyActiveStyle(HBox item) {
        item.setStyle("-fx-background-radius: 10px; -fx-cursor: hand; -fx-background-color: -primary-light;");
        for (javafx.scene.Node node : item.getChildren()) {
            if (node instanceof Label label) {
                label.setStyle(label.getStyle()
                    .replace("-fx-text-fill: -text-secondary;", "-fx-text-fill: -primary;")
                    .replace("-fx-font-weight: 500;", "-fx-font-weight: 600;"));
            }
        }
    }

    private void applyInactiveStyle(HBox item) {
        item.setStyle("-fx-background-radius: 10px; -fx-cursor: hand;");
        for (javafx.scene.Node node : item.getChildren()) {
            if (node instanceof Label label) {
                label.setStyle(label.getStyle()
                    .replace("-fx-text-fill: -primary;", "-fx-text-fill: -text-secondary;")
                    .replace("-fx-font-weight: 600;", "-fx-font-weight: 500;"));
            }
        }
    }
}
