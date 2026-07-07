package com.qiyunge.ui.dashboard;

import com.qiyunge.app.AppContext;
import com.qiyunge.app.NavigationService;
import com.qiyunge.ui.components.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class DashboardView extends VBox {

    private final AppContext appContext;
    private final DashboardViewModel viewModel;

    public DashboardView(AppContext appContext) {
        this.appContext = appContext;
        this.viewModel = new DashboardViewModel(appContext);
        this.setPadding(new Insets(24));
        this.setSpacing(20);
        this.getStyleClass().add("dashboard-view");

        // 1. 云上问候（欢迎区域）
        VBox greetingCard = createGreetingCard();

        // 2. 捷径模块
        HBox shortcutsRow = createShortcuts();

        // 3. 统计卡片行
        HBox statsRow = createStatsRow();

        // 4. 中间两列布局
        HBox middleRow = new HBox(16);
        VBox.setVgrow(middleRow, Priority.ALWAYS);
        HBox.setHgrow(middleRow, Priority.ALWAYS);

        // 左侧：今日待办 + 余音未散
        VBox leftCol = new VBox(16);
        leftCol.setPrefWidth(400);
        HBox.setHgrow(leftCol, Priority.ALWAYS);
        leftCol.getChildren().addAll(
            createTodoCard(),
            createRecentPlayCard()
        );

        // 右侧：一日一景 + 藏处状态
        VBox rightCol = new VBox(16);
        rightCol.setPrefWidth(400);
        HBox.setHgrow(rightCol, Priority.ALWAYS);
        rightCol.getChildren().addAll(
            createGalleryCard(),
            createCacheCard()
        );

        middleRow.getChildren().addAll(leftCol, rightCol);

        this.getChildren().addAll(greetingCard, shortcutsRow, statsRow, middleRow);
    }

    // ========== 云上问候 ==========
    private VBox createGreetingCard() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: linear-gradient(to bottom right, #5B8DEF, #14B8A6); -fx-background-radius: 16px; -fx-padding: 28px 32px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 16, 0, 0, 4);");

        Label greetingLabel = new Label();
        greetingLabel.textProperty().bind(viewModel.greetingProperty());
        greetingLabel.setStyle("-fx-font-size: 26px; -fx-font-weight: 700; -fx-text-fill: white;");

        Label subLabel = new Label();
        subLabel.textProperty().bind(viewModel.greetingSubProperty());
        subLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.85);");

        card.getChildren().addAll(greetingLabel, subLabel);
        return card;
    }

    // ========== 捷径模块 ==========
    private HBox createShortcuts() {
        HBox row = new HBox(12);
        row.getChildren().addAll(
            createShortcut("♫", "听雨轩", NavigationService.Page.MUSIC),
            createShortcut("▦", "拾光廊", NavigationService.Page.GALLERY),
            createShortcut("○", "吾庐", NavigationService.Page.PROFILE),
            createShortcut("⚙", "云枢", NavigationService.Page.SETTINGS)
        );
        if (appContext.getUserSession().isAdmin()) {
            row.getChildren().add(createShortcut("▣", "阁务司", NavigationService.Page.ADMIN));
        }
        return row;
    }

    private VBox createShortcut(String icon, String label, NavigationService.Page page) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 12px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1);");
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle().replace("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1)", "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2)")));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2)", "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 4, 0, 0, 1)")));
        card.setOnMouseClicked(e -> appContext.getNavigationService().navigateTo(page));

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22px;");

        Label textLabel = new Label(label);
        textLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        card.getChildren().addAll(iconLabel, textLabel);
        return card;
    }

    // ========== 阁中概览（统计卡片） ==========
    private HBox createStatsRow() {
        HBox row = new HBox(12);
        HBox.setHgrow(row, Priority.ALWAYS);

        StatCard usersCard = new StatCard("用户总数", "0", "○");
        usersCard.getStyleClass().add("stat-card");
        viewModel.totalUsersProperty().addListener((o, old, val) -> usersCard.setValue(String.valueOf(val)));

        StatCard songsCard = new StatCard("歌曲总数", "0", "♫");
        viewModel.totalSongsProperty().addListener((o, old, val) -> songsCard.setValue(String.valueOf(val)));

        StatCard imagesCard = new StatCard("图库图片", "0", "▦");
        viewModel.totalImagesProperty().addListener((o, old, val) -> imagesCard.setValue(String.valueOf(val)));

        StatCard favCard = new StatCard("藏音", "0", "♥");
        viewModel.favoriteCountProperty().addListener((o, old, val) -> favCard.setValue(String.valueOf(val)));

        HBox.setHgrow(usersCard, Priority.ALWAYS);
        HBox.setHgrow(songsCard, Priority.ALWAYS);
        HBox.setHgrow(imagesCard, Priority.ALWAYS);
        HBox.setHgrow(favCard, Priority.ALWAYS);

        row.getChildren().addAll(usersCard, songsCard, imagesCard, favCard);
        return row;
    }

    // ========== 今日待办 ==========
    private VBox createTodoCard() {
        VBox card = createSectionCard("今日待办", "📋");

        VBox content = new VBox(8);
        content.setPadding(new Insets(4, 0, 0, 0));

        // Admin: show pending count
        Label pendingLabel = new Label();
        pendingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
        pendingLabel.visibleProperty().bind(viewModel.isAdminProperty());

        viewModel.pendingCountProperty().addListener((o, old, val) -> {
            if (val.intValue() > 0) {
                pendingLabel.setText("有 " + val + " 条注册申请待审批");
                pendingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -warning; -fx-font-weight: 600;");
            } else {
                pendingLabel.setText("暂无待审批申请");
                pendingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary;");
            }
        });

        // Normal user message
        Label normalLabel = new Label("一切就绪，没有待办事项。");
        normalLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary;");
        normalLabel.visibleProperty().bind(viewModel.isAdminProperty().not());

        content.getChildren().addAll(pendingLabel, normalLabel);
        card.getChildren().add(content);
        return card;
    }

    // ========== 余音未散 ==========
    private VBox createRecentPlayCard() {
        VBox card = createSectionCard("余音未散", "♫");

        VBox content = new VBox(8);
        content.setPadding(new Insets(4, 0, 0, 0));

        Label playLabel = new Label();
        playLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary;");
        viewModel.playHistoryCountProperty().addListener((o, old, val) -> {
            if (val.intValue() > 0) {
                playLabel.setText("已播放 " + val + " 首歌曲");
            } else {
                playLabel.setText("还没有播放记录");
            }
        });

        content.getChildren().add(playLabel);
        card.getChildren().add(content);
        return card;
    }

    // ========== 一日一景 ==========
    private VBox createGalleryCard() {
        VBox card = createSectionCard("一日一景", "▦");

        VBox content = new VBox(8);
        content.setPadding(new Insets(4, 0, 0, 0));

        Label galleryLabel = new Label();
        galleryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary;");
        viewModel.totalImagesProperty().addListener((o, old, val) -> {
            if (val.intValue() > 0) {
                galleryLabel.setText("图库中有 " + val + " 张图片");
            } else {
                galleryLabel.setText("图库暂无内容");
            }
        });

        content.getChildren().add(galleryLabel);
        card.getChildren().add(content);
        return card;
    }

    // ========== 藏处状态 ==========
    private VBox createCacheCard() {
        VBox card = createSectionCard("藏处状态", "◉");

        VBox content = new VBox(8);
        content.setPadding(new Insets(4, 0, 0, 0));

        Label cacheLabel = new Label();
        cacheLabel.textProperty().bind(viewModel.cacheStatusProperty());
        cacheLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary;");

        Label cacheHint = new Label("缓存运行正常");
        cacheHint.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");

        content.getChildren().addAll(cacheLabel, cacheHint);
        card.getChildren().add(content);
        return card;
    }

    // ========== 通用区块卡片 ==========
    private VBox createSectionCard(String title, String icon) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16px; -fx-padding: 20px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px;");
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        header.getChildren().addAll(iconLabel, titleLabel);

        card.getChildren().add(header);
        return card;
    }
}
