package com.qiyunge.ui.pomodoro;

import com.qiyunge.app.AppContext;
import com.qiyunge.domain.entity.PomodoroTask;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.scene.paint.Color;

import java.util.Optional;
import java.util.function.Consumer;

public class PomodoroView extends BorderPane {

    private final AppContext appContext;
    private final PomodoroViewModel vm;
    private final Runnable onBack;
    private final Consumer<String> onNavigateStats;

    private Circle progressCircle;
    private double circumference;
    private VBox taskListContainer;

    public PomodoroView(AppContext appContext, Runnable onBack, Consumer<String> onNavigateStats) {
        this.appContext = appContext;
        this.vm = new PomodoroViewModel(appContext);
        this.onBack = onBack;
        this.onNavigateStats = onNavigateStats;

        this.setStyle("-fx-background-color: -bg-primary;");

        setTop(buildHeader());
        setCenter(buildMainContent());

        vm.loadTasks();
        vm.loadTodayStats();
    }

    private Node buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 20, 12, 20));
        header.setStyle("-fx-background-color: -bg-card; -fx-border-color: -border-light; -fx-border-width: 0 0 1 0;");

        Button backBtn = new Button("← 返回");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-secondary; " +
            "-fx-font-size: 13px; -fx-font-weight: 500; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 8;");
        backBtn.setOnAction(e -> {
            if (onBack != null) onBack.run();
        });
        backBtn.setOnMouseEntered(e ->
            backBtn.setStyle("-fx-background-color: -bg-hover; -fx-text-fill: -text-primary; " +
                "-fx-font-size: 13px; -fx-font-weight: 500; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 8;"));
        backBtn.setOnMouseExited(e ->
            backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-secondary; " +
                "-fx-font-size: 13px; -fx-font-weight: 500; -fx-cursor: hand; -fx-padding: 6 12; -fx-background-radius: 8;"));

        Label title = new Label("专注计时");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button statsBtn = createIconBtn("📊", "统计");
        statsBtn.setOnAction(e -> {
            if (onNavigateStats != null) onNavigateStats.accept("overview");
        });

        Button settingsBtn = createIconBtn("⚙", "设置");
        settingsBtn.setOnAction(e -> showSettingsDialog());

        header.getChildren().addAll(backBtn, title, spacer, statsBtn, settingsBtn);
        return header;
    }

    private Button createIconBtn(String icon, String tooltip) {
        Button btn = new Button(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-secondary; " +
            "-fx-font-size: 16px; -fx-cursor: hand; -fx-pref-width: 36; -fx-pref-height: 36; " +
            "-fx-background-radius: 8; -fx-padding: 0;");
        btn.setOnMouseEntered(e ->
            btn.setStyle("-fx-background-color: -bg-hover; -fx-text-fill: -text-primary; " +
                "-fx-font-size: 16px; -fx-cursor: hand; -fx-pref-width: 36; -fx-pref-height: 36; " +
                "-fx-background-radius: 8; -fx-padding: 0;"));
        btn.setOnMouseExited(e ->
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-secondary; " +
                "-fx-font-size: 16px; -fx-cursor: hand; -fx-pref-width: 36; -fx-pref-height: 36; " +
                "-fx-background-radius: 8; -fx-padding: 0;"));
        return btn;
    }

    private Node buildMainContent() {
        HBox main = new HBox(0);
        main.setStyle("-fx-background-color: -bg-primary;");

        VBox leftPanel = new VBox(0);
        leftPanel.setPrefWidth(480);
        leftPanel.setMinWidth(420);
        leftPanel.setStyle("-fx-background-color: -bg-primary; -fx-border-color: -border-light; -fx-border-width: 0 1 0 0;");
        VBox.setVgrow(leftPanel, Priority.ALWAYS);

        leftPanel.getChildren().add(buildTimerSection());

        VBox rightPanel = new VBox(0);
        rightPanel.setStyle("-fx-background-color: -bg-primary;");
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        rightPanel.getChildren().add(buildTaskSection());

        main.getChildren().addAll(leftPanel, rightPanel);
        return main;
    }

    private Node buildTimerSection() {
        VBox section = new VBox(0);
        section.setPadding(new Insets(32, 40, 24, 40));
        VBox.setVgrow(section, Priority.ALWAYS);

        double radius = 130;
        circumference = 2 * Math.PI * radius;

        StackPane ringWrap = new StackPane();
        ringWrap.setPrefSize(280, 280);
        ringWrap.setMinSize(280, 280);
        ringWrap.setAlignment(Pos.CENTER);

        Circle bgCircle = new Circle(radius);
        bgCircle.setFill(Color.TRANSPARENT);
        bgCircle.setStroke(Color.web("#E5E7EB"));
        bgCircle.setStrokeWidth(8);
        bgCircle.setStyle("stroke: -border;");

        progressCircle = new Circle(radius);
        progressCircle.setFill(Color.TRANSPARENT);
        progressCircle.setStroke(Color.web("#5B8DEF"));
        progressCircle.setStrokeWidth(8);
        progressCircle.setStrokeType(StrokeType.CENTERED);
        progressCircle.setRotate(-90);
        progressCircle.setStyle("stroke: -primary;");

        vm.remainingSecondsProperty().addListener((obs, oldVal, newVal) -> {
            double pct = vm.getProgressPercent();
            double offset = circumference * (1 - pct / 100.0);
            progressCircle.setStrokeDashOffset(offset);
        });
        progressCircle.setStrokeDashOffset(circumference);
        progressCircle.getStrokeDashArray().setAll(circumference);

        VBox centerContent = new VBox(4);
        centerContent.setAlignment(Pos.CENTER);

        Label timeDisplay = new Label();
        timeDisplay.setStyle("-fx-font-size: 48px; -fx-font-weight: 700; -fx-text-fill: -text-primary; -fx-font-family: 'Consolas', monospace;");
        timeDisplay.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> vm.formatTimeDisplay(vm.remainingSecondsProperty().get()),
                vm.remainingSecondsProperty()
            )
        );

        Label roundLabel = new Label();
        roundLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        roundLabel.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    int idx = vm.currentRoundIndexProperty().get();
                    int interval = vm.longBreakIntervalProperty().get();
                    if (idx <= 0) return "准备开始";
                    return "第 " + ((idx - 1) % interval + 1) + " / " + interval + " 轮";
                },
                vm.currentRoundIndexProperty(), vm.longBreakIntervalProperty()
            )
        );

        Label stateBadge = new Label();
        stateBadge.setPadding(new Insets(3, 14, 3, 14));
        stateBadge.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> vm.getSessionTypeLabel(),
                vm.sessionTypeProperty()
            )
        );
        stateBadge.styleProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    String type = vm.sessionTypeProperty().get();
                    String bg = "-primary-light";
                    String color = "-primary";
                    if ("SHORT_BREAK".equals(type)) { bg = "-accent-light"; color = "-accent"; }
                    else if ("LONG_BREAK".equals(type)) { bg = "-accent-light"; color = "-accent"; }
                    return "-fx-font-size: 11px; -fx-font-weight: 500; -fx-background-color: " + bg +
                        "; -fx-text-fill: " + color + "; -fx-background-radius: 9999;";
                },
                vm.sessionTypeProperty()
            )
        );

        centerContent.getChildren().addAll(timeDisplay, roundLabel, stateBadge);

        ringWrap.getChildren().addAll(bgCircle, progressCircle, centerContent);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(24, 0, 0, 0));

        Button resetBtn = createOutlineBtn("↺ 重置");
        resetBtn.setOnAction(e -> vm.resetTimer());

        Button skipBtn = createOutlineBtn("跳过 →");
        skipBtn.setOnAction(e -> vm.skipSession());

        Button primaryBtn = new Button("开始专注");
        primaryBtn.setPrefHeight(44);
        primaryBtn.setPadding(new Insets(0, 28, 0, 28));
        primaryBtn.setStyle("-fx-background-color: -primary; -fx-text-fill: white; " +
            "-fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 9999; -fx-cursor: hand;");
        primaryBtn.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    if (vm.isRunningProperty().get()) return "暂停";
                    var type = vm.currentSessionTypeProperty().get();
                    if (type == null || type == com.qiyunge.application.service.PomodoroService.SessionType.FOCUS) return "开始专注";
                    return "开始休息";
                },
                vm.isRunningProperty(), vm.currentSessionTypeProperty()
            )
        );
        primaryBtn.setOnAction(e -> {
            if (vm.isRunningProperty().get()) vm.pauseTimer();
            else vm.startTimer();
        });
        primaryBtn.setOnMouseEntered(e ->
            primaryBtn.setStyle("-fx-background-color: -primary-hover; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 9999; -fx-cursor: hand;"));
        primaryBtn.setOnMouseExited(e ->
            primaryBtn.setStyle("-fx-background-color: -primary; -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-font-weight: 600; -fx-background-radius: 9999; -fx-cursor: hand;"));

        actions.getChildren().addAll(resetBtn, primaryBtn, skipBtn);

        VBox currentTaskBox = new VBox(0);
        currentTaskBox.setPadding(new Insets(24, 0, 0, 0));

        Label currentTaskLabel = new Label("当前任务");
        currentTaskLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-font-weight: 500;");

        HBox currentTaskCard = new HBox(16);
        currentTaskCard.setPadding(new Insets(16, 20, 16, 20));
        currentTaskCard.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 12; " +
            "-fx-border-color: -border-light; -fx-border-width: 1;");
        currentTaskCard.setAlignment(Pos.CENTER_LEFT);

        StackPane taskIcon = new StackPane();
        taskIcon.setPrefSize(36, 36);
        taskIcon.setStyle("-fx-background-radius: 10; -fx-background-color: -primary-light;");
        Label iconLbl = new Label("📝");
        iconLbl.setStyle("-fx-font-size: 16px;");
        taskIcon.getChildren().add(iconLbl);

        VBox taskInfo = new VBox(2);
        Label taskName = new Label();
        taskName.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        taskName.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    PomodoroTask t = vm.activeTaskProperty().get();
                    return t != null ? t.getTitle() : "未选择任务";
                },
                vm.activeTaskProperty()
            )
        );

        Label taskMeta = new Label();
        taskMeta.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        taskMeta.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> {
                    PomodoroTask t = vm.activeTaskProperty().get();
                    if (t == null) return "从右侧选择一个任务开始";
                    return t.getCompletedPomodoros() + " / " + t.getEstimatedPomodoros() + " 番茄";
                },
                vm.activeTaskProperty()
            )
        );

        taskInfo.getChildren().addAll(taskName, taskMeta);
        HBox.setHgrow(taskInfo, Priority.ALWAYS);

        currentTaskCard.getChildren().addAll(taskIcon, taskInfo);
        currentTaskBox.getChildren().addAll(currentTaskLabel, currentTaskCard);

        VBox.setMargin(ringWrap, new Insets(0, 0, 0, 0));
        section.getChildren().addAll(ringWrap, actions, currentTaskBox);
        return section;
    }

    private Button createOutlineBtn(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(38);
        btn.setPadding(new Insets(0, 18, 0, 18));
        btn.setStyle("-fx-background-color: -bg-card; -fx-text-fill: -text-secondary; " +
            "-fx-font-size: 13px; -fx-font-weight: 500; -fx-background-radius: 9999; " +
            "-fx-border-color: -border; -fx-border-radius: 9999; -fx-border-width: 1; -fx-cursor: hand;");
        btn.setOnMouseEntered(e ->
            btn.setStyle("-fx-background-color: -bg-hover; -fx-text-fill: -text-primary; " +
                "-fx-font-size: 13px; -fx-font-weight: 500; -fx-background-radius: 9999; " +
                "-fx-border-color: -primary; -fx-border-radius: 9999; -fx-border-width: 1; -fx-cursor: hand;"));
        btn.setOnMouseExited(e ->
            btn.setStyle("-fx-background-color: -bg-card; -fx-text-fill: -text-secondary; " +
                "-fx-font-size: 13px; -fx-font-weight: 500; -fx-background-radius: 9999; " +
                "-fx-border-color: -border; -fx-border-radius: 9999; -fx-border-width: 1; -fx-cursor: hand;"));
        return btn;
    }

    private Node buildTaskSection() {
        VBox section = new VBox(0);
        VBox.setVgrow(section, Priority.ALWAYS);

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-border-color: -border-light; -fx-border-width: 0 0 1 0;");

        Label title = new Label("今日任务");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        Label countLbl = new Label();
        countLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");
        countLbl.textProperty().bind(
            javafx.beans.binding.Bindings.createStringBinding(
                () -> vm.getTaskList().size() + " 项",
                vm.getTaskList()
            )
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ 新建");
        addBtn.setPrefHeight(32);
        addBtn.setPadding(new Insets(0, 14, 0, 14));
        addBtn.setStyle("-fx-background-color: -primary-light; -fx-text-fill: -primary; " +
            "-fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8; -fx-cursor: hand;");
        addBtn.setOnAction(e -> showAddTaskDialog());
        addBtn.setOnMouseEntered(e ->
            addBtn.setStyle("-fx-background-color: rgba(91,141,239,0.18); -fx-text-fill: -primary; " +
                "-fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8; -fx-cursor: hand;"));
        addBtn.setOnMouseExited(e ->
            addBtn.setStyle("-fx-background-color: -primary-light; -fx-text-fill: -primary; " +
                "-fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8; -fx-cursor: hand;"));

        header.getChildren().addAll(title, countLbl, spacer, addBtn);

        taskListContainer = new VBox(2);
        taskListContainer.setPadding(new Insets(8, 12, 12, 12));
        taskListContainer.setStyle("-fx-background-color: -bg-primary;");
        ScrollPane scroll = new ScrollPane(taskListContainer);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("no-scrollbar");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        vm.getTaskList().addListener((javafx.collections.ListChangeListener.Change<? extends PomodoroTask> c) -> {
            refreshTaskList();
        });

        section.getChildren().addAll(header, scroll);

        Platform.runLater(this::refreshTaskList);
        return section;
    }

    private void refreshTaskList() {
        taskListContainer.getChildren().clear();
        if (vm.getTaskList().isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(48, 24, 48, 24));
            Label emptyIcon = new Label("📋");
            emptyIcon.setStyle("-fx-font-size: 36px; -fx-opacity: 0.5;");
            Label emptyText = new Label("还没有任务");
            emptyText.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary;");
            Button emptyAddBtn = new Button("+ 创建第一个任务");
            emptyAddBtn.setStyle("-fx-background-color: -primary-light; -fx-text-fill: -primary; " +
                "-fx-font-size: 12px; -fx-font-weight: 600; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 6 14;");
            emptyAddBtn.setOnAction(e -> showAddTaskDialog());
            empty.getChildren().addAll(emptyIcon, emptyText, emptyAddBtn);
            taskListContainer.getChildren().add(empty);
            return;
        }

        for (PomodoroTask task : vm.getTaskList()) {
            taskListContainer.getChildren().add(createTaskRow(task));
        }
    }

    private Node createTaskRow(PomodoroTask task) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 8, 10, 8));
        row.setStyle("-fx-background-radius: 10; -fx-cursor: hand;");

        boolean isDone = task.getIsCompleted() == 1;
        boolean isActive = vm.activeTaskProperty().get() != null
            && vm.activeTaskProperty().get().getId() == task.getId();

        if (isActive) {
            row.setStyle("-fx-background-radius: 10; -fx-cursor: hand; -fx-background-color: -primary-light;");
        }

        StackPane checkbox = new StackPane();
        checkbox.setPrefSize(20, 20);
        checkbox.setMinSize(20, 20);
        checkbox.setMaxSize(20, 20);
        checkbox.setCursor(javafx.scene.Cursor.HAND);
        if (isDone) {
            checkbox.setStyle("-fx-background-color: -success; -fx-background-radius: 9999;");
            Label check = new Label("✓");
            check.setStyle("-fx-font-size: 11px; -fx-text-fill: white; -fx-font-weight: 700;");
            checkbox.getChildren().add(check);
        } else {
            checkbox.setStyle("-fx-background-color: transparent; -fx-background-radius: 9999; " +
                "-fx-border-color: -border; -fx-border-radius: 9999; -fx-border-width: 2;");
        }
        checkbox.setOnMouseClicked(e -> {
            e.consume();
            vm.toggleTaskComplete(task);
        });

        VBox taskContent = new VBox(2);
        HBox.setHgrow(taskContent, Priority.ALWAYS);

        Label nameLbl = new Label(task.getTitle());
        if (isDone) {
            nameLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-tertiary; -fx-strikethrough: true;");
        } else {
            nameLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-primary;");
        }

        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        String tag = task.getTag();
        if (tag != null && !tag.isEmpty()) {
            Label tagLbl = new Label(tag);
            String tagBg = "-bg-tertiary";
            String tagColor = "-text-secondary";
            if ("工作".equals(tag)) { tagBg = "rgba(91,141,239,0.1)"; tagColor = "#5B8DEF"; }
            else if ("学习".equals(tag)) { tagBg = "rgba(20,184,166,0.1)"; tagColor = "#14B8A6"; }
            else if ("阅读".equals(tag)) { tagBg = "rgba(245,158,11,0.1)"; tagColor = "#F59E0B"; }
            tagLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-background-color: " + tagBg +
                "; -fx-text-fill: " + tagColor + "; -fx-background-radius: 9999; -fx-padding: 2 10 2 10;");
            metaRow.getChildren().add(tagLbl);
        }

        int completed = task.getCompletedPomodoros();
        int estimated = task.getEstimatedPomodoros();
        Label tomatoLbl = new Label(completed + "/" + estimated + " 番茄");
        if (isDone) {
            tomatoLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -success;");
        } else {
            tomatoLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        }
        metaRow.getChildren().add(tomatoLbl);

        taskContent.getChildren().addAll(nameLbl, metaRow);

        Button deleteBtn = new Button("✕");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-tertiary; " +
            "-fx-font-size: 12px; -fx-cursor: hand; -fx-pref-width: 28; -fx-pref-height: 28; " +
            "-fx-background-radius: 6; -fx-padding: 0; -fx-opacity: 0;");
        deleteBtn.setOnAction(e -> vm.deleteTask(task));
        row.setOnMouseEntered(e ->
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-tertiary; " +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-pref-width: 28; -fx-pref-height: 28; " +
                "-fx-background-radius: 6; -fx-padding: 0; -fx-opacity: 1;"));
        row.setOnMouseExited(e ->
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: -text-tertiary; " +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-pref-width: 28; -fx-pref-height: 28; " +
                "-fx-background-radius: 6; -fx-padding: 0; -fx-opacity: 0;"));
        deleteBtn.setOnMouseEntered(e ->
            deleteBtn.setStyle("-fx-background-color: -danger-light; -fx-text-fill: -danger; " +
                "-fx-font-size: 12px; -fx-cursor: hand; -fx-pref-width: 28; -fx-pref-height: 28; " +
                "-fx-background-radius: 6; -fx-padding: 0; -fx-opacity: 1;"));

        row.setOnMouseClicked(e -> {
            if (!isDone) {
                vm.setActiveTask(task);
            }
        });
        row.setOnMouseEntered(e -> {
            if (!isActive) row.setStyle("-fx-background-radius: 10; -fx-cursor: hand; -fx-background-color: -bg-hover;");
        });
        row.setOnMouseExited(e -> {
            if (isActive) row.setStyle("-fx-background-radius: 10; -fx-cursor: hand; -fx-background-color: -primary-light;");
            else row.setStyle("-fx-background-radius: 10; -fx-cursor: hand;");
        });

        row.getChildren().addAll(checkbox, taskContent, deleteBtn);
        return row;
    }

    private void showAddTaskDialog() {
        PomodoroTaskDialog dialog = new PomodoroTaskDialog(null, appContext.getThemeService());
        dialog.initOwner(getScene().getWindow());
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            String title = dialog.getTaskTitle();
            if (title != null && !title.isEmpty()) {
                vm.addTask(title, dialog.getTaskTag(), dialog.getEstimatedPomodoros());
            }
        }
    }

    private void showSettingsDialog() {
        PomodoroSettingsDialog dialog = new PomodoroSettingsDialog(vm, appContext.getThemeService());
        dialog.initOwner(getScene().getWindow());
        dialog.showAndWait();
    }

    public void cleanup() {
        vm.cleanup();
    }
}
