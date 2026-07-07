package com.qiyunge.ui.face;

import com.qiyunge.application.face.FaceRecognitionService;
import com.qiyunge.application.face.FaceRecognitionService.RecognitionResult;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 人脸登录紧凑型面板：不展示摄像头画面，仅显示识别状态。
 * 摄像头在后台静默运行，用户只看到识别进度提示。
 */
public class FaceLoginDialog extends Stage {

    private static final int RECOGNITION_SUCCESS_THRESHOLD = 2;
    private static final int RECOGNITION_TIMEOUT_MS = 15000;

    private final FaceRecognitionService faceService;
    private final Consumer<RecognitionResult> onResult;

    private Label titleLabel;
    private Label statusLabel;
    private Label hintLabel;
    private Button cancelButton;
    private Arc spinnerArc;
    private RotateTransition spinnerAnim;

    private Timeline timeoutTimer;
    private final AtomicInteger successCount = new AtomicInteger(0);
    private volatile boolean finished = false;

    public FaceLoginDialog(Stage owner, FaceRecognitionService faceService,
                            Consumer<RecognitionResult> onResult) {
        this.faceService = faceService;
        this.onResult = onResult;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);
        setTitle("刷脸入阁");

        VBox root = createUI();
        Scene scene = new Scene(root, 340, 220);
        scene.getStylesheets().add(getClass().getResource("/styles/face-dialog.css").toExternalForm());
        setScene(scene);

        centerOnScreen();
    }

    private VBox createUI() {
        VBox root = new VBox(14);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("face-dialog-root");
        root.setPrefWidth(340);
        root.setPrefHeight(220);

        titleLabel = new Label("刷脸入阁");
        titleLabel.getStyleClass().add("face-dialog-title");

        // 旋转加载动画（代替摄像头画面）
        spinnerArc = new Arc(0, 0, 24, 24, 0, 270);
        spinnerArc.setType(ArcType.OPEN);
        spinnerArc.setStroke(Color.web("#5B8DEF"));
        spinnerArc.setStrokeWidth(3);
        spinnerArc.setFill(null);

        spinnerAnim = new RotateTransition(Duration.millis(800), spinnerArc);
        spinnerAnim.setByAngle(360);
        spinnerAnim.setCycleCount(Animation.INDEFINITE);
        spinnerAnim.setInterpolator(Interpolator.LINEAR);

        statusLabel = new Label("正在启动摄像头...");
        statusLabel.getStyleClass().add("face-status-label");

        hintLabel = new Label("请将面部对准摄像头");
        hintLabel.getStyleClass().add("face-hint-label");

        cancelButton = new Button("取消");
        cancelButton.getStyleClass().add("face-secondary-button");
        cancelButton.setOnAction(e -> closeDialog());

        root.getChildren().addAll(titleLabel, spinnerArc, statusLabel, hintLabel, cancelButton);
        return root;
    }

    public void startRecognition() {
        if (!faceService.openCamera()) {
            spinnerArc.setVisible(false);
            spinnerAnim.stop();
            statusLabel.setText("无法打开摄像头");
            statusLabel.setTextFill(Color.web("#E74C3C"));
            hintLabel.setText("请检查摄像头设备或尝试密码登录");
            return;
        }

        spinnerAnim.play();
        statusLabel.setText("正在识别...");
        statusLabel.setTextFill(Color.web("#5B8DEF"));

        // 启动超时计时器
        timeoutTimer = new Timeline(new KeyFrame(Duration.millis(RECOGNITION_TIMEOUT_MS), e -> {
            if (!finished) {
                finish(RecognitionResult.unknown());
            }
        }));
        timeoutTimer.play();

        // 启动后台识别循环（不返回帧图像）
        faceService.startRecognitionLoop(
            this::onRecognitionResult,
            null  // 不预览画面
        );
    }

    private void onRecognitionResult(RecognitionResult result) {
        if (finished) return;

        Platform.runLater(() -> {
            switch (result.status) {
                case SUCCESS -> {
                    int count = successCount.incrementAndGet();
                    statusLabel.setText("识别成功 (" + count + "/" + RECOGNITION_SUCCESS_THRESHOLD + ")");
                    statusLabel.setTextFill(Color.web("#27AE60"));
                    hintLabel.setText("即将入阁...");
                    hintLabel.setTextFill(Color.web("#27AE60"));

                    if (count >= RECOGNITION_SUCCESS_THRESHOLD) {
                        finish(result);
                    }
                }
                case NO_FACE -> {
                    successCount.set(0);
                    hintLabel.setText("请将面部对准摄像头");
                    hintLabel.setTextFill(Color.web("#999"));
                }
                case NO_MODEL -> {
                    statusLabel.setText("暂无人脸数据，请先录入");
                    statusLabel.setTextFill(Color.web("#E67E22"));
                    hintLabel.setText("在个人中心「吾庐」录入人脸");
                    spinnerAnim.stop();
                }
                case UNKNOWN -> {
                    hintLabel.setText("识别中，请稍候...");
                    hintLabel.setTextFill(Color.web("#999"));
                }
                case NO_CAMERA -> {
                    statusLabel.setText("摄像头异常");
                    statusLabel.setTextFill(Color.web("#E74C3C"));
                    spinnerAnim.stop();
                }
            }
        });
    }

    private void finish(RecognitionResult result) {
        if (finished) return;
        finished = true;

        stopAll();

        if (result.isSuccess()) {
            spinnerAnim.stop();
            spinnerArc.setVisible(false);
            statusLabel.setText("欢迎入阁！");
            statusLabel.setTextFill(Color.web("#27AE60"));
            hintLabel.setText("");
            cancelButton.setVisible(false);

            PauseTransition delay = new PauseTransition(Duration.millis(800));
            delay.setOnFinished(e -> {
                if (onResult != null) onResult.accept(result);
                close();
            });
            delay.play();
        } else {
            spinnerAnim.stop();
            statusLabel.setText("识别失败");
            statusLabel.setTextFill(Color.web("#E74C3C"));
            hintLabel.setText("请重试或使用密码登录");
            if (onResult != null) onResult.accept(result);
            close();
        }
    }

    private void closeDialog() {
        if (finished) return;
        finished = true;
        stopAll();
        if (onResult != null) {
            onResult.accept(RecognitionResult.unknown());
        }
        close();
    }

    private void stopAll() {
        if (timeoutTimer != null) {
            timeoutTimer.stop();
            timeoutTimer = null;
        }
        if (spinnerAnim != null) {
            spinnerAnim.stop();
        }
        faceService.stopRecognitionLoop();
        faceService.closeCamera();
    }

    @Override
    public void close() {
        stopAll();
        super.close();
    }
}
