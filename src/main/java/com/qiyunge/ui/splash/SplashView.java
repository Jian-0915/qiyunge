package com.qiyunge.ui.splash;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

public class SplashView extends BorderPane {

    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final Label detailLabel;

    public SplashView() {
        setPrefSize(520, 340);
        setStyle("-fx-background-color: #0D1117; -fx-border-color: #1C2333; -fx-border-width: 1px; -fx-border-radius: 12px; -fx-background-radius: 12px;");

        VBox center = new VBox(20);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40, 50, 40, 50));

        // 应用名称
        Label titleLabel = new Label("栖 云 阁");
        titleLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: 700; -fx-text-fill: #E6EDF3; -fx-letter-spacing: 8px;");

        // 副标题
        Label subtitleLabel = new Label("云栖之阁，心归之处");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7D8590; -fx-letter-spacing: 2px;");

        // 进度条
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(420);
        progressBar.setStyle("-fx-accent: #5B8DEF; -fx-background-color: #21262D; -fx-background-radius: 4px; -fx-border-radius: 4px;");
        progressBar.setMaxHeight(6);

        // 状态文字
        statusLabel = new Label("正在初始化...");
        statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7D8590;");

        // 详情文字
        detailLabel = new Label("");
        detailLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #484F58;");
        detailLabel.setWrapText(true);
        detailLabel.setMaxWidth(420);

        center.getChildren().addAll(titleLabel, subtitleLabel, progressBar, statusLabel, detailLabel);

        setCenter(center);
    }

    public void updateProgress(double progress, String status) {
        progressBar.setProgress(progress);
        statusLabel.setText(status);
    }

    public void updateProgress(double progress, String status, String detail) {
        updateProgress(progress, status);
        detailLabel.setText(detail);
    }
}
