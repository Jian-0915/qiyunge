package com.qiyunge.ui.components;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class Toast {

    private final StackPane container;
    private final VBox toastBox;

    public Toast(StackPane rootContainer) {
        this.container = rootContainer;
        this.toastBox = new VBox();
        this.toastBox.setAlignment(Pos.TOP_RIGHT);
        this.toastBox.setPadding(new Insets(16));
        StackPane.setAlignment(toastBox, Pos.TOP_RIGHT);
        container.getChildren().add(toastBox);
    }

    public void show(String message, Type type, int durationMs) {
        HBox toastItem = new HBox(10);
        toastItem.setAlignment(Pos.CENTER_LEFT);
        toastItem.getStyleClass().add("toast");
        toastItem.getStyleClass().add(type.cssClass);
        toastItem.setPadding(new Insets(12, 20, 12, 20));
        toastItem.setMaxWidth(360);

        String icon = switch (type) {
            case SUCCESS -> "✓";
            case ERROR -> "✕";
            case WARNING -> "⚠";
            case INFO -> "ℹ";
        };

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");

        Label msgLabel = new Label(message);
        msgLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary; -fx-wrap-text: true;");
        HBox.setHgrow(msgLabel, Priority.ALWAYS);

        toastItem.getChildren().addAll(iconLabel, msgLabel);
        toastBox.getChildren().add(toastItem);

        // Auto remove
        PauseTransition delay = new PauseTransition(Duration.millis(durationMs));
        delay.setOnFinished(e -> {
            toastItem.setOpacity(0);
            toastBox.getChildren().remove(toastItem);
        });
        delay.play();
    }

    public void showSuccess(String message) { show(message, Type.SUCCESS, 3000); }
    public void showError(String message) { show(message, Type.ERROR, 4000); }
    public void showWarning(String message) { show(message, Type.WARNING, 3500); }
    public void showInfo(String message) { show(message, Type.INFO, 3000); }

    public enum Type {
        SUCCESS("success"), ERROR("error"), WARNING("warning"), INFO("info");
        final String cssClass;
        Type(String cssClass) { this.cssClass = cssClass; }
    }
}
