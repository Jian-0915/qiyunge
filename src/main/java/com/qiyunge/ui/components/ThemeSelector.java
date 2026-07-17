package com.qiyunge.ui.components;

import com.qiyunge.app.ThemeService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class ThemeSelector extends HBox {

    public ThemeSelector(ThemeService themeService, javafx.scene.Scene scene) {
        this.setSpacing(12);
        this.setAlignment(Pos.CENTER_LEFT);
        this.setPadding(new Insets(8, 0, 8, 0));

        for (ThemeService.Theme theme : themeService.getAllThemes()) {
            VBox option = new VBox(4);
            option.setAlignment(Pos.CENTER);

            Region colorDot = new Region();
            colorDot.setPrefSize(32, 32);
            colorDot.getStyleClass().add("theme-option");

            // Set different colors for each theme
            String color = switch (theme) {
                case MORNING -> "#5B8DEF";
                case BAMBOO -> "#2D8B56";
                case DUSK -> "#A78BFA";
            };
            colorDot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 50%;");

            Label nameLabel = new Label(theme.getDisplayName());
            nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");

            option.getChildren().addAll(colorDot, nameLabel);

            if (theme == themeService.getCurrentTheme()) {
                colorDot.getStyleClass().add("selected");
            }

            option.setOnMouseClicked(e -> {
                themeService.setTheme(theme, scene);
                // Update visual selection
                getChildren().forEach(child -> {
                    if (child instanceof VBox vb) {
                        vb.getChildren().forEach(n -> n.getStyleClass().remove("selected"));
                    }
                });
                colorDot.getStyleClass().add("selected");
            });

            this.getChildren().add(option);
        }
    }
}
