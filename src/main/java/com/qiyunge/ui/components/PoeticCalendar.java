package com.qiyunge.ui.components;

import com.qiyunge.infrastructure.util.LunarCalendar;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 诗意日历组件：点击切换公历/农历显示。
 */
public class PoeticCalendar extends HBox {

    private boolean showLunar = false; // default: solar
    private final Label iconLabel;
    private final Label line1Label;
    private final Label line2Label;
    private final Label swapLabel;

    public PoeticCalendar() {
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(6, 14, 6, 12));
        this.setSpacing(8);
        this.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border-light; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand;"
        );

        // Icon
        iconLabel = new Label("📅");
        iconLabel.setStyle("-fx-font-size: 15px;");

        // Text area
        VBox textBox = new VBox(1);
        textBox.setAlignment(Pos.CENTER_LEFT);

        line1Label = new Label();
        line1Label.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        line2Label = new Label();
        line2Label.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-secondary;");

        textBox.getChildren().addAll(line1Label, line2Label);

        // Swap icon
        swapLabel = new Label("⇄");
        swapLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-tertiary;");

        this.getChildren().addAll(iconLabel, textBox, swapLabel);

        // Click to toggle
        this.setOnMouseClicked(e -> {
            showLunar = !showLunar;
            updateDisplay();
        });

        // Hover effect
        this.setOnMouseEntered(e -> {
            this.setStyle(
                "-fx-background-color: -bg-input; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: -border-light; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 10px; " +
                "-fx-cursor: hand;"
            );
        });
        this.setOnMouseExited(e -> {
            this.setStyle(
                "-fx-background-color: -bg-card; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: -border-light; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 10px; " +
                "-fx-cursor: hand;"
            );
        });

        // Initial display
        updateDisplay();

        // Auto-update every minute
        Timeline timeline = new Timeline(new KeyFrame(Duration.minutes(1), e -> updateDisplay()));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }

    private void updateDisplay() {
        LocalDate today = LocalDate.now();
        LunarCalendar.LunarDate lunar = LunarCalendar.solarToLunar(today);
        String solarTerm = LunarCalendar.getCurrentSolarTerm(today);
        boolean isTermDay = LunarCalendar.isSolarTermDay(today);

        if (showLunar) {
            iconLabel.setText("🌙");
            line1Label.setText(lunar.formatShort());
            line1Label.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -primary;");
            String termText = isTermDay ? "今日" + solarTerm : solarTerm;
            line2Label.setText(lunar.getShengxiao() + "年 · " + termText);
        } else {
            iconLabel.setText("📅");
            String weekDay = getChineseWeekDay(today.getDayOfWeek());
            line1Label.setText(today.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)) + " " + weekDay);
            line1Label.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
            String termText = isTermDay ? "今日" + solarTerm : solarTerm;
            line2Label.setText(termText + " · " + lunar.getYearGanZhi() + lunar.getShengxiao() + "年");
        }
    }

    private String getChineseWeekDay(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "周一";
            case TUESDAY -> "周二";
            case WEDNESDAY -> "周三";
            case THURSDAY -> "周四";
            case FRIDAY -> "周五";
            case SATURDAY -> "周六";
            case SUNDAY -> "周日";
        };
    }
}
