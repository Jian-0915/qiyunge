package com.qiyunge.ui.components;

import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.util.LyricParser.LyricLine;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class LyricPane extends StackPane {

    private final List<LyricLine> lyrics = new ArrayList<>();
    private final VBox lyricsContainer = new VBox(8);
    private final ScrollPane scrollPane = new ScrollPane();
    private final StackPane emptyState = new StackPane();
    private final StackPane loadingState = new StackPane();

    private final HBox songInfoBar = new HBox(12);
    private final Label songTitleLabel = new Label();
    private final Label songArtistLabel = new Label();

    private final HBox progressBar = new HBox(12);
    private final Label currentTimeLabel = new Label("0:00");
    private final Label totalTimeLabel = new Label("0:00");
    private final javafx.scene.control.ProgressBar progressIndicator = new javafx.scene.control.ProgressBar(0);

    private int currentIndex = -1;
    private boolean isLoading = false;
    private SeekCallback seekCallback;

    public LyricPane() {
        this.getStyleClass().add("lyric-pane");
        this.setStyle("-fx-background-color: -bg-primary;");

        songInfoBar.setAlignment(Pos.CENTER_LEFT);
        songInfoBar.setPadding(new Insets(12, 16, 12, 16));
        songInfoBar.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border-light; -fx-border-width: 0 0 1 0;");

        songTitleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        songArtistLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        songInfoBar.getChildren().addAll(songTitleLabel, new Label(" - ") {{
            setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");
        }}, songArtistLabel, spacer);
        songInfoBar.setVisible(false);
        songInfoBar.setManaged(false);

        lyricsContainer.setAlignment(Pos.CENTER);
        lyricsContainer.setPadding(new Insets(60, 24, 60, 24));

        // 使用 StackPane 包裹歌词容器，通过 translateY 实现滚动
        StackPane lyricWrapper = new StackPane(lyricsContainer);
        lyricWrapper.setAlignment(Pos.CENTER);
        scrollPane.setContent(lyricWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setPannable(false);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox emptyContent = new VBox(12);
        emptyContent.setAlignment(Pos.CENTER);
        Label emptyIcon = new Label("\u266A");
        emptyIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: -text-tertiary;");
        Label emptyHint = new Label("暂无歌词");
        emptyHint.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        emptyContent.getChildren().addAll(emptyIcon, emptyHint);
        emptyState.getChildren().add(emptyContent);
        emptyState.setStyle("-fx-background-color: transparent;");

        VBox loadingContent = new VBox(8);
        loadingContent.setAlignment(Pos.CENTER);
        Label loadingIcon = new Label("\u266B");
        loadingIcon.setStyle("-fx-font-size: 36px; -fx-text-fill: -primary;");
        Label loadingText = new Label("加载歌词中...");
        loadingText.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary;");
        loadingContent.getChildren().addAll(loadingIcon, loadingText);
        loadingState.getChildren().add(loadingContent);
        loadingState.setStyle("-fx-background-color: transparent;");
        loadingState.setVisible(false);
        loadingState.setManaged(false);

        progressBar.setAlignment(Pos.CENTER);
        progressBar.setPadding(new Insets(12, 20, 12, 20));
        progressBar.setStyle("-fx-background-color: -bg-secondary; -fx-border-color: -border-light; -fx-border-width: 1 0 0 0;");
        progressBar.setPrefHeight(50);

        currentTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: -text-secondary; -fx-min-width: 50px; -fx-text-alignment: right;");
        totalTimeLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: -text-secondary; -fx-min-width: 50px;");
        progressIndicator.setPrefWidth(280);
        progressIndicator.setMinWidth(150);
        progressIndicator.setMaxWidth(450);
        progressIndicator.setPrefHeight(8);
        progressIndicator.setStyle("-fx-accent: -primary; -fx-background-color: -border-light;");
        HBox.setHgrow(progressIndicator, Priority.ALWAYS);

        progressIndicator.setOnMousePressed(event -> {
            if (seekCallback != null) {
                seekCallback.onSeek(event.getX() / progressIndicator.getWidth());
            }
        });
        progressIndicator.setOnMouseDragged(event -> {
            double x = Math.max(0, Math.min(event.getX(), progressIndicator.getWidth()));
            if (seekCallback != null) {
                seekCallback.onSeek(x / progressIndicator.getWidth());
            }
        });

        progressBar.getChildren().addAll(currentTimeLabel, progressIndicator, totalTimeLabel);
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        VBox root = new VBox();
        root.getChildren().addAll(songInfoBar, scrollPane, progressBar);
        this.getChildren().addAll(root, emptyState, loadingState);
    }

    public void setSongInfo(Song song) {
        if (song != null) {
            songTitleLabel.setText(song.getDisplayTitle());
            songArtistLabel.setText(song.getDisplayArtist());
            songInfoBar.setVisible(true);
            songInfoBar.setManaged(true);
            progressBar.setVisible(true);
            progressBar.setManaged(true);
        } else {
            songInfoBar.setVisible(false);
            songInfoBar.setManaged(false);
            progressBar.setVisible(false);
            progressBar.setManaged(false);
        }
    }

    public void setLyrics(List<LyricLine> lines) {
        Platform.runLater(() -> {
            lyrics.clear();
            if (lines != null) {
                lyrics.addAll(lines);
            }
            isLoading = false;
            loadingState.setVisible(false);
            loadingState.setManaged(false);
            currentIndex = -1;
            rebuildUI();
        });
    }

    public void showLoading() {
        isLoading = true;
        Platform.runLater(() -> {
            lyrics.clear();
            lyricsContainer.getChildren().clear();
            emptyState.setVisible(false);
            loadingState.setVisible(true);
            loadingState.setManaged(true);
        });
    }

    private void rebuildUI() {
        lyricsContainer.getChildren().clear();
        if (lyrics.isEmpty()) {
            emptyState.setVisible(true);
            loadingState.setVisible(false);
            loadingState.setManaged(false);
            return;
        }
        emptyState.setVisible(false);

        for (int i = 0; i < lyrics.size(); i++) {
            LyricLine line = lyrics.get(i);
            Label label = new Label(line.getText());
            label.setWrapText(true);
            label.setMaxWidth(400);
            label.setAlignment(Pos.CENTER);
            label.setStyle(getLineStyle(i, -1));
            label.setPadding(new Insets(4, 8, 4, 8));
            lyricsContainer.getChildren().add(label);
        }
    }

    public void updateProgress(double timeMs) {
        if (lyrics.isEmpty()) return;
        int newIndex = findCurrentIndex(timeMs);
        if (newIndex == currentIndex) return;
        currentIndex = newIndex;
        Platform.runLater(() -> highlightLine(currentIndex));
    }

    public void updateProgressbar(double currentSec, double totalSec) {
        Platform.runLater(() -> {
            currentTimeLabel.setText(formatTime(currentSec));
            totalTimeLabel.setText(formatTime(totalSec));
            if (totalSec > 0) {
                progressIndicator.setProgress(currentSec / totalSec);
            }
        });
    }

    private int findCurrentIndex(double timeMs) {
        int idx = -1;
        for (int i = 0; i < lyrics.size(); i++) {
            if (lyrics.get(i).getTimeMs() <= timeMs) {
                idx = i;
            } else {
                break;
            }
        }
        return idx;
    }

    private void highlightLine(int index) {
        if (index < 0 || index >= lyricsContainer.getChildren().size()) return;

        for (int i = 0; i < lyricsContainer.getChildren().size(); i++) {
            var node = lyricsContainer.getChildren().get(i);
            if (node instanceof Label label) {
                label.setStyle(getLineStyle(i, index));
            }
        }

        scrollToIndex(index);
    }

    private void scrollToIndex(int index) {
        if (lyricsContainer.getChildren().isEmpty()) return;
        if (index < 0 || index >= lyricsContainer.getChildren().size()) return;

        lyricsContainer.applyCss();
        lyricsContainer.layout();

        var target = lyricsContainer.getChildren().get(index);
        double targetHeight = target.getBoundsInLocal().getHeight();
        double targetCenterY = target.getLayoutY() + targetHeight / 2;

        double viewportHeight = scrollPane.getHeight();
        // 高亮行固定在视口中央位置
        double centerY = viewportHeight / 2.0;
        // 计算需要的偏移量：让目标行中心对齐视口中心
        // translateY 为负数表示整体向上移动
        double translateY = centerY - targetCenterY;

        double currentTranslateY = lyricsContainer.getTranslateY();
        double duration = Math.abs(translateY - currentTranslateY) > 50 ? 300 : 150;

        Timeline scrollTimeline = new Timeline();
        KeyValue translateKV = new KeyValue(lyricsContainer.translateYProperty(), translateY);
        scrollTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(duration), translateKV));
        scrollTimeline.play();
    }

    private String getLineStyle(int lineIndex, int highlightIndex) {
        if (highlightIndex < 0) {
            return "-fx-font-size: 13px; -fx-text-fill: -text-tertiary;";
        }
        if (lineIndex == highlightIndex) {
            return "-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -primary;";
        }
        int distance = Math.abs(lineIndex - highlightIndex);
        if (distance == 1) {
            return "-fx-font-size: 14px; -fx-text-fill: -text-secondary;";
        }
        return "-fx-font-size: 13px; -fx-text-fill: -text-tertiary;";
    }

    private String formatTime(double seconds) {
        if (seconds <= 0) return "0:00";
        int min = (int) (seconds / 60);
        int sec = (int) (seconds % 60);
        return min + ":" + String.format("%02d", sec);
    }

    public void clear() {
        Platform.runLater(() -> {
            lyrics.clear();
            currentIndex = -1;
            isLoading = false;
            loadingState.setVisible(false);
            loadingState.setManaged(false);
            lyricsContainer.getChildren().clear();
            emptyState.setVisible(true);
            lyricsContainer.setTranslateY(0);
            scrollPane.setVvalue(0);
        });
    }

    @FunctionalInterface
    public interface SeekCallback {
        void onSeek(double progress);
    }

    public void setSeekCallback(SeekCallback callback) {
        this.seekCallback = callback;
    }
}
