package com.qiyunge.ui.face;

import com.qiyunge.application.face.FaceRecognitionService;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

import org.bytedeco.javacpp.BytePointer;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * 人脸采集对话框：用于注册用户时采集多个人脸样本。
 */
public class FaceCaptureDialog extends Stage {

    private static final int REQUIRED_SAMPLES = 8;
    private static final int CAPTURE_DELAY_MS = 400;

    private final FaceRecognitionService faceService;
    private final int userId;
    private final Consumer<List<Path>> onComplete;

    private ImageView cameraView;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Button captureButton;
    private Button cancelButton;

    private AnimationTimer timer;
    private final List<Path> capturedSamples = new ArrayList<>();
    private volatile boolean isCapturing = false;
    private long lastCaptureTime = 0;

    public FaceCaptureDialog(Stage owner, FaceRecognitionService faceService,
                              int userId, Consumer<List<Path>> onComplete) {
        this.faceService = faceService;
        this.userId = userId;
        this.onComplete = onComplete;

        initOwner(owner);
        initModality(Modality.APPLICATION_MODAL);
        initStyle(StageStyle.UNDECORATED);
        setTitle("录入人脸");

        VBox root = createUI();
        Scene scene = new Scene(root, 520, 520);
        scene.getStylesheets().add(getClass().getResource("/styles/face-dialog.css").toExternalForm());
        setScene(scene);

        centerOnScreen();
    }

    private VBox createUI() {
        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("face-dialog-root");

        Label title = new Label("人脸录入");
        title.getStyleClass().add("face-dialog-title");

        cameraView = new ImageView();
        cameraView.setFitWidth(480);
        cameraView.setFitHeight(360);
        cameraView.setPreserveRatio(true);
        cameraView.getStyleClass().add("camera-view");

        statusLabel = new Label("请正对摄像头，点击开始采集");
        statusLabel.getStyleClass().add("face-status-label");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(480);
        progressBar.getStyleClass().add("face-progress");

        captureButton = new Button("开始采集");
        captureButton.getStyleClass().add("face-primary-button");
        captureButton.setOnAction(e -> startCapture());

        cancelButton = new Button("取消");
        cancelButton.getStyleClass().add("face-secondary-button");
        cancelButton.setOnAction(e -> closeDialog());

        javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(12, captureButton, cancelButton);
        buttons.setAlignment(Pos.CENTER);

        root.getChildren().addAll(title, cameraView, statusLabel, progressBar, buttons);
        return root;
    }

    public void startPreview() {
        if (!faceService.openCamera()) {
            statusLabel.setText("无法打开摄像头，请检查设备");
            statusLabel.setTextFill(Color.web("#E74C3C"));
            return;
        }

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateFrame();
            }
        };
        timer.start();
    }

    private void updateFrame() {
        FaceRecognitionService.FrameResult result = faceService.captureAndDetect();
        if (result == null) return;

        // 在人脸上画框
        Mat displayFrame = result.frame.clone();
        for (int i = 0; i < result.faces.size(); i++) {
            Rect r = result.faces.get(i);
            int color = isCapturing ? 0x00FF00 : 0x5B8DEF;  // 绿色(采集中)或蓝色
            rectangle(displayFrame, r, new org.bytedeco.opencv.opencv_core.Scalar(color), 2, LINE_8, 0);
        }

        Image fxImage = matToImage(displayFrame);
        cameraView.setImage(fxImage);

        displayFrame.release();

        // 自动采集逻辑
        if (isCapturing && result.faces.size() > 0) {
            long now = System.currentTimeMillis();
            if (now - lastCaptureTime > CAPTURE_DELAY_MS && capturedSamples.size() < REQUIRED_SAMPLES) {
                captureSample(result);
                lastCaptureTime = now;
            }
        }

        result.release();
    }

    private void captureSample(FaceRecognitionService.FrameResult result) {
        // 取最大的人脸
        Rect mainFace = null;
        double maxArea = 0;
        for (int i = 0; i < result.faces.size(); i++) {
            Rect r = result.faces.get(i);
            double area = r.width() * r.height();
            if (area > maxArea) {
                maxArea = area;
                mainFace = r;
            }
        }

        if (mainFace == null) return;

        try {
            Mat faceMat = faceService.extractFace(result.frame, mainFace);
            Path samplePath = faceService.getFaceDataBasePath().resolve("user_" + userId);

            Files.createDirectories(samplePath);
            Path file = samplePath.resolve("sample_" + capturedSamples.size() + ".png");
            // 使用 imencode + Files.write 绕过 imwrite 不支持中文路径的问题
            BytePointer buffer = new BytePointer();
            imencode(".png", faceMat, buffer);
            byte[] bytes = new byte[(int) buffer.limit()];
            buffer.get(bytes);
            buffer.deallocate();
            Files.write(file, bytes);
            faceMat.release();

            capturedSamples.add(file);
            double progress = (double) capturedSamples.size() / REQUIRED_SAMPLES;
            progressBar.setProgress(progress);
            statusLabel.setText("已采集 " + capturedSamples.size() + " / " + REQUIRED_SAMPLES + " 张样本");

            if (capturedSamples.size() >= REQUIRED_SAMPLES) {
                finishCapture();
            }
        } catch (Exception e) {
            System.err.println("[FaceCapture] Sample capture failed: " + e.getMessage());
        }
    }

    private void startCapture() {
        isCapturing = true;
        capturedSamples.clear();
        captureButton.setDisable(true);
        captureButton.setText("采集中...");
        statusLabel.setText("请保持面部在框内，自动采集中...");
    }

    private void finishCapture() {
        isCapturing = false;
        statusLabel.setText("采集完成！");
        stopPreview();

        if (onComplete != null) {
            onComplete.accept(new ArrayList<>(capturedSamples));
        }
        close();
    }

    private void closeDialog() {
        stopPreview();
        close();
    }

    private void stopPreview() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        faceService.closeCamera();
    }

    private Image matToImage(Mat mat) {
        BytePointer buffer = new BytePointer();
        imencode(".png", mat, buffer);
        byte[] bytes = new byte[(int) buffer.limit()];
        buffer.get(bytes);
        buffer.deallocate();
        return new Image(new ByteArrayInputStream(bytes));
    }

    @Override
    public void close() {
        stopPreview();
        super.close();
    }
}
