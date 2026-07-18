package com.qiyunge.ui.pomodoro;

import com.qiyunge.app.ThemeService;
import com.qiyunge.domain.entity.PomodoroTask;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class PomodoroTaskDialog extends Dialog<ButtonType> {

    private final TextField titleField;
    private final ChoiceBox<String> tagSelect;
    private final Spinner<Integer> estimatedSpinner;

    public PomodoroTaskDialog(PomodoroTask task, ThemeService themeService) {
        setTitle(task == null ? "新建任务" : "编辑任务");
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setHeaderText(null);
        setGraphic(null);

        VBox content = new VBox(0);
        content.setPrefWidth(400);
        content.setPadding(new Insets(36));
        content.getStyleClass().add("dialog-card");
        content.setStyle("-fx-background-color: -bg-card-solid; " +
            "-fx-background-radius: 16; -fx-border-radius: 16; " +
            "-fx-border-color: -border-light; -fx-border-width: 0.5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 25, 0, 0, 8), " +
            "dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2);");

        Label titleLbl = new Label(task == null ? "新建任务" : "编辑任务");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Region divider = new Region();
        divider.getStyleClass().add("dialog-divider");
        VBox.setMargin(divider, new Insets(16, 0, 16, 0));

        content.getChildren().addAll(titleLbl, divider);

        HBox nameRow = createSettingRow();
        Label nameLabel = createFieldLabel("任务名称");
        titleField = new TextField(task != null ? task.getTitle() : "");
        titleField.setPromptText("输入任务名称");
        titleField.getStyleClass().add("form-input");
        titleField.setStyle("-fx-background-color: -bg-input; -fx-background-radius: 10; " +
            "-fx-border-color: -border; -fx-border-radius: 10; -fx-border-width: 1; " +
            "-fx-pref-height: 38; -fx-text-fill: -text-primary; -fx-font-size: 14px; -fx-padding: 0 12;");
        HBox.setHgrow(titleField, Priority.ALWAYS);
        nameRow.getChildren().addAll(nameLabel, titleField);

        HBox tagRow = createSettingRow();
        Label tagLabel = createFieldLabel("标签");
        tagSelect = new ChoiceBox<>();
        tagSelect.getItems().addAll("工作", "学习", "阅读", "其他");
        String currentTag = task != null && task.getTag() != null ? task.getTag() : "工作";
        if (!tagSelect.getItems().contains(currentTag)) {
            tagSelect.getItems().add(currentTag);
        }
        tagSelect.setValue(currentTag);
        tagSelect.getStyleClass().add("form-select");
        tagSelect.setStyle("-fx-background-color: -bg-input; -fx-background-radius: 10; " +
            "-fx-border-color: -border; -fx-border-radius: 10; -fx-border-width: 1; " +
            "-fx-pref-height: 38; -fx-text-fill: -text-primary; -fx-font-size: 14px;");
        HBox.setHgrow(tagSelect, Priority.ALWAYS);
        tagRow.getChildren().addAll(tagLabel, tagSelect);

        HBox estimatedRow = createSettingRow();
        Label estimatedLabel = createFieldLabel("预计番茄数");
        int estimated = task != null ? task.getEstimatedPomodoros() : 1;
        estimatedSpinner = new Spinner<>(1, 20, estimated, 1);
        estimatedSpinner.getStyleClass().add("form-spinner");
        estimatedSpinner.setStyle("-fx-background-color: -bg-input; -fx-background-radius: 10; " +
            "-fx-border-color: -border; -fx-border-radius: 10; -fx-border-width: 1; " +
            "-fx-pref-height: 38; -fx-effect: null;");
        estimatedSpinner.setEditable(true);
        estimatedRow.getChildren().addAll(estimatedLabel, estimatedSpinner);

        content.getChildren().addAll(nameRow, tagRow, estimatedRow);

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(20, 0, 0, 0));

        ButtonType cancelBtnType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmBtnType = new ButtonType(task == null ? "创建" : "保存", ButtonBar.ButtonData.OK_DONE);

        Button cancelBtn = new Button("取消");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; " +
            "-fx-border-color: -border; -fx-border-radius: 10; -fx-border-width: 1; " +
            "-fx-text-fill: -text-primary; -fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 8 20;");
        cancelBtn.setOnAction(e -> {
            setResult(cancelBtnType);
            close();
        });

        Button confirmBtn = new Button(task == null ? "创建" : "保存");
        confirmBtn.getStyleClass().add("btn-primary");
        confirmBtn.setStyle("-fx-background-color: -primary; -fx-background-radius: 10; " +
            "-fx-text-fill: -primary-text; -fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 8 20; -fx-border-width: 0;");
        confirmBtn.setOnAction(e -> {
            setResult(confirmBtnType);
            close();
        });

        btnRow.getChildren().addAll(cancelBtn, confirmBtn);
        content.getChildren().add(btnRow);

        getDialogPane().setContent(content);
        getDialogPane().getStyleClass().add("custom-dialog");
        getDialogPane().setStyle("-fx-background-color: transparent;");

        content.setScaleX(0.85);
        content.setScaleY(0.85);
        content.setOpacity(0);

        setOnShown(e -> {
            Scene scene = getDialogPane().getScene();
            scene.setFill(Color.TRANSPARENT);
            var themeUrl = getClass().getResource("/styles/theme.css");
            if (themeUrl != null) scene.getStylesheets().add(themeUrl.toExternalForm());
            var compUrl = getClass().getResource("/styles/components.css");
            if (compUrl != null) scene.getStylesheets().add(compUrl.toExternalForm());
            themeService.applyTheme(scene);

            ScaleTransition st = new ScaleTransition(Duration.millis(250), content);
            st.setFromX(0.85); st.setFromY(0.85);
            st.setToX(1.0); st.setToY(1.0);

            FadeTransition ft = new FadeTransition(Duration.millis(250), content);
            ft.setFromValue(0);
            ft.setToValue(1.0);

            ParallelTransition pt = new ParallelTransition(st, ft);
            pt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            pt.play();

            titleField.requestFocus();
        });
    }

    private HBox createSettingRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("setting-row");
        return row;
    }

    private Label createFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        lbl.setMinWidth(90);
        return lbl;
    }

    public String getTaskTitle() { return titleField.getText().trim(); }
    public String getTaskTag() { return tagSelect.getValue(); }
    public int getEstimatedPomodoros() { return estimatedSpinner.getValue(); }
}
