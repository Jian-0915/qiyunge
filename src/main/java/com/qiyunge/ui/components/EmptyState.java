package com.qiyunge.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * 通用空状态组件。
 * 提供图标 + 标题 + 消息的标准空状态展示。
 */
public class EmptyState extends VBox {

    /**
     * 创建空状态组件。
     */
    public EmptyState(String icon, String title, String message) {
        this(icon, title, message, false);
    }

    /**
     * 创建空状态组件（可控制是否自动换行）。
     */
    public EmptyState(String icon, String title, String message, boolean wrapText) {
        this.getStyleClass().add("empty-state");
        this.setAlignment(Pos.CENTER);
        this.setSpacing(12);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 48px; -fx-opacity: 0.4;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: -text-secondary;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary; -fx-line-spacing: 6;");
        msgLabel.setWrapText(true);
        if (wrapText) {
            msgLabel.setMaxWidth(300);
            msgLabel.setAlignment(Pos.CENTER);
        }

        this.getChildren().addAll(iconLabel, titleLabel, msgLabel);
    }
}
