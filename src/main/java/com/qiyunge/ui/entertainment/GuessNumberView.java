package com.qiyunge.ui.entertainment;

import com.qiyunge.app.AppContext;
import com.qiyunge.domain.entity.GameRecord;
import com.qiyunge.ui.entertainment.GuessNumberViewModel.GuessEntry;
import javafx.animation.ScaleTransition;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;

public class GuessNumberView extends VBox {

    private final GuessNumberViewModel viewModel;
    private TextField guessField;
    private Button submitButton;
    private Button replayButton;
    private Label bigNumberLabel;
    private Label hintLabel;
    private Label hintArrowLabel;
    private FlowPane historyPane;
    private Label attemptLabel;
    private Label scoreValueLabel;
    private Label bestValueLabel;
    private Region progressFill;
    private Label rangeChipValue;
    private Label diffChipValue;
    private Label subtitleLabel;
    private Runnable onBack;

    // Difficulty pills
    private Button easyPill;
    private Button normalPill;
    private Button hardPill;

    private final ChangeListener<Number> attemptsListener = (obs, oldVal, newVal) -> refreshUI();
    private final ChangeListener<String> resultListener = (obs, oldVal, newVal) -> refreshUI();
    private final ChangeListener<GameRecord> bestRecordListener = (obs, oldVal, newVal) -> refreshBestRecord();

    public GuessNumberView(AppContext appContext) {
        this.viewModel = new GuessNumberViewModel(appContext);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(24, 24, 24, 24));
        setStyle("-fx-background-color: -bg-primary;");

        viewModel.resetGame();

        getChildren().addAll(
            buildHeaderRow(),
            buildTitleSection(),
            buildGameCard()
        );

        bindData();
    }

    // ==================== Header Row (Back + Difficulty Pills) ====================

    private HBox buildHeaderRow() {
        Button backBtn = new Button("← 返回游乐场");
        backBtn.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-text-fill: -text-secondary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6px 14px; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand;"
        );
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(
            "-fx-background-color: -bg-hover; " +
            "-fx-text-fill: -text-primary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6px 14px; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand;"
        ));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-text-fill: -text-secondary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 6px 14px; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand;"
        ));
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });

        HBox diffSelector = buildDiffSelector();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(0, backBtn, spacer, diffSelector);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildDiffSelector() {
        easyPill = createDiffPill("简单(1-100)", "easy");
        normalPill = createDiffPill("普通(1-500)", "normal");
        hardPill = createDiffPill("困难(1-1000)", "hard");

        applyPillActive(easyPill, true);
        applyPillInactive(normalPill);
        applyPillInactive(hardPill);

        easyPill.setOnAction(e -> switchDifficulty("easy"));
        normalPill.setOnAction(e -> switchDifficulty("normal"));
        hardPill.setOnAction(e -> switchDifficulty("hard"));

        HBox selector = new HBox(6, easyPill, normalPill, hardPill);
        selector.setAlignment(Pos.CENTER_RIGHT);
        return selector;
    }

    private Button createDiffPill(String text, String diff) {
        Button pill = new Button(text);
        pill.setUserData(diff);
        pill.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-text-fill: -text-secondary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 5px 14px; " +
            "-fx-background-radius: 9999px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 9999px; " +
            "-fx-cursor: hand;"
        );
        return pill;
    }

    private void applyPillActive(Button pill, boolean isGameReset) {
        pill.setStyle(
            "-fx-background-color: -primary; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 5px 14px; " +
            "-fx-background-radius: 9999px; " +
            "-fx-border-color: -primary; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 9999px; " +
            "-fx-cursor: hand;"
        );
        pill.setOnMouseEntered(e -> pill.setStyle(
            "-fx-background-color: -primary-hover; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 5px 14px; " +
            "-fx-background-radius: 9999px; " +
            "-fx-border-color: -primary-hover; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 9999px; " +
            "-fx-cursor: hand;"
        ));
        pill.setOnMouseExited(e -> applyPillActive(pill, false));
    }

    private void applyPillInactive(Button pill) {
        pill.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-text-fill: -text-secondary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 5px 14px; " +
            "-fx-background-radius: 9999px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 9999px; " +
            "-fx-cursor: hand;"
        );
        pill.setOnMouseEntered(e -> pill.setStyle(
            "-fx-background-color: -bg-hover; " +
            "-fx-text-fill: -text-primary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 5px 14px; " +
            "-fx-background-radius: 9999px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 9999px; " +
            "-fx-cursor: hand;"
        ));
        pill.setOnMouseExited(e -> applyPillInactive(pill));
    }

    private void switchDifficulty(String diff) {
        applyPillActive(easyPill, "easy".equals(diff));
        applyPillInactive(normalPill);
        applyPillInactive(hardPill);
        if (!"easy".equals(diff)) applyPillInactive(easyPill);
        if ("normal".equals(diff)) {
            applyPillActive(normalPill, true);
        } else {
            applyPillInactive(normalPill);
        }
        if ("hard".equals(diff)) {
            applyPillActive(hardPill, true);
        } else {
            applyPillInactive(hardPill);
        }
        viewModel.setDifficulty(diff);
        refreshUI();
        clearInputAndFocus();
    }

    // ==================== Title Section ====================

    private VBox buildTitleSection() {
        Label title = new Label("猜数");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        title.setAlignment(Pos.CENTER);

        subtitleLabel = new Label("猜一个1到100之间的数字");
        subtitleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        subtitleLabel.setAlignment(Pos.CENTER);

        VBox box = new VBox(2, title, subtitleLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(0, 0, 24, 0));
        return box;
    }

    // ==================== Game Card ====================

    private VBox buildGameCard() {
        VBox card = new VBox(0);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(600);
        card.setPadding(new Insets(32, 28, 28, 28));
        card.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: -border-light; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 4);"
        );

        // a. Attempt label
        attemptLabel = new Label("请输入你的猜测...");
        attemptLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-font-weight: 500;");
        attemptLabel.setAlignment(Pos.CENTER);
        attemptLabel.setMaxWidth(Double.MAX_VALUE);

        // b. Big number display
        bigNumberLabel = new Label("?");
        bigNumberLabel.setStyle("-fx-font-size: 72px; -fx-font-weight: 700; -fx-text-fill: -text-primary; -fx-line-spacing: 1.1;");
        bigNumberLabel.setAlignment(Pos.CENTER);
        bigNumberLabel.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(bigNumberLabel, new Insets(8, 0, 4, 0));

        // c. Hint text with arrow
        hintLabel = new Label("");
        hintLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -accent;");
        hintLabel.setAlignment(Pos.CENTER);

        hintArrowLabel = new Label("");
        hintArrowLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -accent;");

        HBox hintBox = new HBox(8, hintLabel, hintArrowLabel);
        hintBox.setAlignment(Pos.CENTER);

        // d. Guess history tags
        historyPane = new FlowPane();
        historyPane.setHgap(8);
        historyPane.setVgap(8);
        historyPane.setAlignment(Pos.CENTER);
        historyPane.setPadding(new Insets(20, 0, 0, 0));

        // e. Input row
        guessField = new TextField();
        guessField.setPromptText("输入你的猜测...");
        guessField.setStyle(
            "-fx-background-color: -bg-input; " +
            "-fx-text-fill: -text-primary; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 0 16px; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 10px; " +
            "-fx-prompt-text-fill: -text-tertiary;"
        );
        guessField.setPrefHeight(44);
        guessField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                guessField.setStyle(
                    "-fx-background-color: -bg-input; " +
                    "-fx-text-fill: -text-primary; " +
                    "-fx-font-size: 14px; " +
                    "-fx-padding: 0 16px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-border-color: -primary; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-prompt-text-fill: -text-tertiary; " +
                    "-fx-effect: dropshadow(gaussian, rgba(91,141,239,0.15), 6, 0, 0, 0);"
                );
            } else {
                guessField.setStyle(
                    "-fx-background-color: -bg-input; " +
                    "-fx-text-fill: -text-primary; " +
                    "-fx-font-size: 14px; " +
                    "-fx-padding: 0 16px; " +
                    "-fx-background-radius: 10px; " +
                    "-fx-border-color: -border; " +
                    "-fx-border-width: 1px; " +
                    "-fx-border-radius: 10px; " +
                    "-fx-prompt-text-fill: -text-tertiary;"
                );
            }
        });
        guessField.textProperty().bindBidirectional(viewModel.inputTextProperty());
        guessField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                doGuess();
            }
        });
        HBox.setHgrow(guessField, Priority.ALWAYS);

        submitButton = new Button("猜！");
        submitButton.setStyle(
            "-fx-background-color: -primary; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 0 24px; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-border-width: 0; " +
            "-fx-border-color: transparent;"
        );
        submitButton.setPrefHeight(44);
        submitButton.setOnMouseEntered(e -> submitButton.setStyle(
            "-fx-background-color: -primary-hover; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 0 24px; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-border-width: 0; " +
            "-fx-border-color: transparent; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 4);"
        ));
        submitButton.setOnMouseExited(e -> submitButton.setStyle(
            "-fx-background-color: -primary; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 0 24px; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-border-width: 0; " +
            "-fx-border-color: transparent;"
        ));
        submitButton.setOnAction(e -> doGuess());

        replayButton = new Button("再来一局");
        replayButton.setStyle(
            "-fx-background-color: -primary; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 0 24px; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-border-width: 0; " +
            "-fx-border-color: transparent;"
        );
        replayButton.setPrefHeight(44);
        replayButton.setOnMouseEntered(e -> replayButton.setStyle(
            "-fx-background-color: -primary-hover; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 0 24px; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-border-width: 0; " +
            "-fx-border-color: transparent; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 4);"
        ));
        replayButton.setOnMouseExited(e -> replayButton.setStyle(
            "-fx-background-color: -primary; " +
            "-fx-text-fill: #FFFFFF; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 0 24px; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-border-width: 0; " +
            "-fx-border-color: transparent;"
        ));
        replayButton.setOnAction(e -> {
            viewModel.resetGame();
            refreshUI();
            clearInputAndFocus();
        });
        replayButton.setVisible(false);
        replayButton.setManaged(false);

        HBox inputRow = new HBox(10, guessField, submitButton, replayButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(inputRow, new Insets(24, 0, 0, 0));

        // f. Score panel
        VBox scoreCurrent = new VBox(2);
        scoreCurrent.setAlignment(Pos.CENTER);
        scoreValueLabel = new Label("0");
        scoreValueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label scoreLabel = new Label("当前步数");
        scoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-font-weight: 500;");
        scoreCurrent.getChildren().addAll(scoreValueLabel, scoreLabel);

        VBox scoreBest = new VBox(2);
        scoreBest.setAlignment(Pos.CENTER);
        bestValueLabel = new Label("--");
        bestValueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label bestLabel = new Label("最佳记录");
        bestLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-font-weight: 500;");
        scoreBest.getChildren().addAll(bestValueLabel, bestLabel);

        HBox scorePanel = new HBox(16, scoreCurrent, scoreBest);
        scorePanel.setAlignment(Pos.CENTER);
        VBox.setMargin(scorePanel, new Insets(20, 0, 0, 0));

        // g. Progress bar
        StackPane progressTrack = new StackPane();
        progressTrack.setPrefWidth(240);
        progressTrack.setMaxWidth(240);
        progressTrack.setPrefHeight(6);
        progressTrack.setMinHeight(6);
        progressTrack.setStyle("-fx-background-color: -bg-tertiary; -fx-background-radius: 9999px;");

        progressFill = new Region();
        progressFill.setPrefHeight(6);
        progressFill.setMinHeight(6);
        progressFill.setMaxHeight(6);
        progressFill.setPrefWidth(0);
        progressFill.setStyle("-fx-background-color: -primary; -fx-background-radius: 9999px;");

        StackPane.setAlignment(progressFill, Pos.CENTER_LEFT);
        progressTrack.getChildren().add(progressFill);

        HBox progressWrapper = new HBox(progressTrack);
        progressWrapper.setAlignment(Pos.CENTER);
        VBox.setMargin(progressWrapper, new Insets(12, 0, 0, 0));

        // h. Game info chips
        HBox rangeChip = new HBox(6);
        rangeChip.setStyle("-fx-background-color: -bg-tertiary; -fx-background-radius: 10px; -fx-padding: 6px 14px;");
        rangeChip.setAlignment(Pos.CENTER);
        Label rangeChipLabel = new Label("范围:");
        rangeChipLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: -text-secondary;");
        rangeChipValue = new Label("1 — 100");
        rangeChipValue.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        rangeChip.getChildren().addAll(rangeChipLabel, rangeChipValue);

        HBox diffChip = new HBox(6);
        diffChip.setStyle("-fx-background-color: -bg-tertiary; -fx-background-radius: 10px; -fx-padding: 6px 14px;");
        diffChip.setAlignment(Pos.CENTER);
        Label diffChipLabel = new Label("难度:");
        diffChipLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 500; -fx-text-fill: -text-secondary;");
        diffChipValue = new Label("简单");
        diffChipValue.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        diffChip.getChildren().addAll(diffChipLabel, diffChipValue);

        HBox infoRow = new HBox(16, rangeChip, diffChip);
        infoRow.setAlignment(Pos.CENTER);
        VBox.setMargin(infoRow, new Insets(20, 0, 0, 0));

        card.getChildren().addAll(
            attemptLabel,
            bigNumberLabel,
            hintBox,
            historyPane,
            inputRow,
            scorePanel,
            progressWrapper,
            infoRow
        );

        return card;
    }

    // ==================== History Tag Builders ====================

    private Label buildHistoryTag(GuessEntry entry, boolean isCurrent) {
        String arrow;
        if (entry.isUp()) arrow = "↑";
        else if (entry.isDown()) arrow = "↓";
        else arrow = "✓";

        Label tag = new Label(entry.getNumber() + " " + arrow);
        if (isCurrent) {
            tag.setStyle(
                "-fx-background-color: -primary-light; " +
                "-fx-text-fill: -primary; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 600; " +
                "-fx-padding: 4px 12px; " +
                "-fx-background-radius: 9999px; " +
                "-fx-border-color: rgba(91,141,239,0.25); " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 9999px;"
            );
        } else {
            tag.setStyle(
                "-fx-background-color: -bg-tertiary; " +
                "-fx-text-fill: -text-secondary; " +
                "-fx-font-size: 12px; " +
                "-fx-font-weight: 500; " +
                "-fx-padding: 4px 12px; " +
                "-fx-background-radius: 9999px;"
            );
        }
        return tag;
    }

    // ==================== Bounce Animation for Hint Arrow ====================

    private void startArrowBounce() {
        ScaleTransition st = new ScaleTransition(Duration.millis(600), hintArrowLabel);
        st.setFromY(0);
        st.setToY(1);
        st.setFromX(1);
        st.setToX(1);
        st.setAutoReverse(true);
        st.setCycleCount(ScaleTransition.INDEFINITE);
        st.play();
    }

    private void stopArrowBounce() {
        hintArrowLabel.setScaleY(1);
    }

    // ==================== UI Refresh ====================

    private void refreshUI() {
        boolean gameOver = viewModel.isGameOver();
        int attempts = viewModel.attemptsProperty().get();

        // Attempt label
        if (attempts == 0) {
            attemptLabel.setText("请输入你的猜测...");
        } else {
            attemptLabel.setText("第 " + attempts + " 次猜测");
        }

        // Big number
        if (attempts == 0) {
            bigNumberLabel.setText("?");
        } else {
            bigNumberLabel.setText(String.valueOf(viewModel.getLastGuessNumber()));
        }

        // Hint text & arrow
        String result = viewModel.resultTextProperty().get();
        if (gameOver) {
            hintLabel.setText("猜中了！");
            hintLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -success;");
            hintArrowLabel.setText("");
            hintArrowLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -success;");
            stopArrowBounce();
        } else if (!result.isEmpty()) {
            hintLabel.setText(result);
            hintLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -accent;");
            if (result.contains("大了")) {
                hintArrowLabel.setText("↓");
                hintArrowLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -accent;");
            } else if (result.contains("小了")) {
                hintArrowLabel.setText("↑");
                hintArrowLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -accent;");
            } else {
                hintArrowLabel.setText("");
            }
            startArrowBounce();
        } else {
            hintLabel.setText("");
            hintArrowLabel.setText("");
            stopArrowBounce();
        }

        // History tags
        historyPane.getChildren().clear();
        List<GuessEntry> history = viewModel.getHistory();
        for (int i = 0; i < history.size(); i++) {
            boolean isCurrent = (i == history.size() - 1) && !gameOver;
            historyPane.getChildren().add(buildHistoryTag(history.get(i), isCurrent));
        }

        // Input row: submit vs replay
        if (gameOver) {
            submitButton.setVisible(false);
            submitButton.setManaged(false);
            replayButton.setVisible(true);
            replayButton.setManaged(true);
            guessField.setEditable(false);
            guessField.setDisable(true);
        } else {
            submitButton.setVisible(true);
            submitButton.setManaged(true);
            replayButton.setVisible(false);
            replayButton.setManaged(false);
            guessField.setEditable(true);
            guessField.setDisable(false);
        }

        // Score
        scoreValueLabel.setText(String.valueOf(attempts));
        refreshBestRecord();

        // Progress bar
        double pct = viewModel.getProgressPercent();
        progressFill.setPrefWidth(240 * pct);

        // Subtitle
        subtitleLabel.setText("猜一个" + viewModel.getMinRange() + "到" + viewModel.getMaxRange() + "之间的数字");

        // Info chips
        rangeChipValue.setText(viewModel.getRangeText());
        diffChipValue.setText(viewModel.getDifficultyName());
    }

    private void refreshBestRecord() {
        GameRecord r = viewModel.bestRecordProperty().get();
        if (r == null || r.getScore() <= 0) {
            bestValueLabel.setText("--");
        } else {
            bestValueLabel.setText(r.getScore() + "步");
        }
    }

    // ==================== Data Binding ====================

    private void bindData() {
        viewModel.attemptsProperty().addListener(attemptsListener);
        viewModel.resultTextProperty().addListener(resultListener);
        viewModel.bestRecordProperty().addListener(bestRecordListener);
    }

    // ==================== Game Actions ====================

    private void doGuess() {
        viewModel.makeGuess(guessField.getText());
        refreshUI();
        clearInputAndFocus();
    }

    private void clearInputAndFocus() {
        guessField.clear();
        guessField.requestFocus();
    }

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }
}
