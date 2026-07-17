package com.qiyunge.ui.entertainment;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.EntertainmentService;
import com.qiyunge.domain.entity.GameRecord;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;

/**
 * 百趣园模块主视图。
 * 采用三栏布局：顶部标题栏、左侧边导航、主内容区。
 */
public class EntertainmentView extends BorderPane {

    private final AppContext appContext;
    private final VBox contentArea;
    private Object currentGameView = null;

    public EntertainmentView(AppContext appContext) {
        this.appContext = appContext;
        this.getStyleClass().add("entertainment-view");

        // ===== Top: 顶部标题栏 =====
        EntertainmentHeader header = new EntertainmentHeader(appContext);
        setTop(header);

        // ===== Left: 侧边导航栏 =====
        EntertainmentSidebar sidebar = new EntertainmentSidebar(appContext, this::navigateTo);
        setLeft(sidebar);

        // ===== Center: 主内容区 =====
        // 不内嵌 ScrollPane，由 MainShell 的 wrapPage ScrollPane 统一处理滚动
        contentArea = new VBox(8);
        contentArea.setPadding(new Insets(24));
        contentArea.setStyle("-fx-background-color: -bg-primary;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        setCenter(contentArea);

        // 默认显示游乐场
        navigateTo("games");
    }

    /**
     * 页面导航方法。
     */
    private void navigateTo(String pageKey) {
        switch (pageKey) {
            case "games" -> showGamesPage();
            case "guessNumber" -> showGuessNumberPage();
            case "memoryFlip" -> showMemoryFlipPage();
            case "tools" -> showToolsPage();
            case "records" -> showRecordsPage();
            default -> showGamesPage();
        }
    }

    // ========== 游乐场页面 ==========
    private void showGamesPage() {
        cleanupCurrentGame();
        contentArea.getChildren().clear();

        // 页面标题
        VBox pageHeader = new VBox(4);
        pageHeader.setPadding(new Insets(0, 0, 28, 0));
        Label title = new Label("游乐场");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label subtitle = new Label("选一个游戏，放松一下");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        pageHeader.getChildren().addAll(title, subtitle);
        contentArea.getChildren().add(pageHeader);

        // 游戏卡片网格 (2列)
        GridPane gameGrid = new GridPane();
        gameGrid.setHgap(16);
        gameGrid.setVgap(16);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setPercentWidth(50);
        gameGrid.getColumnConstraints().addAll(col1, col2);

        gameGrid.add(createGameCard("♦", "猜数", "猜一个数字，感受运筹的乐趣",
                "-primary", "rgba(91,141,239,0.7)", "BEST", "-primary", true), 0, 0);
        gameGrid.add(createGameCard("♠", "翻牌", "翻转卡牌，寻找配对",
                "-accent", "rgba(20,184,166,0.7)", "BEST", "-accent", true, "memoryFlip"), 1, 0);
        gameGrid.add(createGameCard("♣", "华容道", "滑动方块，破解迷局",
                "rgba(91,141,239,0.75)", "rgba(20,184,166,0.75)", "敬请期待", "-accent", false), 0, 1);
        gameGrid.add(createGameCard("♛", "落子", "黑白之间，胜负立判",
                "rgba(20,184,166,0.85)", "rgba(91,141,239,0.6)", "敬请期待", "-primary", false), 1, 1);

        contentArea.getChildren().add(gameGrid);

        // 快速统计行 (3列)
        HBox statsRow = new HBox(12);
        statsRow.setPadding(new Insets(24, 0, 0, 0));

        int todayGames = 0;
        int streak = 0;
        int totalAchievements = 0;
        try {
            EntertainmentService svc = appContext.getEntertainmentService();
            if (svc != null && appContext.getUserSession().isLoggedIn()) {
                int uid = appContext.getUserSession().getUserId();
                totalAchievements = svc.getTotalGameCount(uid);
            }
        } catch (Exception ignored) {}

        statsRow.getChildren().addAll(
            createStatCard(String.valueOf(todayGames), "今日游戏"),
            createStatCard(String.valueOf(streak), "连续打卡"),
            createStatCard(String.valueOf(totalAchievements), "总成就")
        );
        HBox.setHgrow(statsRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(statsRow.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(statsRow.getChildren().get(2), Priority.ALWAYS);

        contentArea.getChildren().add(statsRow);
    }

    private VBox createGameCard(String icon, String name, String desc,
                                String gradientFrom, String gradientTo,
                                String badgeText, String badgeColor, boolean clickable) {
        return createGameCard(icon, name, desc, gradientFrom, gradientTo, badgeText, badgeColor, clickable, clickable ? "guessNumber" : null);
    }

    private VBox createGameCard(String icon, String name, String desc,
                                String gradientFrom, String gradientTo,
                                String badgeText, String badgeColor, boolean clickable, String navKey) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16; " +
                "-fx-border-color: -border-light; -fx-border-width: 1; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1); " +
                "-fx-cursor: hand;");

        // Hover: translateY -2px + enhanced shadow
        String baseStyle = card.getStyle();
        String hoverStyle = "-fx-background-color: -bg-card; -fx-background-radius: 16; " +
                "-fx-border-color: -border-light; -fx-border-width: 1; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 4); " +
                "-fx-cursor: hand; -fx-translate-y: -2;";
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        if (clickable && navKey != null) {
            card.setOnMouseClicked(e -> navigateTo(navKey));
        }

        // 图标容器 (56x56, 渐变背景, 白色图标)
        StackPane iconWrapper = new StackPane();
        iconWrapper.setPrefSize(56, 56);
        iconWrapper.setMinSize(56, 56);
        iconWrapper.setMaxSize(56, 56);
        iconWrapper.setStyle("-fx-background-radius: 10; " +
                "-fx-background-color: linear-gradient(to bottom right, " + gradientFrom + ", " + gradientTo + ");");
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: white;");
        iconWrapper.getChildren().add(iconLabel);
        card.getChildren().add(iconWrapper);

        // 名称行: 游戏名 + 徽章
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        nameRow.getChildren().add(nameLabel);

        // 徽章 (pill shape)
        Label badge = new Label(badgeText);
        // 根据颜色选择浅色背景
        String badgeBg = badgeColor.equals("-primary")
                ? "-primary-light" : "-accent-light";
        badge.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: " + badgeColor + "; " +
                "-fx-background-color: " + badgeBg + "; -fx-background-radius: 9999; " +
                "-fx-padding: 2 8 2 8;");
        nameRow.getChildren().add(badge);
        card.getChildren().add(nameRow);

        // 描述
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        descLabel.setWrapText(true);
        card.getChildren().add(descLabel);

        return card;
    }

    private VBox createStatCard(String value, String caption) {
        VBox stat = new VBox(4);
        stat.setPadding(new Insets(16));
        stat.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 10; " +
                "-fx-border-color: -border-light; -fx-border-width: 1; -fx-border-radius: 10;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        Label captionLabel = new Label(caption);
        captionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        stat.getChildren().addAll(valueLabel, captionLabel);
        return stat;
    }

    // ========== 猜数游戏页面 ==========
    private void showGuessNumberPage() {
        cleanupCurrentGame();
        contentArea.getChildren().clear();
        GuessNumberView guessNumberView = new GuessNumberView(appContext);
        guessNumberView.setOnBack(() -> navigateTo("games"));
        VBox.setVgrow(guessNumberView, Priority.ALWAYS);
        contentArea.getChildren().add(guessNumberView);
        currentGameView = guessNumberView;
    }

    // ========== 记忆翻牌游戏页面 ==========
    private void showMemoryFlipPage() {
        cleanupCurrentGame();
        contentArea.getChildren().clear();
        MemoryFlipView memoryFlipView = new MemoryFlipView(appContext);
        memoryFlipView.setOnBack(() -> navigateTo("games"));
        VBox.setVgrow(memoryFlipView, Priority.ALWAYS);
        contentArea.getChildren().add(memoryFlipView);
        currentGameView = memoryFlipView;
    }

    // ========== 趣味坊页面 ==========
    private void showToolsPage() {
        cleanupCurrentGame();
        contentArea.getChildren().clear();

        // 页面标题
        VBox pageHeader = new VBox(4);
        pageHeader.setPadding(new Insets(0, 0, 28, 0));
        Label title = new Label("趣味坊");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label subtitle = new Label("小工具，大乐趣");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        pageHeader.getChildren().addAll(title, subtitle);
        contentArea.getChildren().add(pageHeader);

        // 工具卡片网格 (3列)
        GridPane toolGrid = new GridPane();
        toolGrid.setHgap(20);
        toolGrid.setVgap(20);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(33.3);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setPercentWidth(33.3);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setPercentWidth(33.3);
        toolGrid.getColumnConstraints().addAll(c1, c2, c3);

        toolGrid.add(createToolCard("☯", "求签", "摇一摇，看看今天的运势",
                "-warning-light", "-warning", "敬请期待"), 0, 0);
        toolGrid.add(createToolCard("☯", "问卜", "抛个卦，探一探前路方向",
                "-accent-light", "-accent", "敬请期待"), 1, 0);
        toolGrid.add(createToolCard("⏱", "专注计时", "设定时间，沉浸当下",
                "-primary-light", "-primary", "敬请期待"), 2, 0);

        contentArea.getChildren().add(toolGrid);

        // 即将上线区域
        VBox comingSoonSection = new VBox(16);
        comingSoonSection.setPadding(new Insets(32, 0, 0, 0));

        // 区域标题: 短线 + "即将上线"
        HBox sectionTitleRow = new HBox(8);
        sectionTitleRow.setAlignment(Pos.CENTER_LEFT);
        Separator line = new Separator();
        line.setPrefWidth(20);
        line.setStyle("-fx-color: -text-secondary;");
        Label sectionTitle = new Label("即将上线");
        sectionTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");
        sectionTitleRow.getChildren().addAll(line, sectionTitle);
        comingSoonSection.getChildren().add(sectionTitleRow);

        // 即将上线卡片网格 (2列)
        GridPane soonGrid = new GridPane();
        soonGrid.setHgap(20);
        soonGrid.setVgap(16);
        ColumnConstraints sc1 = new ColumnConstraints();
        sc1.setPercentWidth(50);
        ColumnConstraints sc2 = new ColumnConstraints();
        sc2.setPercentWidth(50);
        soonGrid.getColumnConstraints().addAll(sc1, sc2);

        soonGrid.add(createComingSoonCard("📝", "每日一题", "每天一道趣味挑战"), 0, 0);
        soonGrid.add(createComingSoonCard("🔗", "接龙", "词语接龙，思维碰撞"), 1, 0);

        comingSoonSection.getChildren().add(soonGrid);
        contentArea.getChildren().add(comingSoonSection);
    }

    private VBox createToolCard(String icon, String name, String desc,
                                String iconBgColor, String iconColor, String footerText) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16; " +
                "-fx-border-color: -border-light; -fx-border-width: 1; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1); " +
                "-fx-cursor: hand;");

        String baseStyle = card.getStyle();
        String hoverStyle = "-fx-background-color: -bg-card; -fx-background-radius: 16; " +
                "-fx-border-color: -border-light; -fx-border-width: 1; -fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.10), 12, 0, 0, 4); " +
                "-fx-cursor: hand; -fx-translate-y: -2;";
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        // 图标容器 (44x44)
        StackPane iconWrapper = new StackPane();
        iconWrapper.setPrefSize(44, 44);
        iconWrapper.setMinSize(44, 44);
        iconWrapper.setMaxSize(44, 44);
        iconWrapper.setStyle("-fx-background-radius: 10; -fx-background-color: " + iconBgColor + ";");
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: " + iconColor + ";");
        iconWrapper.getChildren().add(iconLabel);
        card.getChildren().add(iconWrapper);

        // 名称
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        card.getChildren().add(nameLabel);

        // 描述
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        descLabel.setWrapText(true);
        card.getChildren().add(descLabel);

        // 底部: badge 或 统计文本
        Label footerLabel = new Label(footerText);
        footerLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: -text-secondary; " +
                "-fx-background-color: -bg-tertiary; -fx-background-radius: 9999; " +
                "-fx-padding: 2 8 2 8;");
        card.getChildren().add(footerLabel);

        return card;
    }

    private VBox createComingSoonCard(String icon, String name, String desc) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16; " +
                "-fx-border-color: -border-light; -fx-border-width: 1; -fx-border-radius: 16; " +
                "-fx-opacity: 0.55;");

        // 名称行 + 右上角 "即将" badge
        HBox nameRow = new HBox(8);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label soonBadge = new Label("即将");
        soonBadge.setStyle("-fx-font-size: 10px; -fx-font-weight: 500; -fx-text-fill: -text-secondary; " +
                "-fx-background-color: -bg-tertiary; -fx-background-radius: 9999; " +
                "-fx-padding: 1 6 1 6;");
        nameRow.getChildren().addAll(nameLabel, spacer, soonBadge);
        card.getChildren().add(nameRow);

        // 图标
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");
        card.getChildren().add(iconLabel);

        // 描述
        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        descLabel.setWrapText(true);
        card.getChildren().add(descLabel);

        return card;
    }

    // ========== 趣迹页面 ==========

    private void cleanupCurrentGame() {
        if (currentGameView instanceof MemoryFlipView mfv) {
            mfv.cleanup();
        }
        currentGameView = null;
    }


    private void showRecordsPage() {
        cleanupCurrentGame();
        contentArea.getChildren().clear();

        // 页面标题
        VBox pageHeader = new VBox(4);
        pageHeader.setPadding(new Insets(0, 0, 28, 0));
        Label title = new Label("趣迹");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label subtitle = new Label("你的闲云馆足迹");
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: -text-secondary;");
        pageHeader.getChildren().addAll(title, subtitle);
        contentArea.getChildren().add(pageHeader);

        // 统计概览 (3列)
        int totalGames = 0;
        int streak = 0;
        int achievements = 0;
        try {
            EntertainmentService svc = appContext.getEntertainmentService();
            if (svc != null && appContext.getUserSession().isLoggedIn()) {
                int uid = appContext.getUserSession().getUserId();
                totalGames = svc.getTotalGameCount(uid);
            }
        } catch (Exception ignored) {}

        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
            createRecordStatCard("🎮", String.valueOf(totalGames), "累计游戏",
                    "-primary-light", "-primary"),
            createRecordStatCard("🔥", String.valueOf(streak), "连续打卡",
                    "-accent-light", "-accent"),
            createRecordStatCard("🏆", String.valueOf(achievements), "成就解锁",
                    "-warning-light", "-warning")
        );
        for (var node : statsRow.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }
        contentArea.getChildren().add(statsRow);

        // 最佳成绩区域
        VBox bestRecordsSection = new VBox(12);
        bestRecordsSection.setPadding(new Insets(28, 0, 0, 0));

        Label sectionTitle = new Label("最佳成绩");
        sectionTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        bestRecordsSection.getChildren().add(sectionTitle);

        // 表格
        VBox table = new VBox(0);
        table.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 10; " +
                "-fx-border-color: -border-light; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);");

        // 表头
        HBox headerRow = createTableRow("游戏", "难度", "最佳成绩", "达成时间", true);
        table.getChildren().add(headerRow);

        // 从服务加载真实数据
        boolean hasRecords = false;
        try {
            EntertainmentService svc = appContext.getEntertainmentService();
            if (svc != null && appContext.getUserSession().isLoggedIn()) {
                int uid = appContext.getUserSession().getUserId();
                // 获取猜数游戏的最佳成绩
                GameRecord bestEasy = svc.getBestScore(uid, "guess_number", "简单");
                GameRecord bestMedium = svc.getBestScore(uid, "guess_number", "中等");
                GameRecord bestHard = svc.getBestScore(uid, "guess_number", "困难");

                if (bestEasy != null) {
                    table.getChildren().add(createTableRow("猜数", "简单",
                            String.valueOf(bestEasy.getScore()),
                            bestEasy.getCreatedAt() != null ? bestEasy.getCreatedAt() : "-", false));
                    hasRecords = true;
                }
                if (bestMedium != null) {
                    HBox row = createTableRow("猜数", "中等",
                            String.valueOf(bestMedium.getScore()),
                            bestMedium.getCreatedAt() != null ? bestMedium.getCreatedAt() : "-", false);
                    row.setStyle(row.getStyle() + "-fx-background-color: -bg-primary;");
                    table.getChildren().add(row);
                    hasRecords = true;
                }
                if (bestHard != null) {
                    table.getChildren().add(createTableRow("猜数", "困难",
                            String.valueOf(bestHard.getScore()),
                            bestHard.getCreatedAt() != null ? bestHard.getCreatedAt() : "-", false));
                    hasRecords = true;
                }

                // 获取翻牌游戏的最佳成绩
                GameRecord bestFlipEasy = svc.getBestScore(uid, "memoryFlip", "easy");
                GameRecord bestFlipHard = svc.getBestScore(uid, "memoryFlip", "hard");

                if (bestFlipEasy != null) {
                    String flipTime = bestFlipEasy.getTimeSeconds() != null && bestFlipEasy.getTimeSeconds() > 0
                            ? bestFlipEasy.getTimeSeconds() + "秒" : "—";
                    table.getChildren().add(createTableRow("翻牌", "4x4",
                            bestFlipEasy.getScore() + "次 · " + flipTime,
                            bestFlipEasy.getCreatedAt() != null ? bestFlipEasy.getCreatedAt() : "-", false));
                    hasRecords = true;
                }
                if (bestFlipHard != null) {
                    String flipTime = bestFlipHard.getTimeSeconds() != null && bestFlipHard.getTimeSeconds() > 0
                            ? bestFlipHard.getTimeSeconds() + "秒" : "—";
                    HBox row = createTableRow("翻牌", "6x6",
                            bestFlipHard.getScore() + "次 · " + flipTime,
                            bestFlipHard.getCreatedAt() != null ? bestFlipHard.getCreatedAt() : "-", false);
                    row.setStyle(row.getStyle() + "-fx-background-color: -bg-primary;");
                    table.getChildren().add(row);
                    hasRecords = true;
                }
            }
        } catch (Exception ignored) {}

        if (!hasRecords) {
            HBox emptyRow = createTableRow("-", "-", "-", "-", false);
            emptyRow.setStyle("-fx-padding: 24;");
            table.getChildren().add(emptyRow);
        }

        bestRecordsSection.getChildren().add(table);
        contentArea.getChildren().add(bestRecordsSection);

        // 成就徽章区域
        VBox badgesSection = new VBox(16);
        badgesSection.setPadding(new Insets(28, 0, 0, 0));

        Label badgeTitle = new Label("成就徽章");
        badgeTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        badgesSection.getChildren().add(badgeTitle);

        // 徽章网格 (4列 on wide)
        GridPane badgeGrid = new GridPane();
        badgeGrid.setHgap(12);
        badgeGrid.setVgap(12);
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(25);
            badgeGrid.getColumnConstraints().add(cc);
        }

        // 已解锁徽章示例
        badgeGrid.add(createBadgeCard("🎯", "初来乍到", "首次进入闲云馆", "-primary", true, "2025-01-01"), 0, 0);
        badgeGrid.add(createBadgeCard("🔢", "数字达人", "猜数游戏胜3局", "-accent", true, "2025-01-02"), 1, 0);
        // 未解锁徽章
        badgeGrid.add(createBadgeCard("🔒", "连胜之王", "连续获胜5次", "-border", false, null), 2, 0);
        badgeGrid.add(createBadgeCard("🔒", "全勤玩家", "连续打卡7天", "-border", false, null), 3, 0);

        badgesSection.getChildren().add(badgeGrid);
        contentArea.getChildren().add(badgesSection);
    }

    private HBox createRecordStatCard(String icon, String value, String caption,
                                       String iconBgColor, String iconColor) {
        HBox card = new HBox(14);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 10; " +
                "-fx-border-color: -border-light; -fx-border-width: 1; -fx-border-radius: 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);");

        // 图标盒
        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(48, 48);
        iconBox.setMinSize(48, 48);
        iconBox.setMaxSize(48, 48);
        iconBox.setStyle("-fx-background-radius: 10; -fx-background-color: " + iconBgColor + ";");
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 22px;");
        iconBox.getChildren().add(iconLabel);
        card.getChildren().add(iconBox);

        // 文字区
        VBox textBox = new VBox(2);
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label captionLabel = new Label(caption);
        captionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        textBox.getChildren().addAll(valueLabel, captionLabel);
        card.getChildren().add(textBox);

        return card;
    }

    private HBox createTableRow(String col1, String col2, String col3, String col4, boolean isHeader) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        String rowStyle = isHeader
                ? "-fx-padding: 12 16; -fx-background-color: -bg-primary; -fx-background-radius: 10 10 0 0; "
                : "-fx-padding: 12 16; ";
        row.setStyle(rowStyle);

        String cellStyle = isHeader
                ? "-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;"
                : "-fx-font-size: 13px; -fx-text-fill: -text-primary;";

        Label c1 = new Label(col1);
        c1.setStyle(cellStyle);
        c1.setPrefWidth(120);

        Label c2 = new Label(col2);
        c2.setStyle(cellStyle);
        c2.setPrefWidth(80);

        Label c3 = new Label(col3);
        c3.setStyle(cellStyle);
        HBox.setHgrow(c3, Priority.ALWAYS);

        Label c4 = new Label(col4);
        c4.setStyle(cellStyle);
        c4.setPrefWidth(160);

        row.getChildren().addAll(c1, c2, c3, c4);
        return row;
    }

    private VBox createBadgeCard(String icon, String name, String desc,
                                  String borderColor, boolean unlocked, String date) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setAlignment(Pos.CENTER);

        if (unlocked) {
            card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 10; " +
                    "-fx-border-color: " + borderColor + "; -fx-border-width: 2; -fx-border-radius: 10;");
        } else {
            card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 10; " +
                    "-fx-border-color: -border; -fx-border-width: 1; -fx-border-radius: 10; " +
                    "-fx-opacity: 0.5;");
        }

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 28px;");
        if (!unlocked) {
            iconLabel.setStyle("-fx-font-size: 28px; -fx-opacity: 0.5;");
        }

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Label descLabel = new Label(desc);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");
        descLabel.setWrapText(true);

        if (unlocked && date != null) {
            Label dateLabel = new Label(date);
            dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-tertiary;");
            card.getChildren().addAll(iconLabel, nameLabel, descLabel, dateLabel);
        } else if (!unlocked) {
            Label lockLabel = new Label("未解锁");
            lockLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-tertiary;");
            card.getChildren().addAll(iconLabel, nameLabel, descLabel, lockLabel);
        } else {
            card.getChildren().addAll(iconLabel, nameLabel, descLabel);
        }

        return card;
    }
}
