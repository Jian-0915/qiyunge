package com.qiyunge.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class PageHeader extends VBox {

    public PageHeader(String title, String subtitle) {
        this.getStyleClass().add("page-header");
        this.setSpacing(4);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        if (subtitle != null && !subtitle.isEmpty()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
            this.getChildren().addAll(titleLabel, subtitleLabel);
        } else {
            this.getChildren().add(titleLabel);
        }
    }
}
