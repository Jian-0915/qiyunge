package com.qiyunge.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class WindowTitleBar extends HBox {

    private double xOffset = 0;
    private double yOffset = 0;
    private final Stage stage;

    public WindowTitleBar(Stage stage) {
        this.stage = stage;
        this.getStyleClass().add("window-title-bar");
        this.setPrefHeight(36);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(0, 8, 0, 12));

        // Drag to move window
        this.setOnMousePressed(this::onMousePressed);
        this.setOnMouseDragged(this::onMouseDragged);

        // App title
        Label titleLabel = new Label("栖云阁");
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-font-weight: 500;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Window control buttons
        Label minimizeBtn = new Label("─");
        minimizeBtn.getStyleClass().add("win-btn");
        minimizeBtn.getStyleClass().add("win-btn-minimize");
        minimizeBtn.setOnMouseClicked(e -> stage.setIconified(true));

        Label maximizeBtn = new Label("□");
        maximizeBtn.getStyleClass().add("win-btn");
        maximizeBtn.getStyleClass().add("win-btn-maximize");
        maximizeBtn.setOnMouseClicked(e -> {
            if (stage.isMaximized()) {
                stage.setMaximized(false);
            } else {
                stage.setMaximized(true);
            }
        });

        Label closeBtn = new Label("✕");
        closeBtn.getStyleClass().add("win-btn");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnMouseClicked(e -> {
            javafx.application.Platform.exit();
        });

        this.getChildren().addAll(titleLabel, spacer, minimizeBtn, maximizeBtn, closeBtn);
    }

    private void onMousePressed(MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    private void onMouseDragged(MouseEvent event) {
        if (stage.isMaximized()) {
            stage.setMaximized(false);
        }
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}
