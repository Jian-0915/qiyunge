package com.qiyunge.ui.components;

import com.qiyunge.application.service.MusicPlayerService;
import com.qiyunge.domain.entity.Song;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

/**
 * 底部浮声栏：全局音乐播放控制条。
 * 左侧显示当前歌曲封面与信息，中间为播放控制与进度条，右侧为音量与其他功能。
 */
public class MusicBar extends HBox {

    private final Label songTitle;
    private final Label songArtist;
    private final Label playBtn;
    private final ProgressBar progressBar;
    private final Slider volumeSlider;
    private final Label currentTime;
    private final Label totalTime;
    private final Label errorHint;
    private final Label favBtn;

    public MusicBar(
        ObjectProperty<Song> currentSong,
        BooleanProperty playing,
        DoubleProperty progress,
        StringProperty currentTimeText,
        StringProperty totalTimeText,
        DoubleProperty volume,
        StringProperty errorMessage,
        ObjectProperty<MusicPlayerService.PlayMode> playMode,
        ObjectProperty<Image> coverImage,
        Runnable onTogglePause,
        Runnable onPlayNext,
        Runnable onPlayPrevious,
        Runnable onCyclePlayMode,
        javafx.util.Callback<Double, Void> onSeek,
        Runnable onToggleLyrics,
        Runnable onToggleQueue
    ) {
        this.getStyleClass().add("music-bar");
        this.setPrefHeight(72);
        this.setMinHeight(72);
        this.setMaxHeight(72);
        this.setAlignment(Pos.CENTER);
        this.setPadding(new Insets(0, 24, 0, 24));
        this.setSpacing(20);
        // 样式完全由 CSS .music-bar 控制

        // ===== 左侧：歌曲信息（含封面 + 收藏按钮） =====
        HBox songInfo = new HBox(12);
        songInfo.setAlignment(Pos.CENTER_LEFT);
        songInfo.setPrefWidth(220);
        songInfo.setMinWidth(220);

        ImageView coverView = new ImageView();
        coverView.setFitWidth(40);
        coverView.setFitHeight(40);
        coverView.setPreserveRatio(true);
        coverView.setStyle("-fx-background-color: -bg-tertiary; -fx-background-radius: 6px;");
        coverView.setSmooth(true);
        // 绑定封面图片属性
        coverView.imageProperty().bind(coverImage);

        VBox songText = new VBox(2);
        songTitle = new Label("未在播放");
        songTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        songArtist = new Label("--");
        songArtist.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");
        songText.getChildren().addAll(songTitle, songArtist);

        // 收藏按钮
        favBtn = new Label("\u2661"); // ♡ 空心
        favBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: -text-tertiary; -fx-cursor: hand;");
        favBtn.setOnMouseEntered(e -> favBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: #ef4444; -fx-cursor: hand;"));
        favBtn.setOnMouseExited(e -> {
            Song song = currentSong.get();
            boolean isFav = song != null && song.isFavorited();
            favBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (isFav ? "#ef4444" : "-text-tertiary") + "; -fx-cursor: hand;");
        });
        // 收藏按钮点击事件需要外部传入，这里仅做视觉展示
        // 实际收藏功能通过歌曲表格操作
        favBtn.setDisable(true);
        favBtn.setOpacity(0.5);
        Tooltip favTooltip = new Tooltip("请通过歌曲列表收藏");
        favBtn.setTooltip(favTooltip);

        songInfo.getChildren().addAll(coverView, songText, favBtn);

        // ===== 中间：播放控制 + 进度条 + 时间 =====
        VBox centerControls = new VBox(6);
        centerControls.setAlignment(Pos.CENTER);
        centerControls.setMaxWidth(480);
        centerControls.setPrefWidth(480);

        HBox controls = new HBox(20);
        controls.setAlignment(Pos.CENTER);

        Label prevBtn = new Label("\u23EE");
        prevBtn.getStyleClass().add("icon-button");
        prevBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: -text-primary;");
        prevBtn.setOnMouseClicked(e -> onPlayPrevious.run());

        playBtn = new Label("\u25B6");
        playBtn.getStyleClass().add("icon-button");
        playBtn.setStyle("-fx-font-size: 22px; -fx-cursor: hand; -fx-text-fill: -primary;");
        playBtn.setOnMouseClicked(e -> onTogglePause.run());

        Label nextBtn = new Label("\u23ED");
        nextBtn.getStyleClass().add("icon-button");
        nextBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: -text-primary;");
        nextBtn.setOnMouseClicked(e -> onPlayNext.run());

        // 播放模式按钮
        Label modeBtn = new Label("\uD83D\uDD04");  // 🔃 顺序
        modeBtn.getStyleClass().add("icon-button");
        modeBtn.setStyle("-fx-font-size: 14px; -fx-cursor: hand; -fx-text-fill: -text-secondary;");
        modeBtn.setOnMouseClicked(e -> onCyclePlayMode.run());
        // Tooltip 显示当前模式
        Tooltip modeTooltip = new Tooltip();
        modeTooltip.textProperty().bind(Bindings.createStringBinding(() -> {
            return switch (playMode.get()) {
                case SEQUENTIAL -> "顺序播放";
                case SINGLE_LOOP -> "单曲循环";
                case SHUFFLE -> "随机播放";
            };
        }, playMode));
        Tooltip.install(modeBtn, modeTooltip);
        // 图标随模式变化
        playMode.addListener((obs, old, mode) -> {
            modeBtn.setText(switch (mode) {
                case SEQUENTIAL -> "\uD83D\uDD04";  // 🔃
                case SINGLE_LOOP -> "\uD83D\uDD01";  // 🔁
                case SHUFFLE -> "\uD83D\uDD00";  // 🔀
            });
        });

        controls.getChildren().addAll(prevBtn, playBtn, nextBtn, modeBtn);

        HBox progressRow = new HBox(10);
        progressRow.setAlignment(Pos.CENTER);
        progressRow.setPrefWidth(480);

        currentTime = new Label("0:00");
        currentTime.getStyleClass().add("player-time");
        currentTime.textProperty().bind(currentTimeText);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(360);
        progressBar.setPrefHeight(8);
        progressBar.setMaxHeight(8);
        progressBar.getStyleClass().add("player-progress");
        progressBar.progressProperty().bind(progress);
        progressBar.setOnMouseReleased(e -> {
            double ratio = e.getX() / progressBar.getWidth();
            onSeek.call(Math.max(0, Math.min(1, ratio)));
        });

        totalTime = new Label("0:00");
        totalTime.getStyleClass().add("player-time");
        totalTime.textProperty().bind(totalTimeText);

        progressRow.getChildren().addAll(currentTime, progressBar, totalTime);
        HBox.setHgrow(progressBar, Priority.ALWAYS);

        centerControls.getChildren().addAll(controls, progressRow);

        // ===== 右侧：音量滑块 + 歌词按钮 + 队列按钮 =====
        HBox rightArea = new HBox(16);
        rightArea.setAlignment(Pos.CENTER_RIGHT);
        rightArea.setPrefWidth(220);
        rightArea.setMinWidth(220);

        // 音量区域
        HBox volumeArea = new HBox(8);
        volumeArea.setAlignment(Pos.CENTER_RIGHT);
        Label volumeIcon = new Label("\uD83D\uDD0A");
        volumeIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary;");
        volumeSlider = new Slider(0, 1, 0.7);
        volumeSlider.setPrefWidth(90);
        volumeSlider.setPrefHeight(16);
        volumeSlider.valueProperty().bindBidirectional(volume);
        volumeArea.getChildren().addAll(volumeIcon, volumeSlider);

        // 歌词按钮
        Label lyricBtn = new Label("\u266A"); // ♪
        lyricBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: -text-secondary;");
        lyricBtn.setOnMouseEntered(e -> lyricBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: -primary;"));
        lyricBtn.setOnMouseExited(e -> lyricBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: -text-secondary;"));
        Tooltip lyricTooltip = new Tooltip("歌词");
        Tooltip.install(lyricBtn, lyricTooltip);
        lyricBtn.setOnMouseClicked(e -> {
            if (onToggleLyrics != null) onToggleLyrics.run();
        });

        // 队列按钮
        Label queueBtn = new Label("\u2630"); // ☰
        queueBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: -text-secondary;");
        queueBtn.setOnMouseEntered(e -> queueBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: -primary;"));
        queueBtn.setOnMouseExited(e -> queueBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: -text-secondary;"));
        Tooltip queueTooltip = new Tooltip("播放队列");
        Tooltip.install(queueBtn, queueTooltip);
        queueBtn.setOnMouseClicked(e -> {
            if (onToggleQueue != null) onToggleQueue.run();
        });

        rightArea.getChildren().addAll(volumeArea, lyricBtn, queueBtn);

        // 左右弹性区域，使中间控件居中
        Region spacerLeft = new Region();
        HBox.setHgrow(spacerLeft, Priority.ALWAYS);
        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        this.getChildren().addAll(songInfo, spacerLeft, centerControls, spacerRight, rightArea);

        // ===== 绑定歌曲信息 =====
        currentSong.addListener((obs, old, song) -> {
            if (song != null) {
                songTitle.setText(song.getDisplayTitle());
                songArtist.setText(song.getDisplayArtist());
                // 更新收藏按钮状态
                boolean isFav = song.isFavorited();
                favBtn.setText(isFav ? "\u2665" : "\u2661"); // ♥ 或 ♡
                favBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (isFav ? "#ef4444" : "-text-tertiary") + "; -fx-cursor: hand;");
            } else {
                songTitle.setText("未在播放");
                songArtist.setText("--");
                favBtn.setText("\u2661");
                favBtn.setStyle("-fx-font-size: 16px; -fx-text-fill: -text-tertiary; -fx-cursor: hand;");
            }
        });

        // 绑定播放/暂停按钮文本
        playing.addListener((obs, old, isPlaying) -> {
            playBtn.setText(isPlaying ? "\u23F8" : "\u25B6");
        });

        // 错误提示
        errorHint = new Label();
        errorHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444;");
        errorHint.textProperty().bind(errorMessage);
        errorHint.setVisible(false);
        errorHint.setManaged(false);
        errorMessage.addListener((obs, old, val) -> {
            boolean hasError = val != null && !val.isEmpty();
            errorHint.setVisible(hasError);
            errorHint.setManaged(hasError);
        });
    }
}
