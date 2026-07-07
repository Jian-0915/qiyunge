package com.qiyunge.ui.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public class SideNavItem extends HBox {

    private final Label iconLabel;
    private final Label textLabel;

    public SideNavItem(String text, String iconName) {
        this.getStyleClass().add("side-nav-item");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(12);

        // Icon placeholder - using Unicode symbols for now
        iconLabel = new Label(getIcon(iconName));
        iconLabel.getStyleClass().add("nav-icon");
        iconLabel.setStyle("-fx-font-size: 16px;");

        textLabel = new Label(text);
        textLabel.getStyleClass().add("nav-label");

        this.getChildren().addAll(iconLabel, textLabel);
    }

    private String getIcon(String name) {
        return switch (name) {
            case "dashboard" -> "◉";
            case "music" -> "♫";
            case "gallery" -> "▦";
            case "entertainment" -> "♦";
            case "profile" -> "○";
            case "admin" -> "▣";
            case "settings" -> "⚙";
            case "logout" -> "⏻";
            default -> "●";
        };
    }
}
