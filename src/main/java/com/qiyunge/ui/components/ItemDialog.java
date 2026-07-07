package com.qiyunge.ui.components;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

/**
 * 通用的"名称 + 描述"输入对话框。
 * 可用于创建曲笺、图集等场景。
 */
public class ItemDialog extends Dialog<ButtonType> {

    private final TextField nameField;
    private final TextArea descField;

    /**
     * @param title         对话框标题
     * @param nameLabel     名称标签文字（如"曲笺名称"、"图集名称"）
     * @param namePrompt    名称输入提示文字
     * @param defaultName   默认名称
     * @param confirmBtnText 确认按钮文字（如"创建"）
     */
    public ItemDialog(String title, String nameLabel, String namePrompt, String defaultName, String confirmBtnText) {
        setTitle(title);
        initModality(Modality.APPLICATION_MODAL);
        setHeaderText(null);
        setGraphic(null);

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        Label lbl = new Label(nameLabel);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        nameField = new TextField(defaultName != null ? defaultName : "");
        nameField.setPromptText(namePrompt);

        Label descLabel = new Label("描述（可选）");
        descLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        descField = new TextArea();
        descField.setPromptText("简要描述");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);

        content.getChildren().addAll(lbl, nameField, descLabel, descField);
        getDialogPane().setContent(content);

        ButtonType createBtn = new ButtonType(confirmBtnText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(createBtn, cancelBtn);

        nameField.requestFocus();
    }

    public String getItemName() { return nameField.getText().trim(); }
    public String getItemDescription() { return descField.getText().trim(); }
}
