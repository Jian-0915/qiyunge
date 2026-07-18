package com.qiyunge.ui.pomodoro;

import com.qiyunge.app.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.function.Consumer;

public class PomodoroStatsView extends VBox {

    private final AppContext appContext;
    private final PomodoroViewModel vm;
    private final Runnable onBack;
    private final Consumer<String> onSwitchTab;
    private String currentTab;

    public PomodoroStatsView(AppContext appContext, String initialTab, Runnable onBack, Consumer<String> onSwitchTab) {
        this.appContext = appContext;
        this.vm = new PomodoroViewModel(appContext);
        this.onBack = onBack;
        this.onSwitchTab = onSwitchTab;
        this.currentTab = initialTab != null ? initialTab : "weekly";

        this.setPadding(new Insets(24));
        this.setSpacing(16);
        this.setStyle("-fx-background-color: transparent;");

        buildUI();
    }

    private void buildUI() {
        this.getChildren().clear();
        this.getChildren().add(buildBackButton());

        switch (currentTab) {
            case "weekly" -> {
                vm.loadWeeklyStats();
                this.getChildren().add(buildWeeklyView());
            }
            case "monthly" -> {
                vm.loadMonthlyStats();
                this.getChildren().add(buildMonthlyView());
            }
            case "overview" -> {
                vm.loadTotalStats();
                this.getChildren().add(buildOverviewView());
            }
        }
    }

    // ========== 返回按钮 ==========
    private Node buildBackButton() {
        HBox backBtn = new HBox(6);
        backBtn.setAlignment(Pos.CENTER_LEFT);
        backBtn.setPadding(new Insets(0, 0, 4, 0));
        backBtn.setStyle("-fx-cursor: hand;");

        Label icon = new Label("←");
        icon.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");

        Label text = new Label("返回专注计时");
        text.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");

        backBtn.getChildren().addAll(icon, text);

        backBtn.setOnMouseClicked(e -> {
            if (onBack != null) onBack.run();
        });

        backBtn.setOnMouseEntered(e -> {
            icon.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-primary;");
            text.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");
        });
        backBtn.setOnMouseExited(e -> {
            icon.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
            text.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
        });

        return backBtn;
    }

    // ========== 周省 (周统计) ==========
    private Node buildWeeklyView() {
        VBox content = new VBox(16);

        // 页面标题
        VBox header = new VBox(4);
        Label title = new Label("周省");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label subtitle = new Label("本周专注分布");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        header.getChildren().addAll(title, subtitle);

        // 本周总结卡片
        VBox summaryCard = new VBox(4);
        summaryCard.setPadding(new Insets(24));
        summaryCard.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16; " +
            "-fx-border-color: -border-light; -fx-border-radius: 16; -fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);");

        Label summaryLabel = new Label("本周专注");
        summaryLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary; -fx-font-weight: 500;");

        Label summaryValue = new Label();
        summaryValue.setStyle("-fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: -primary;");
        summaryValue.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(() -> {
                int total = 0;
                for (Number num : vm.getWeeklyValues()) {
                    total += num.intValue();
                }
                return vm.formatDurationDisplay(total);
            }, vm.getWeeklyValues())
        );

        Label summarySub = new Label();
        summarySub.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        summarySub.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(() -> {
                int totalTomatoes = 0;
                for (Number num : vm.getWeeklyValues()) {
                    totalTomatoes += num.intValue() / 25;
                }
                return "完成 " + totalTomatoes + " 个番茄";
            }, vm.getWeeklyValues())
        );

        summaryCard.getChildren().addAll(summaryLabel, summaryValue, summarySub);

        // 柱状图卡片
        VBox chartCard = new VBox(24);
        chartCard.setPadding(new Insets(24));
        chartCard.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16; " +
            "-fx-border-color: -border-light; -fx-border-radius: 16; -fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);");

        Label chartTitle = new Label("每日专注");
        chartTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        // 柱状图区域
        StackPane chartArea = buildWeeklyBarChart();

        chartCard.getChildren().addAll(chartTitle, chartArea);

        content.getChildren().addAll(header, summaryCard, chartCard);
        return content;
    }

    private StackPane buildWeeklyBarChart() {
        StackPane chartArea = new StackPane();
        chartArea.setPadding(new Insets(0, 0, 0, 28));

        VBox chartContent = new VBox(0);

        // Y轴标签 + 柱状图
        HBox chartWithYAxis = new HBox(0);

        // Y轴标签
        VBox yLabels = new VBox(0);
        yLabels.setAlignment(Pos.TOP_RIGHT);
        yLabels.setPrefWidth(24);
        String[] yTexts = {"4h", "3h", "2h", "1h", "0h"};
        for (int i = 0; i < yTexts.length; i++) {
            Label lbl = new Label(yTexts[i]);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
            lbl.setPrefHeight(i == 0 ? 0 : 45);
            lbl.setAlignment(i == 0 ? Pos.TOP_RIGHT : Pos.CENTER_RIGHT);
            yLabels.getChildren().add(lbl);
        }

        // 柱状图区
        VBox barsArea = new VBox(0);
        barsArea.setPrefHeight(180);

        // 参考线 + 柱状
        StackPane barsPane = new StackPane();
        barsPane.setPrefHeight(180);

        // 参考线
        VBox refLines = new VBox(0);
        refLines.setPrefHeight(180);
        for (int i = 0; i < 5; i++) {
            Region line = new Region();
            line.setPrefHeight(45);
            if (i == 0) {
                line.setStyle("-fx-border-color: -border-light; -fx-border-width: 0 0 1 0;");
            } else if (i < 4) {
                line.setStyle("-fx-border-color: -border; -fx-border-style: dashed; -fx-border-width: 0 0 1 0;");
            }
            refLines.getChildren().add(line);
        }

        // 柱状图条
        HBox barsRow = new HBox(8);
        barsRow.setPadding(new Insets(0, 4, 0, 4));
        barsRow.setAlignment(Pos.BOTTOM_CENTER);

        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int todayIdx = (LocalDate.now().getDayOfWeek().getValue() - 1 + 7) % 7;

        for (int i = 0; i < 7; i++) {
            final int idx = i;
            StackPane barCol = new StackPane();
            barCol.setAlignment(Pos.BOTTOM_CENTER);
            HBox.setHgrow(barCol, Priority.ALWAYS);

            VBox barContent = new VBox(6);
            barContent.setAlignment(Pos.BOTTOM_CENTER);

            // 高度计算
            double maxMinutes = 240;
            int value = 0;
            if (idx < vm.getWeeklyValues().size()) {
                value = vm.getWeeklyValues().get(idx).intValue();
            }
            double pct = Math.min(value / maxMinutes, 1.0);
            double height = 180 * pct;

            Label valueLabel = new Label();
            if (height > 20) {
                valueLabel.setText(vm.formatDurationDisplay(value));
                valueLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
            }

            Region bar = new Region();
            bar.setPrefWidth(36);
            bar.setPrefHeight(Math.max(height, 2));
            String color = idx == todayIdx ? "-primary-hover" : "-primary";
            bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4 4 0 0;");

            barContent.getChildren().addAll(valueLabel, bar);
            barCol.getChildren().add(barContent);
            barsRow.getChildren().add(barCol);
        }

        barsPane.getChildren().addAll(refLines, barsRow);
        barsArea.getChildren().add(barsPane);

        // X轴标签
        HBox xLabels = new HBox(8);
        xLabels.setPadding(new Insets(12, 4, 0, 4));
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(dayNames[i]);
            if (i == todayIdx) {
                dayLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -primary; -fx-font-weight: 500;");
            } else {
                dayLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
            }
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(dayLabel, Priority.ALWAYS);
            xLabels.getChildren().add(dayLabel);
        }

        chartContent.getChildren().addAll(barsArea, xLabels);
        chartWithYAxis.getChildren().addAll(yLabels, chartContent);
        chartArea.getChildren().add(chartWithYAxis);

        return chartArea;
    }

    // ========== 月览 (月热力图) ==========
    private Node buildMonthlyView() {
        VBox content = new VBox(16);

        VBox header = new VBox(4);
        Label title = new Label("月览");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label subtitle = new Label("本月专注热力图");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        header.getChildren().addAll(title, subtitle);

        // 3个统计卡片
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(12);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.33);
            statsGrid.getColumnConstraints().add(cc);
        }

        VBox monthFocusCard = createSmallStatCard("45小时", "本月专注");
        VBox daysCard = createSmallStatCard("18天", "专注天数");
        VBox avgCard = createSmallStatCard("5.2个", "日均番茄");

        statsGrid.add(monthFocusCard, 0, 0);
        statsGrid.add(daysCard, 1, 0);
        statsGrid.add(avgCard, 2, 0);

        // 热力图卡片
        VBox heatmapCard = new VBox(20);
        heatmapCard.setPadding(new Insets(24));
        heatmapCard.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16; " +
            "-fx-border-color: -border-light; -fx-border-radius: 16; -fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);");

        LocalDate now = LocalDate.now();
        String monthTitle = now.getYear() + "年" + now.getMonthValue() + "月";
        Label hmTitle = new Label(monthTitle);
        hmTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        HBox heatmapArea = buildMonthlyHeatmap(now);

        // 图例
        HBox legend = new HBox(4);
        legend.setAlignment(Pos.CENTER_RIGHT);
        Label lessLbl = new Label("少");
        lessLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");

        String[] levels = {
            "-bg-tertiary",
            "rgba(91,141,239,0.2)",
            "rgba(91,141,239,0.4)",
            "rgba(91,141,239,0.6)",
            "rgba(91,141,239,0.9)"
        };
        legend.getChildren().add(lessLbl);
        for (String level : levels) {
            Region r = new Region();
            r.setPrefSize(14, 14);
            r.setStyle("-fx-background-color: " + level + "; -fx-background-radius: 2;");
            legend.getChildren().add(r);
        }
        Label moreLbl = new Label("多");
        moreLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
        legend.getChildren().add(moreLbl);

        heatmapCard.getChildren().addAll(hmTitle, heatmapArea, legend);

        content.getChildren().addAll(header, statsGrid, heatmapCard);
        return content;
    }

    private HBox buildMonthlyHeatmap(LocalDate now) {
        HBox container = new HBox(3);

        // 星期标签列
        VBox weekLabels = new VBox(2);
        weekLabels.setPadding(new Insets(0, 6, 0, 0));
        String[] weekNames = {"一", "二", "三", "四", "五", "六", "日"};
        for (String w : weekNames) {
            Label lbl = new Label(w);
            lbl.setPrefSize(14, 14);
            lbl.setAlignment(Pos.CENTER_RIGHT);
            lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
            weekLabels.getChildren().add(lbl);
        }

        // 热力图网格
        HBox weeksContainer = new HBox(3);
        int daysInMonth = now.lengthOfMonth();
        int firstDayOfWeek = (now.withDayOfMonth(1).getDayOfWeek().getValue() - 1 + 7) % 7;
        int totalWeeks = (int) Math.ceil((firstDayOfWeek + daysInMonth) / 7.0);

        for (int w = 0; w < totalWeeks; w++) {
            VBox weekCol = new VBox(2);
            for (int d = 0; d < 7; d++) {
                int dayNum = w * 7 + d - firstDayOfWeek + 1;
                Region cell = new Region();
                cell.setPrefSize(14, 14);

                if (dayNum >= 1 && dayNum <= daysInMonth) {
                    int valIdx = dayNum - 1;
                    int value = valIdx < vm.getMonthlyHeatmap().size()
                        ? vm.getMonthlyHeatmap().get(valIdx).intValue() : 0;

                    String bgColor;
                    if (value == 0) {
                        bgColor = "-bg-tertiary";
                    } else if (value <= 25) {
                        bgColor = "rgba(91,141,239,0.2)";
                    } else if (value <= 50) {
                        bgColor = "rgba(91,141,239,0.4)";
                    } else if (value <= 100) {
                        bgColor = "rgba(91,141,239,0.6)";
                    } else {
                        bgColor = "rgba(91,141,239,0.9)";
                    }
                    cell.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 2;");
                } else {
                    cell.setStyle("-fx-background-color: transparent;");
                }

                weekCol.getChildren().add(cell);
            }
            weeksContainer.getChildren().add(weekCol);
        }

        container.getChildren().addAll(weekLabels, weeksContainer);
        return container;
    }

    private VBox createSmallStatCard(String value, String label) {
        VBox card = new VBox(4);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 12; " +
            "-fx-border-color: -border-light; -fx-border-radius: 12; -fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);");

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");

        card.getChildren().addAll(valueLbl, labelLbl);
        return card;
    }

    // ========== 征程 (总览) ==========
    private Node buildOverviewView() {
        VBox content = new VBox(16);

        VBox header = new VBox(4);
        Label title = new Label("征程");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label subtitle = new Label("你的专注历程");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        header.getChildren().addAll(title, subtitle);

        // 渐变卡片 - 累计专注时长
        VBox heroCard = new VBox(8);
        heroCard.setAlignment(Pos.CENTER);
        heroCard.setPadding(new Insets(40));
        heroCard.setStyle("-fx-background-color: linear-gradient(to bottom right, -primary, -accent); -fx-background-radius: 16;");

        Label heroValue = new Label();
        heroValue.setStyle("-fx-font-size: 42px; -fx-font-weight: 700; -fx-text-fill: white;");
        heroValue.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> vm.formatDurationDisplay(vm.totalFocusMinutesProperty().get()),
                vm.totalFocusMinutesProperty()
            )
        );

        Label heroLabel = new Label("累计专注时长");
        heroLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: rgba(255,255,255,0.8);");

        heroCard.getChildren().addAll(heroValue, heroLabel);

        // 2x2 统计卡片
        GridPane detailGrid = new GridPane();
        detailGrid.setHgap(16);
        detailGrid.setVgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            detailGrid.getColumnConstraints().add(cc);
        }

        detailGrid.add(createOverviewStatCard("⏱", "累计番茄",
            String.valueOf(vm.totalCompletedPomodorosProperty().get()), "个", "-primary"), 0, 0);
        detailGrid.add(createOverviewStatCard("📅", "专注天数",
            "45", "天", "-primary"), 1, 0);
        detailGrid.add(createOverviewStatCard("🔥", "最长连续",
            String.valueOf(vm.longestStreakProperty().get()), "天", "-primary"), 0, 1);
        detailGrid.add(createOverviewStatCard("⚡", "当前连续",
            String.valueOf(vm.currentStreakProperty().get()), "天", "-accent"), 1, 1);

        // 里程碑时间线
        VBox timelineCard = new VBox(24);
        timelineCard.setPadding(new Insets(24));
        timelineCard.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 16; " +
            "-fx-border-color: -border-light; -fx-border-radius: 16; -fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);");

        Label tlTitle = new Label("笃行 · 里程碑");
        tlTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        VBox timeline = buildMilestoneTimeline();

        timelineCard.getChildren().addAll(tlTitle, timeline);

        content.getChildren().addAll(header, heroCard, detailGrid, timelineCard);
        return content;
    }

    private HBox createOverviewStatCard(String icon, String label, String value, String unit, String colorVar) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 12; " +
            "-fx-border-color: -border-light; -fx-border-radius: 12; -fx-border-width: 1; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);");

        StackPane iconBox = new StackPane();
        iconBox.setPrefSize(40, 40);
        iconBox.setStyle("-fx-background-radius: 10; -fx-background-color: " +
            (colorVar.equals("-accent") ? "rgba(20,184,166,0.1)" : "-primary-light") + ";");
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px;");
        iconBox.getChildren().add(iconLbl);

        VBox textBox = new VBox(2);
        Label valueLbl = new Label(value + unit);
        valueLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: " +
            (colorVar.equals("-accent") ? "#14B8A6" : colorVar) + ";");
        Label labelLbl = new Label(label);
        labelLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
        textBox.getChildren().addAll(valueLbl, labelLbl);

        card.getChildren().addAll(iconBox, textBox);
        return card;
    }

    private VBox buildMilestoneTimeline() {
        VBox timeline = new VBox(0);
        timeline.setPadding(new Insets(0, 0, 0, 24));
        timeline.setStyle("-border-color: -border; -border-width: 0 0 0 2;");

        String[][] milestones = {
            {"初试锋芒", "完成第一个番茄钟", "2026-01-01", "unlocked"},
            {"笃行不怠", "累计专注 10 小时", "2026-01-15", "unlocked"},
            {"七日一心", "连续专注 7 天", "2026-01-08", "unlocked"},
            {"金石可镂", "累计专注 100 小时", "2026-05-10", "unlocked"},
            {"磨杵成针", "累计专注 500 小时", "128 / 500", "locked"},
            {"百日筑基", "连续专注 100 天", "7 / 100", "locked"}
        };

        for (String[] m : milestones) {
            boolean isUnlocked = "unlocked".equals(m[3]);

            HBox item = new HBox(8);
            item.setPadding(new Insets(0, 0, 20, 0));

            StackPane dot = new StackPane();
            dot.setPrefSize(12, 12);
            dot.setTranslateX(-30);
            if (isUnlocked) {
                dot.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 9999; " +
                    "-fx-border-color: -primary; -fx-border-radius: 9999; -fx-border-width: 2;");
            } else {
                dot.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 9999; " +
                    "-fx-border-color: -text-tertiary; -fx-border-radius: 9999; -fx-border-width: 2; -fx-opacity: 0.5;");
            }

            VBox info = new VBox(2);
            String titleColor = isUnlocked ? "-text-primary" : "-text-tertiary";
            String descColor = isUnlocked ? "-text-secondary" : "-text-tertiary";

            Label titleLbl = new Label(m[0]);
            titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: " + titleColor + ";");
            Label descLbl = new Label(m[1]);
            descLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: " + descColor + ";");
            Label dateLbl = new Label(m[2]);
            dateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");

            info.getChildren().addAll(titleLbl, descLbl, dateLbl);
            if (!isUnlocked) {
                Label badge = new Label("未解锁");
                badge.setStyle("-fx-font-size: 11px; -fx-background-color: -bg-tertiary; " +
                    "-fx-text-fill: -text-tertiary; -fx-background-radius: 9999; -fx-padding: 2 8 2 8;");
                info.getChildren().add(badge);
            }

            item.getChildren().addAll(dot, info);

            StackPane itemWrap = new StackPane();
            itemWrap.setAlignment(Pos.CENTER_LEFT);
            itemWrap.getChildren().add(item);
            timeline.getChildren().add(itemWrap);
        }

        return timeline;
    }

    public void switchTab(String tab) {
        this.currentTab = tab;
        buildUI();
    }

    public void cleanup() {
        vm.cleanup();
    }
}
