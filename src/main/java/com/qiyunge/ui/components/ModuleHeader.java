package com.qiyunge.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * 通用模块头部组件：Logo + 标题 + 副标题。
 * 各模块（音乐、图库等）复用此组件，通过参数自定义。
 */
public class ModuleHeader extends HBox {

    public ModuleHeader(String icon, String title, String subtitle) {
        this.setPrefHeight(60);
        this.setMinHeight(60);
        this.setMaxHeight(60);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(0, 24, 0, 24));
        this.setSpacing(16);
        this.setStyle("-fx-background-color: -bg-primary; -fx-border-color: -border-light; -fx-border-width: 0 0 1px 0;");

        HBox titleGroup = new HBox(12);
        titleGroup.setAlignment(Pos.CENTER_LEFT);

        Label logoLabel = new Label(icon);
        logoLabel.setStyle("-fx-font-size: 22px; -fx-text-fill: -primary;");

        VBox titleText = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        titleText.getChildren().addAll(titleLabel, subtitleLabel);

        titleGroup.getChildren().addAll(logoLabel, titleText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.getChildren().addAll(titleGroup, spacer);
    }
}
