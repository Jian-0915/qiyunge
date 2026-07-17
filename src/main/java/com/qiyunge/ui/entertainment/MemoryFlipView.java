package com.qiyunge.ui.entertainment;

import com.qiyunge.app.AppContext;
import com.qiyunge.domain.entity.GameRecord;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * 记忆翻牌游戏视图。
 */
public class MemoryFlipView extends VBox {

    private final MemoryFlipViewModel viewModel;
    private Runnable onBack;

    // UI elements
    private GridPane cardGrid;
    private StackPane[] cardPanes; // Each card's StackPane
    private VBox[] cardBacks;  // Back side of each card
    private VBox[] cardFronts; // Front side of each card
    private Label flipCountValue;
    private Label bestRecordValue;
    private Label matchedValue;
    private Label timeValue;
    private HBox pairProgressDots;
    private HBox boardProgressDots;
    private Label diffValueLabel;
    private Label pairCountLabel;
    private Button restartBtn;
    private Button pill4x4;
    private Button pill6x6;

    // Timer
    private Timeline timer;

    public MemoryFlipView(AppContext appContext) {
        this.viewModel = new MemoryFlipViewModel(appContext);
        setAlignment(Pos.TOP_CENTER);
        setPadding(new Insets(0));
        setStyle("-fx-background-color: -bg-primary;");

        viewModel.resetGame();

        getChildren().addAll(
            buildNavBar(),
            buildGameLayout()
        );

        // Start timer update
        timer = new Timeline(new KeyFrame(Duration.seconds(1), e -> viewModel.updateTimer()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();

        // Bind properties
        viewModel.flipCountProperty().addListener((obs, old, val) -> refreshStats());
        viewModel.matchedPairsProperty().addListener((obs, old, val) -> refreshStats());
        viewModel.timeProperty().addListener((obs, old, val) -> refreshStats());
        viewModel.bestRecordProperty().addListener((obs, old, val) -> refreshStats());
        viewModel.gameOverProperty().addListener((obs, old, val) -> {
            if (val) onGameComplete();
        });
    }

    // ==================== Navigation Bar ====================

    private HBox buildNavBar() {
        Button backBtn = new Button("← 返回游乐场");
        backBtn.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-text-fill: -text-secondary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 7px 14px; " +
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
            "-fx-padding: 7px 14px; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -text-tertiary; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand;"
        ));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-text-fill: -text-secondary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 7px 14px; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand;"
        ));
        backBtn.setOnAction(e -> { if (onBack != null) onBack.run(); });

        // Center: title + subtitle
        VBox centerBox = new VBox(1);
        centerBox.setAlignment(Pos.CENTER);
        Label title = new Label("翻牌");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label subtitle = new Label("翻牌配对，考验你的记忆");
        subtitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: -text-tertiary;");
        centerBox.getChildren().addAll(title, subtitle);

        // Right: difficulty pills
        pill4x4 = createDiffPill("4x4");
        pill6x6 = createDiffPill("6x6");
        applyPillActive(pill4x4);
        applyPillInactive(pill6x6);
        pill4x4.setOnAction(e -> switchDifficulty("easy"));
        pill6x6.setOnAction(e -> switchDifficulty("hard"));

        HBox diffBox = new HBox(6, pill4x4, pill6x6);
        diffBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox navBar = new HBox(0, backBtn, spacer1, centerBox, spacer2, diffBox);
        navBar.setAlignment(Pos.CENTER_LEFT);
        navBar.setPadding(new Insets(0, 0, 20, 0));
        return navBar;
    }

    private Button createDiffPill(String text) {
        Button pill = new Button(text);
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

    private void applyPillActive(Button pill) {
        pill.setStyle(
            "-fx-background-color: -primary; " +
            "-fx-text-fill: -text-on-primary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 5px 14px; " +
            "-fx-background-radius: 9999px; " +
            "-fx-border-color: -primary; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 9999px; " +
            "-fx-cursor: hand;"
        );
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
    }

    private void switchDifficulty(String diff) {
        if ("hard".equals(diff)) {
            applyPillActive(pill6x6);
            applyPillInactive(pill4x4);
        } else {
            applyPillActive(pill4x4);
            applyPillInactive(pill6x6);
        }
        viewModel.setDifficulty(diff);
        rebuildBoard();
    }

    // ==================== Two-Column Layout ====================

    private HBox buildGameLayout() {
        HBox layout = new HBox(24);
        layout.setAlignment(Pos.CENTER);
        layout.setMaxWidth(820);

        // Left: game board
        VBox gameCore = buildGameCore();
        HBox.setHgrow(gameCore, Priority.ALWAYS);

        // Right: stats sidebar
        VBox statsSidebar = buildStatsSidebar();

        layout.getChildren().addAll(gameCore, statsSidebar);
        return layout;
    }

    // ==================== Game Board ====================

    private VBox buildGameCore() {
        VBox core = new VBox(0);
        core.setAlignment(Pos.CENTER);
        core.setPadding(new Insets(28, 24, 24, 24));
        core.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: -border-light; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 4);"
        );

        // Card grid
        cardGrid = new GridPane();
        cardGrid.setHgap(12);
        cardGrid.setVgap(12);
        cardGrid.setAlignment(Pos.CENTER);
        cardGrid.setMaxWidth(380);
        cardGrid.setPadding(new Insets(0, 0, 20, 0));

        // Build initial cards
        buildCards();

        // Progress dots below grid
        boardProgressDots = new HBox(8);
        boardProgressDots.setAlignment(Pos.CENTER);
        rebuildProgressDots();

        core.getChildren().addAll(cardGrid, boardProgressDots);
        return core;
    }

    private void buildCards() {
        int size = viewModel.getGridSize();
        int total = size * size;
        cardPanes = new StackPane[total];
        cardBacks = new VBox[total];
        cardFronts = new VBox[total];

        // Adjust card size based on grid
        double cardSize = size == 4 ? 80 : 56;
        double symbolSize = size == 4 ? 28 : 20;
        double backIconSize = size == 4 ? 22 : 16;

        for (int i = 0; i < total; i++) {
            final int index = i;

            VBox back = new VBox();
            back.setAlignment(Pos.CENTER);
            back.setPrefSize(cardSize, cardSize);
            back.setMinSize(cardSize, cardSize);
            back.setMaxSize(cardSize, cardSize);
            back.setStyle(
                "-fx-background-color: -bg-tertiary; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: -border; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 10px; " +
                "-fx-cursor: hand;"
            );
            Label backIcon = new Label("\u2601"); // ☁ cloud
            backIcon.setStyle("-fx-font-size: " + backIconSize + "px; -fx-text-fill: -text-tertiary; -fx-opacity: 0.5;");
            back.getChildren().add(backIcon);
            cardBacks[i] = back;

            VBox front = new VBox();
            front.setAlignment(Pos.CENTER);
            front.setPrefSize(cardSize, cardSize);
            front.setMinSize(cardSize, cardSize);
            front.setMaxSize(cardSize, cardSize);
            front.setStyle(
                "-fx-background-color: -bg-card; " +
                "-fx-background-radius: 10px; " +
                "-fx-border-color: -border; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 10px;"
            );
            String symbol = viewModel.getCardSymbol(i);
            Label symbolLabel = new Label(symbol);
            symbolLabel.setStyle("-fx-font-size: " + symbolSize + "px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
            front.getChildren().add(symbolLabel);
            cardFronts[i] = front;

            StackPane cardPane = new StackPane();
            cardPane.setPrefSize(cardSize, cardSize);
            cardPane.setMinSize(cardSize, cardSize);
            cardPane.setMaxSize(cardSize, cardSize);
            cardPane.getChildren().add(back); // Initially show back

            // Hover effect on face-down cards
            cardPane.setOnMouseEntered(e -> {
                if (!viewModel.isCardMatched(index) && !viewModel.isCardFaceUp(index)) {
                    back.setStyle(
                        "-fx-background-color: -bg-tertiary; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-color: -border; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2); " +
                        "-fx-translate-y: -2;"
                    );
                }
            });
            cardPane.setOnMouseExited(e -> {
                if (!viewModel.isCardMatched(index) && !viewModel.isCardFaceUp(index)) {
                    back.setStyle(
                        "-fx-background-color: -bg-tertiary; " +
                        "-fx-background-radius: 10px; " +
                        "-fx-border-color: -border; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-border-radius: 10px; " +
                        "-fx-cursor: hand;"
                    );
                }
            });

            // Click handler
            cardPane.setOnMouseClicked(e -> handleCardClick(index));

            cardPanes[i] = cardPane;

            int row = i / size;
            int col = i % size;
            cardGrid.add(cardPane, col, row);
        }
    }

    private void handleCardClick(int index) {
        if (viewModel.isProcessing()) {
            // Visual feedback: ViewModel already blocks the action
            return;
        }
        MemoryFlipViewModel.FlipResult result = viewModel.flipCard(index);
        if (result == null) return;

        // Flip animation: show front
        showCardFace(index, result.symbol);

        if (!result.isFirstOfPair && !result.isMatch) {
            // Two cards flipped, no match → delay then flip back
            int firstIndex = -1;
            int secondIndex = index;
            // Find the other face-up, non-matched card
            for (int i = 0; i < cardPanes.length; i++) {
                if (i != index && viewModel.isCardFaceUp(i) && !viewModel.isCardMatched(i)) {
                    firstIndex = i;
                    break;
                }
            }
            final int fi = firstIndex;
            final int si = secondIndex;
            
            cardGrid.setDisable(true);
            PauseTransition pause = new PauseTransition(Duration.millis(800));
            pause.setOnFinished(e -> {
                hideCardFace(fi);
                hideCardFace(si);
                viewModel.hideUnmatched();
                cardGrid.setDisable(false);
            });
            pause.play();
        } else if (result.isMatch) {
            // Match found → apply matched style to both cards
            applyMatchedStyle(index);
            // Find the other card in the pair
            for (int i = 0; i < cardPanes.length; i++) {
                if (i != index && viewModel.isCardMatched(i) && viewModel.isCardFaceUp(i)) {
                    applyMatchedStyle(i);
                    break;
                }
            }
        }
    }

    private void showCardFace(int index, String symbol) {
        StackPane pane = cardPanes[index];
        VBox front = cardFronts[index];
        
        // Apply "currently flipping" style
        front.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -primary; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 10px; " +
            "-fx-effect: dropshadow(gaussian, rgba(91,141,239,0.15), 6, 0, 0, 0);"
        );
        // Update symbol color to primary
        for (var node : front.getChildren()) {
            if (node instanceof Label label) {
                label.setStyle(label.getStyle().replace("-fx-text-fill: -text-primary;", "-fx-text-fill: -primary;"));
            }
        }

        // Simple flip animation using scale
        ScaleTransition stHide = new ScaleTransition(Duration.millis(120), pane);
        stHide.setFromX(1);
        stHide.setToX(0);
        stHide.setOnFinished(e -> {
            pane.getChildren().clear();
            pane.getChildren().add(front);
            ScaleTransition stShow = new ScaleTransition(Duration.millis(120), pane);
            stShow.setFromX(0);
            stShow.setToX(1);
            stShow.play();
        });
        stHide.play();
    }

    private void hideCardFace(int index) {
        StackPane pane = cardPanes[index];
        VBox back = cardBacks[index];
        
        // Reset front style
        VBox front = cardFronts[index];
        double symbolSize = viewModel.getGridSize() == 4 ? 28 : 20;
        front.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 10px;"
        );
        for (var node : front.getChildren()) {
            if (node instanceof Label label) {
                label.setStyle("-fx-font-size: " + symbolSize + "px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
            }
        }

        ScaleTransition stHide = new ScaleTransition(Duration.millis(120), pane);
        stHide.setFromX(1);
        stHide.setToX(0);
        stHide.setOnFinished(e -> {
            pane.getChildren().clear();
            pane.getChildren().add(back);
            ScaleTransition stShow = new ScaleTransition(Duration.millis(120), pane);
            stShow.setFromX(0);
            stShow.setToX(1);
            stShow.play();
        });
        stHide.play();
    }

    private void applyMatchedStyle(int index) {
        VBox front = cardFronts[index];
        double symbolSize = viewModel.getGridSize() == 4 ? 28 : 20;
        
        front.setStyle(
            "-fx-background-color: -success-light; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -success; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 10px;"
        );
        for (var node : front.getChildren()) {
            if (node instanceof Label label) {
                label.setStyle("-fx-font-size: " + symbolSize + "px; -fx-font-weight: 700; -fx-text-fill: -success;");
            }
        }

        // Add ✓ badge (a small circle with checkmark)
        StackPane pane = cardPanes[index];
        // Remove hover effects on matched cards
        pane.setOnMouseEntered(null);
        pane.setOnMouseExited(null);
        pane.setOnMouseClicked(null);
        pane.setStyle("-fx-cursor: default;");
    }

    private void rebuildBoard() {
        // Clear the card grid and rebuild
        cardGrid.getChildren().clear();
        buildCards();
        rebuildProgressDots();
        refreshStats();

        // If the game core is inside a StackPane (overlay), restore it
        if (getChildren().size() >= 2) {
            HBox gameLayout = (HBox) getChildren().get(1);
            if (!gameLayout.getChildren().isEmpty()) {
                Object firstChild = gameLayout.getChildren().get(0);
                if (firstChild instanceof StackPane sp && sp.getChildren().size() >= 2) {
                    VBox gameCore = (VBox) sp.getChildren().get(0);
                    gameLayout.getChildren().set(0, gameCore);
                    HBox.setHgrow(gameCore, Priority.ALWAYS);
                }
            }
        }
    }

    private void rebuildProgressDots() {
        boardProgressDots.getChildren().clear();
        int pairs = viewModel.getTotalPairs();
        for (int i = 0; i < pairs; i++) {
            Circle dot = new Circle(6);
            dot.setStyle("-fx-fill: transparent; -fx-stroke: -border; -fx-stroke-width: 2px;");
            // JavaFX Circle doesn't support CSS variables directly in fill/stroke
            // Use a Region instead
            Region dotRegion = new Region();
            dotRegion.setPrefSize(12, 12);
            dotRegion.setMinSize(12, 12);
            dotRegion.setMaxSize(12, 12);
            dotRegion.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-background-radius: 9999px; " +
                "-fx-border-color: -border; " +
                "-fx-border-width: 2px; " +
                "-fx-border-radius: 9999px;"
            );
            boardProgressDots.getChildren().add(dotRegion);
        }
        updateProgressDots();
    }

    private void updateProgressDots() {
        int matched = viewModel.matchedPairsProperty().get();
        var dots = boardProgressDots.getChildren();
        for (int i = 0; i < dots.size(); i++) {
            Region dot = (Region) dots.get(i);
            if (i < matched) {
                dot.setStyle(
                    "-fx-background-color: -success; " +
                    "-fx-background-radius: 9999px; " +
                    "-fx-border-color: -success; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 9999px;"
                );
            } else {
                dot.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-background-radius: 9999px; " +
                    "-fx-border-color: -border; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 9999px;"
                );
            }
        }
        // Also update pair progress dots in sidebar
        if (pairProgressDots != null) {
            var pairDots = pairProgressDots.getChildren();
            for (int i = 0; i < pairDots.size(); i++) {
                Region dot = (Region) pairDots.get(i);
                if (i < matched) {
                    dot.setStyle(
                        "-fx-background-color: -success; " +
                        "-fx-background-radius: 9999px; " +
                        "-fx-border-color: -success; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-border-radius: 9999px;"
                    );
                } else {
                    dot.setStyle(
                        "-fx-background-color: transparent; " +
                        "-fx-background-radius: 9999px; " +
                        "-fx-border-color: -border; " +
                        "-fx-border-width: 1.5px; " +
                        "-fx-border-radius: 9999px;"
                    );
                }
            }
        }
    }

    // ==================== Stats Sidebar ====================

    private VBox buildStatsSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.setPrefWidth(200);
        sidebar.setMinWidth(200);
        sidebar.setMaxWidth(200);

        // Game Info Card
        VBox infoCard = new VBox(0);
        infoCard.setPadding(new Insets(16));
        infoCard.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: -border-light; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);"
        );

        Label infoTitle = new Label("游戏信息");
        infoTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: -text-tertiary; -fx-letter-spacing: 0.03em;");
        infoTitle.setPadding(new Insets(0, 0, 10, 0));

        flipCountValue = new Label("0次");
        bestRecordValue = new Label("--");
        matchedValue = new Label("0/" + viewModel.getTotalPairs());
        timeValue = new Label("0:00");

        HBox row1 = createStatRow("翻牌次数", flipCountValue, true);
        HBox row2 = createStatRow("最佳记录", bestRecordValue, false);
        row2.setStyle(row2.getStyle() + " -fx-border-color: -border-light; -fx-border-width: 0 0 1px 0;");
        applyAccentValue(bestRecordValue);
        HBox row3 = createStatRow("已配对", matchedValue, false);
        row3.setStyle(row3.getStyle() + " -fx-border-color: -border-light; -fx-border-width: 0 0 1px 0;");
        HBox row4 = createStatRow("用时", timeValue, false);

        // Pair progress dots
        pairProgressDots = new HBox(6);
        pairProgressDots.setAlignment(Pos.CENTER_LEFT);
        pairProgressDots.setPadding(new Insets(8, 0, 0, 0));
        for (int i = 0; i < viewModel.getTotalPairs(); i++) {
            Region dot = new Region();
            dot.setPrefSize(10, 10);
            dot.setMinSize(10, 10);
            dot.setMaxSize(10, 10);
            dot.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-background-radius: 9999px; " +
                "-fx-border-color: -border; " +
                "-fx-border-width: 1.5px; " +
                "-fx-border-radius: 9999px;"
            );
            pairProgressDots.getChildren().add(dot);
        }

        infoCard.getChildren().addAll(infoTitle, row1, row2, row3, row4, pairProgressDots);

        // Difficulty Info Card
        VBox diffCard = new VBox(0);
        diffCard.setPadding(new Insets(16));
        diffCard.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: -border-light; " +
            "-fx-border-width: 1px; " +
            "-fx-border-radius: 16px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1);"
        );

        Label diffTitle = new Label("当前难度");
        diffTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: -text-tertiary; -fx-letter-spacing: 0.03em;");
        diffTitle.setPadding(new Insets(0, 0, 10, 0));

        diffValueLabel = new Label(viewModel.getDifficultyName());
        pairCountLabel = new Label(viewModel.getTotalPairs() + "对");

        HBox diffRow1 = createStatRow("难度", diffValueLabel, false);
        HBox diffRow2 = createStatRow("配对数", pairCountLabel, false);

        diffCard.getChildren().addAll(diffTitle, diffRow1, diffRow2);

        // Restart button
        restartBtn = new Button("↻ 再来一局");
        restartBtn.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-text-fill: -text-secondary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 10px 0; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center;"
        );
        restartBtn.setMaxWidth(Double.MAX_VALUE);
        restartBtn.setOnMouseEntered(e -> restartBtn.setStyle(
            "-fx-background-color: -bg-hover; " +
            "-fx-text-fill: -text-primary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 10px 0; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -text-tertiary; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center;"
        ));
        restartBtn.setOnMouseExited(e -> restartBtn.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-text-fill: -text-secondary; " +
            "-fx-font-size: 12px; " +
            "-fx-font-weight: 500; " +
            "-fx-padding: 10px 0; " +
            "-fx-background-radius: 10px; " +
            "-fx-border-color: -border; " +
            "-fx-border-width: 1.5px; " +
            "-fx-border-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-alignment: center;"
        ));
        restartBtn.setOnAction(e -> {
            viewModel.resetGame();
            rebuildBoard();
            diffValueLabel.setText(viewModel.getDifficultyName());
            pairCountLabel.setText(viewModel.getTotalPairs() + "对");
        });

        sidebar.getChildren().addAll(infoCard, diffCard, restartBtn);
        return sidebar;
    }

    private HBox createStatRow(String label, Label value, boolean isPrimary) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 0, 6, 0));
        row.setStyle("-fx-border-color: -border-light; -fx-border-width: 0 0 1px 0;");

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        if (isPrimary) {
            applyPrimaryValue(value);
        }
        
        row.getChildren().addAll(labelNode, spacer, value);
        return row;
    }

    private void applyPrimaryValue(Label value) {
        value.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -primary;");
    }

    private void applyAccentValue(Label value) {
        value.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -accent;");
    }

    // ==================== Stats Refresh ====================

    private void refreshStats() {
        if (flipCountValue != null) {
            flipCountValue.setText(viewModel.flipCountProperty().get() + "次");
        }
        if (bestRecordValue != null) {
            GameRecord best = viewModel.bestRecordProperty().get();
            bestRecordValue.setText(best != null ? best.getScore() + "次" : "--");
        }
        if (matchedValue != null) {
            matchedValue.setText(viewModel.matchedPairsProperty().get() + "/" + viewModel.totalPairsProperty().get());
        }
        if (timeValue != null) {
            timeValue.setText(viewModel.timeProperty().get());
        }
        updateProgressDots();
    }

    // ==================== Game Complete ====================

    private void onGameComplete() {
        // Show completion overlay on the game board
        // Find the game core VBox (first child of game layout HBox)
        if (getChildren().size() < 2) return;
        HBox gameLayout = (HBox) getChildren().get(1);
        if (gameLayout.getChildren().isEmpty()) return;

        VBox gameCore;
        Object firstChild = gameLayout.getChildren().get(0);
        if (firstChild instanceof StackPane sp) {
            // Already has overlay from previous completion, get the inner VBox
            gameCore = (VBox) sp.getChildren().get(0);
        } else {
            gameCore = (VBox) firstChild;
        }

        VBox overlay = new VBox(12);
        overlay.setAlignment(Pos.CENTER);
        overlay.setPadding(new Insets(24));
        overlay.setStyle(
            "-fx-background-color: rgba(255,255,255,0.95); " +
            "-fx-background-radius: 16px; " +
            "-fx-border-color: -success; " +
            "-fx-border-width: 2px; " +
            "-fx-border-radius: 16px;"
        );

        Label emoji = new Label("\uD83C\uDF89");
        emoji.setStyle("-fx-font-size: 40px;");
        Label title = new Label("全部配对成功！");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label stats = new Label("翻牌 " + viewModel.flipCountProperty().get() + " 次 · 用时 " + viewModel.timeProperty().get());
        stats.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");

        Button playAgain = new Button("再来一局");
        playAgain.setStyle(
            "-fx-background-color: -primary; " +
            "-fx-text-fill: -text-on-primary; " +
            "-fx-font-size: 14px; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 8px 24px; " +
            "-fx-background-radius: 10px; " +
            "-fx-cursor: hand; " +
            "-fx-border-width: 0;"
        );
        playAgain.setOnAction(e -> {
            viewModel.resetGame();
            rebuildBoard();
            diffValueLabel.setText(viewModel.getDifficultyName());
            pairCountLabel.setText(viewModel.getTotalPairs() + "对");
            // Remove overlay by restoring gameCore directly
            gameLayout.getChildren().set(0, gameCore);
            HBox.setHgrow(gameCore, Priority.ALWAYS);
        });

        overlay.getChildren().addAll(emoji, title, stats, playAgain);

        StackPane overlayContainer = new StackPane(gameCore, overlay);
        overlayContainer.setAlignment(Pos.CENTER);
        gameLayout.getChildren().set(0, overlayContainer);
        HBox.setHgrow(overlayContainer, Priority.ALWAYS);
    }

    // ==================== Public Methods ====================

    public void setOnBack(Runnable onBack) {
        this.onBack = onBack;
    }

    /**
     * 清理资源，停止计时器。
     */
    public void cleanup() {
        if (timer != null) {
            timer.stop();
        }
    }
}
