package com.qiyunge.ui.components;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class UserAvatar extends StackPane {

    private Label initialLabel;

    public UserAvatar(String username, double size) {
        this.getStyleClass().add("user-avatar");
        this.setPrefSize(size, size);
        this.setStyle("-fx-background-color: -primary-light; -fx-background-radius: 50%;");

        String initial = username != null && !username.isEmpty() ? username.substring(0, 1) : "?";
        initialLabel = new Label(initial);
        initialLabel.setStyle("-fx-font-size: " + (size * 0.4) + "px; -fx-font-weight: 700; -fx-text-fill: -primary;");
        this.getChildren().add(initialLabel);
    }

    public void update(String displayName) {
        if (initialLabel != null && displayName != null && !displayName.isEmpty()) {
            initialLabel.setText(displayName.substring(0, 1));
        }
    }
}
