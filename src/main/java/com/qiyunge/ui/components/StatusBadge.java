package com.qiyunge.ui.components;

import javafx.scene.control.Label;

public class StatusBadge extends Label {

    public StatusBadge(String text, String status) {
        super(text);
        this.getStyleClass().add("status-badge");
        this.getStyleClass().add(status);
    }
}
