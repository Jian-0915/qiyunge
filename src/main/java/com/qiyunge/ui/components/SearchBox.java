package com.qiyunge.ui.components;

import javafx.scene.control.TextField;

public class SearchBox extends TextField {

    public SearchBox() {
        super();
        this.getStyleClass().add("search-box");
        this.setPromptText("搜索...");
        this.setPrefWidth(240);
    }

    public SearchBox(String prompt) {
        super();
        this.getStyleClass().add("search-box");
        this.setPromptText(prompt);
        this.setPrefWidth(240);
    }

    public SearchBox(String prompt, double width) {
        super();
        this.getStyleClass().add("search-box");
        this.setPromptText(prompt);
        this.setPrefWidth(width);
    }
}
