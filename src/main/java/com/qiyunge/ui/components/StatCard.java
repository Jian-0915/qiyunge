package com.qiyunge.ui.components;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class StatCard extends VBox {

    private final Label valueLabel;
    private final Label titleLabel;

    public StatCard(String title, String value, String icon) {
        this.getStyleClass().add("stat-card");
        this.setSpacing(8);
        this.setPadding(new Insets(20));

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.getStyleClass().add("stat-icon");
        iconLabel.setStyle("-fx-font-size: 24px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        headerRow.getChildren().addAll(iconLabel, spacer);

        valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stat-value");
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-label");
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        this.getChildren().addAll(headerRow, valueLabel, titleLabel);
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }
}
