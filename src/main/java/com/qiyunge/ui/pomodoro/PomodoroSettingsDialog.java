package com.qiyunge.ui.pomodoro;

import com.qiyunge.app.ThemeService;
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

public class PomodoroSettingsDialog extends Dialog<ButtonType> {

    private final Spinner<Integer> focusDuration;
    private final Spinner<Integer> shortBreakDuration;
    private final Spinner<Integer> longBreakDuration;
    private final Spinner<Integer> longBreakInterval;
    private final CheckBox autoStartBreaks;
    private final CheckBox autoStartFocus;
    private final CheckBox alwaysOnTop;
    private final CheckBox showNotifications;
    private final ChoiceBox<String> musicBehavior;

    public PomodoroSettingsDialog(PomodoroViewModel vm, ThemeService themeService) {
        setTitle("专注计时设置");
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.TRANSPARENT);
        setHeaderText(null);
        setGraphic(null);

        VBox content = new VBox(0);
        content.setPrefWidth(460);
        content.setPadding(new Insets(36));
        content.getStyleClass().add("dialog-card");
        content.setStyle("-fx-background-color: -bg-card-solid; " +
            "-fx-background-radius: 16; -fx-border-radius: 16; " +
            "-fx-border-color: -border-light; -fx-border-width: 0.5; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 25, 0, 0, 8), " +
            "dropshadow(gaussian, rgba(0,0,0,0.12), 8, 0, 0, 2);");

        Label titleLbl = new Label("专注计时设置");
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Region divider = new Region();
        divider.getStyleClass().add("dialog-divider");
        VBox.setMargin(divider, new Insets(16, 0, 16, 0));

        content.getChildren().addAll(titleLbl, divider);

        Label section1 = createSectionLabel("时长设置");
        content.getChildren().add(section1);

        focusDuration = addDurationRow(content, "专注时长",
            vm.focusDurationProperty().get(), 1, 90, "分钟");
        shortBreakDuration = addDurationRow(content, "短休时长",
            vm.shortBreakDurationProperty().get(), 1, 30, "分钟");
        longBreakDuration = addDurationRow(content, "长休时长",
            vm.longBreakDurationProperty().get(), 1, 60, "分钟");
        longBreakInterval = addDurationRow(content, "长休间隔",
            vm.longBreakIntervalProperty().get(), 2, 10, "个番茄");

        Label section2 = createSectionLabel("自动化");
        VBox.setMargin(section2, new Insets(8, 0, 0, 0));
        content.getChildren().add(section2);

        autoStartBreaks = addCheckRow(content, "专注结束后自动开始休息",
            vm.autoStartBreaksProperty().get());
        autoStartFocus = addCheckRow(content, "休息结束后自动开始专注",
            vm.autoStartFocusProperty().get());
        alwaysOnTop = addCheckRow(content, "计时窗口置顶",
            vm.alwaysOnTopProperty().get());
        showNotifications = addCheckRow(content, "显示通知提醒", true);

        Label section3 = createSectionLabel("专注音乐");
        VBox.setMargin(section3, new Insets(8, 0, 0, 0));
        content.getChildren().add(section3);

        HBox musicRow = createSettingRow();
        Label musicLbl = new Label("专注开始时");
        musicLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");
        musicLbl.setMinWidth(90);

        musicBehavior = new ChoiceBox<>();
        musicBehavior.getItems().addAll("不做操作", "继续播放", "暂停音乐", "切换白噪音");
        String behavior = "不做操作";
        if (vm.pauseMusicOnFocusProperty().get()) behavior = "暂停音乐";
        else if (vm.switchToAmbientOnFocusProperty().get()) behavior = "切换白噪音";
        else if (vm.resumeMusicOnBreakProperty().get()) behavior = "继续播放";
        musicBehavior.setValue(behavior);
        musicBehavior.getStyleClass().add("form-select");
        musicBehavior.setStyle("-fx-background-color: -bg-input; -fx-background-radius: 10; " +
            "-fx-border-color: -border; -fx-border-radius: 10; -fx-border-width: 1; " +
            "-fx-pref-height: 38; -fx-text-fill: -text-primary; -fx-font-size: 14px; -fx-effect: null;");
        musicBehavior.setPrefWidth(180);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        musicRow.getChildren().addAll(musicLbl, spacer, musicBehavior);
        content.getChildren().add(musicRow);

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(20, 0, 0, 0));

        ButtonType cancelBtnType = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType saveBtnType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);

        Button cancelBtn = new Button("取消");
        cancelBtn.getStyleClass().add("btn-ghost");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-background-radius: 10; " +
            "-fx-border-color: -border; -fx-border-radius: 10; -fx-border-width: 1; " +
            "-fx-text-fill: -text-primary; -fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 8 20;");
        cancelBtn.setOnAction(e -> {
            setResult(cancelBtnType);
            close();
        });

        Button saveBtn = new Button("保存");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setStyle("-fx-background-color: -primary; -fx-background-radius: 10; " +
            "-fx-text-fill: -primary-text; -fx-font-size: 13px; -fx-font-weight: 500; -fx-padding: 8 20; -fx-border-width: 0;");
        saveBtn.setOnAction(e -> {
            applySettings(vm);
            setResult(saveBtnType);
            close();
        });

        btnRow.getChildren().addAll(cancelBtn, saveBtn);
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
        });
    }

    private void applySettings(PomodoroViewModel vm) {
        vm.focusDurationProperty().set(focusDuration.getValue());
        vm.shortBreakDurationProperty().set(shortBreakDuration.getValue());
        vm.longBreakDurationProperty().set(longBreakDuration.getValue());
        vm.longBreakIntervalProperty().set(longBreakInterval.getValue());
        vm.autoStartBreaksProperty().set(autoStartBreaks.isSelected());
        vm.autoStartFocusProperty().set(autoStartFocus.isSelected());
        vm.alwaysOnTopProperty().set(alwaysOnTop.isSelected());

        String mb = musicBehavior.getValue();
        vm.pauseMusicOnFocusProperty().set("暂停音乐".equals(mb));
        vm.switchToAmbientOnFocusProperty().set("切换白噪音".equals(mb));
        vm.resumeMusicOnBreakProperty().set("继续播放".equals(mb));

        vm.saveSettings();
        if (!vm.isRunningProperty().get()) {
            vm.resetTimer();
        }
    }

    private Label createSectionLabel(String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("section-label");
        return lbl;
    }

    private HBox createSettingRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("setting-row");
        return row;
    }

    private Spinner<Integer> addDurationRow(VBox parent, String label,
                                            int value, int min, int max, String unit) {
        HBox row = createSettingRow();
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");
        lbl.setMinWidth(90);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox controlBox = new HBox(8);
        controlBox.setAlignment(Pos.CENTER_LEFT);

        Spinner<Integer> spinner = new Spinner<>(min, max, value, 1);
        spinner.getStyleClass().add("form-spinner");
        spinner.setStyle("-fx-background-color: -bg-input; -fx-background-radius: 10; " +
            "-fx-border-color: -border; -fx-border-radius: 10; -fx-border-width: 1; " +
            "-fx-pref-height: 38; -fx-effect: null;");
        spinner.setEditable(true);

        Label unitLbl = new Label(unit);
        unitLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");

        controlBox.getChildren().addAll(spinner, unitLbl);
        row.getChildren().addAll(lbl, spacer, controlBox);
        parent.getChildren().add(row);
        return spinner;
    }

    private CheckBox addCheckRow(VBox parent, String label, boolean selected) {
        HBox row = createSettingRow();
        CheckBox check = new CheckBox(label);
        check.setSelected(selected);
        check.getStyleClass().add("form-checkbox");
        row.getChildren().add(check);
        parent.getChildren().add(row);
        return check;
    }
}
