package com.qiyunge.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class ImageCard extends VBox {

    public ImageCard(String title, String category) {
        this.getStyleClass().add("image-card");
        this.setSpacing(4);

        // Image placeholder
        Region imagePlaceholder = new Region();
        imagePlaceholder.setStyle("-fx-background-color: -bg-tertiary; -fx-background-radius: 8px 8px 0 0; -fx-min-height: 160px; -fx-pref-height: 160px;");
        VBox.setVgrow(imagePlaceholder, Priority.ALWAYS);

        // Info area
        VBox infoArea = new VBox(2);
        infoArea.setPadding(new Insets(8, 10, 10, 10));

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Label categoryLabel = new Label(category);
        categoryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");

        infoArea.getChildren().addAll(titleLabel, categoryLabel);

        this.getChildren().addAll(imagePlaceholder, infoArea);
    }
}
