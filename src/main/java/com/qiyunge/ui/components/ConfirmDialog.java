package com.qiyunge.ui.components;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * 确认对话框：提供是/否确认操作。
 * 实际使用时建议通过 DialogService 调用。
 */
public class ConfirmDialog {

    /**
     * 显示确认对话框并返回用户选择。
     */
    public static boolean show(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initStyle(javafx.stage.StageStyle.UTILITY);
        // 居中显示
        alert.getDialogPane().setStyle("-fx-font-size: 13px;");
        ButtonType result = alert.showAndWait().orElse(ButtonType.NO);
        return result == ButtonType.YES;
    }
}
