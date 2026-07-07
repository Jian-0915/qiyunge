package com.qiyunge.ui.entertainment;

import com.qiyunge.app.AppContext;
import com.qiyunge.ui.components.*;
import javafx.geometry.Insets;
import javafx.scene.layout.*;

public class EntertainmentView extends VBox {

    public EntertainmentView(AppContext appContext) {
        this.setPadding(new Insets(24));
        this.setSpacing(20);
        this.getStyleClass().add("entertainment-view");

        PageHeader header = new PageHeader("娱乐", "放松一下，享受乐趣");

        EmptyState emptyState = new EmptyState("♦", "敬请期待", "娱乐功能正在开发中，敬请期待。");

        this.getChildren().addAll(header, emptyState);
    }
}
