package com.qiyunge.ui.components;

import javafx.scene.control.Button;

public class AppButton extends Button {

    public enum Style {
        PRIMARY, SECONDARY, DANGER, OUTLINE, GHOST
    }

    public AppButton(String text) {
        super(text);
        this.getStyleClass().add("app-button");
    }

    public AppButton(String text, Style style) {
        super(text);
        this.getStyleClass().add("app-button");
        applyStyle(style);
    }

    public AppButton(String text, Style style, Runnable onAction) {
        this(text, style);
        this.setOnAction(e -> onAction.run());
    }

    private void applyStyle(Style style) {
        switch (style) {
            case SECONDARY -> this.getStyleClass().add("secondary");
            case DANGER -> this.getStyleClass().add("danger");
            case OUTLINE -> this.getStyleClass().add("outline");
            case GHOST -> this.getStyleClass().add("ghost");
            default -> {} // PRIMARY is default
        }
    }
}
