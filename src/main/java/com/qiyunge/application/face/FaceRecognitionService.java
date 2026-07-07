package com.qiyunge.application.face;

import com.qiyunge.domain.entity.User;
import com.qiyunge.domain.entity.UserFaceData;
import com.qiyunge.infrastructure.repository.UserFaceDataRepository;
import com.qiyunge.infrastructure.storage.AppStorage;
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

/**
 * 人脸识别服务：基于 OpenCV LBPH 算法 + sarxos Webcam Capture 摄像头捕获。
 * Webcam Capture 比 OpenCV VideoCapture 启动更快、更轻量。
 */
public class FaceRecognitionService {

    // Webcam Capture 分辨率（降低以减少数据量）
    private static final Dimension CAPTURE_SIZE = WebcamResolution.VGA.getSize(); // 640x480
    private static final int FACE_WIDTH = 200;
    private static final int FACE_HEIGHT = 200;
    private static final double RECOGNITION_THRESHOLD = 65.0;
    private static final int MIN_SAMPLES_FOR_TRAINING = 5;

    private final AppStorage appStorage;
    private final UserFaceDataRepository faceDataRepository;
    private final Path faceDataPath;
    private final ExecutorService executor;

    // Webcam Capture
    private volatile Webcam webcam;
    private volatile boolean cameraOpen = false;

    // OpenCV
    private volatile CascadeClassifier faceDetector;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private volatile boolean nativeReady = false;

    // 图像转换器（BufferedImage <-> Mat）
    private final Java2DFrameConverter java2dConverter = new Java2DFrameConverter();
    private final OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();

    public FaceRecognitionService(AppStorage appStorage, UserFaceDataRepository faceDataRepository) {
        this.appStorage = appStorage;
        this.faceDataRepository = faceDataRepository;
        this.faceDataPath = appStorage.getFaceDataPath();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "face-recognition");
            t.setDaemon(true);
            return t;
        });
        ensureDirectories();
        executor.execute(this::initNative);
    }

    private void initNative() {
        try {
            long start = System.currentTimeMillis();
            Loader.load(org.bytedeco.opencv.global.opencv_core.class);
            Loader.load(org.bytedeco.opencv.global.opencv_imgproc.class);
            Loader.load(org.bytedeco.opencv.global.opencv_imgcodecs.class);
            Loader.load(org.bytedeco.opencv.global.opencv_objdetect.class);
            Loader.load(org.bytedeco.opencv.global.opencv_face.class);
            System.out.println("[FaceRecognition] Native libraries loaded in " + (System.currentTimeMillis() - start) + "ms");
        } catch (Exception e) {
            System.err.println("[FaceRecognition] Failed to load native libs: " + e.getMessage());
        }
        initFaceDetector();
        nativeReady = true;
    }

    public boolean awaitReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!nativeReady && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(50); } catch (InterruptedException e) { return false; }
        }
        return nativeReady;
    }

    public boolean isReady() { return nativeReady; }

    private void ensureDirectories() {
        try {
            Files.createDirectories(faceDataPath);
        } catch (IOException e) {
            System.err.println("[FaceRecognition] Failed to create face data directory: " + e.getMessage());
        }
    }

    private void initFaceDetector() {
        executor.execute(() -> {
            try {
                String classifierName = "haarcascade_frontalface_default.xml";
                Path classifierPath = extractClassifier(classifierName);
                if (classifierPath != null) {
                    faceDetector = new CascadeClassifier(classifierPath.toString());
                    System.out.println("[FaceRecognition] Face detector initialized");
                }
            } catch (Exception e) {
                System.err.println("[FaceRecognition] Failed to init face detector: " + e.getMessage());
            }
        });
    }

    private Path extractClassifier(String name) throws IOException {
        Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "qiyunge_face");
        Files.createDirectories(tempDir);
        Path target = tempDir.resolve(name);
        if (Files.exists(target)) return target;

        Path userDataPath = faceDataPath.resolve(name);
        if (Files.exists(userDataPath)) {
            Files.copy(userDataPath, target);
            return target;
        }

        try (var in = getClass().getResourceAsStream("/" + name)) {
            if (in != null) {
                Files.copy(in, target);
                return target;
            }
        }

        String[] possiblePaths = {
            "org/bytedeco/opencv/share/opencv4/haarcascades/" + name,
            "org/bytedeco/opencv/share/opencv4/lbpcascades/" + name
        };
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        for (String path : possiblePaths) {
            try (InputStream in = cl.getResourceAsStream(path)) {
                if (in != null) {
                    Files.copy(in, target);
                    return target;
                }
            }
        }

        System.err.println("[FaceRecognition] Could not find classifier: " + name);
        return null;
    }

    // ==================== 摄像头管理（Webcam Capture）====================

    public boolean openCamera() {
        if (cameraOpen && webcam != null && webcam.isOpen()) {
            return true;
        }
        try {
            webcam = Webcam.getDefault();
            if (webcam == null) {
                System.err.println("[FaceRecognition] No webcam found");
                return false;
            }
            webcam.setViewSize(CAPTURE_SIZE);
            webcam.open(true);  // true = async open, non-blocking
            cameraOpen = webcam.isOpen();
            if (cameraOpen) {
                System.out.println("[FaceRecognition] Camera opened (Webcam Capture)");
            } else {
                System.err.println("[FaceRecognition] Failed to open camera");
            }
            return cameraOpen;
        } catch (Exception e) {
            System.err.println("[FaceRecognition] Camera open error: " + e.getMessage());
            return false;
        }
    }

    public void closeCamera() {
        if (webcam != null) {
            try {
                if (webcam.isOpen()) {
                    webcam.close();
                }
            } catch (Exception e) {
                System.err.println("[FaceRecognition] Camera close error: " + e.getMessage());
            }
            webcam = null;
        }
        cameraOpen = false;
        System.out.println("[FaceRecognition] Camera closed");
    }

    public boolean isCameraOpen() {
        return cameraOpen && webcam != null && webcam.isOpen();
    }

    // ==================== 帧捕获与检测 ====================

    public FrameResult captureAndDetect() {
        if (!isCameraOpen() || faceDetector == null || faceDetector.isNull()) {
            return null;
        }

        BufferedImage image = webcam.getImage();
        if (image == null) {
            return null;
        }

        // BufferedImage -> Frame -> Mat
        Frame frame = java2dConverter.convert(image);
        Mat mat = matConverter.convert(frame);

        // 水平翻转（镜像效果）
        Mat flipped = new Mat();
        flip(mat, flipped, 1);
        mat.release();

        RectVector faces = detectFaces(flipped);
        return new FrameResult(flipped, faces);
    }

    private RectVector detectFaces(Mat frame) {
        Mat gray = new Mat();
        cvtColor(frame, gray, COLOR_BGR2GRAY);
        equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        if (faceDetector != null && !faceDetector.isNull()) {
            faceDetector.detectMultiScale(gray, faces,
                1.1,
                3,
                0,
                new Size(100, 100),
                new Size(CAPTURE_SIZE.width, CAPTURE_SIZE.height)
            );
        }
        gray.release();
        return faces;
    }

    public Mat extractFace(Mat frame, Rect faceRect) {
        Mat gray = new Mat();
        cvtColor(frame, gray, COLOR_BGR2GRAY);

        Mat faceROI = new Mat(gray, faceRect);
        Mat resized = new Mat();
        resize(faceROI, resized, new Size(FACE_WIDTH, FACE_HEIGHT));

        faceROI.release();
        gray.release();
        return resized;
    }

    // ==================== 人脸注册（训练）====================

    public boolean trainModel(int userId, List<Path> faceImages) {
        if (faceImages.size() < MIN_SAMPLES_FOR_TRAINING) {
            System.err.println("[FaceRecognition] Need at least " + MIN_SAMPLES_FOR_TRAINING + " samples");
            return false;
        }

        try {
            // 先过滤掉无法读取的图片，避免空 Mat 导致 train 崩溃
            List<Mat> validImages = new ArrayList<>();
            List<Integer> validLabels = new ArrayList<>();

            for (Path imagePath : faceImages) {
                // 使用 Files.readAllBytes + imdecode 绕过 imread 不支持中文路径的问题
                Mat img;
                try {
                    byte[] bytes = Files.readAllBytes(imagePath);
                    img = imdecode(new Mat(bytes), opencv_imgcodecs.IMREAD_GRAYSCALE);
                } catch (IOException e) {
                    System.err.println("[FaceRecognition] Skipping unreadable image: " + imagePath + " (" + e.getMessage() + ")");
                    continue;
                }
                if (img.empty()) {
                    System.err.println("[FaceRecognition] Skipping unreadable image: " + imagePath);
                    img.release();
                    continue;
                }
                Mat resized = new Mat();
                resize(img, resized, new Size(FACE_WIDTH, FACE_HEIGHT));
                validImages.add(resized);
                validLabels.add(userId);
                img.release();
            }

            if (validImages.size() < MIN_SAMPLES_FOR_TRAINING) {
                System.err.println("[FaceRecognition] Only " + validImages.size() + " valid images, need " + MIN_SAMPLES_FOR_TRAINING);
                validImages.forEach(Mat::release);
                return false;
            }

            MatVector images = new MatVector(validImages.size());
            Mat labels = new Mat(validImages.size(), 1, CV_32SC1);
            IntBuffer labelsBuf = labels.createBuffer();

            for (int i = 0; i < validImages.size(); i++) {
                images.put(i, validImages.get(i));
                labelsBuf.put(i, validLabels.get(i));
            }

            LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
            recognizer.train(images, labels);

            Path modelFile = faceDataPath.resolve("user_" + userId + "_model.yml");
            // recognizer.write() 不支持中文路径，先写到临时目录再复制
            try {
                Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "qiyunge_face");
                Files.createDirectories(tempDir);
                Path tempModel = tempDir.resolve("user_" + userId + "_model.yml");
                recognizer.write(tempModel.toString());
                Files.copy(tempModel, modelFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(tempModel);
            } catch (IOException e) {
                System.err.println("[FaceRecognition] Failed to save model: " + e.getMessage());
                recognizer.close();
                images.close();
                labels.release();
                validImages.forEach(Mat::release);
                return false;
            }

            recognizer.close();
            images.close();
            labels.release();

            UserFaceData data = new UserFaceData();
            data.setUserId(userId);
            data.setModelPath(modelFile.toString());
            data.setFaceImagePath(faceDataPath.resolve("user_" + userId).toString());
            data.setSampleCount(validImages.size());
            data.setEnabled(true);
            faceDataRepository.save(data);

            System.out.println("[FaceRecognition] Model trained for user " + userId + " with " + validImages.size() + " valid images");
            return true;
        } catch (Exception e) {
            System.err.println("[FaceRecognition] Training failed: " + e.getMessage());
            return false;
        }
    }

    public CompletableFuture<Boolean> trainModelAsync(int userId, List<Path> faceImages) {
        return CompletableFuture.supplyAsync(() -> trainModel(userId, faceImages), executor);
    }

    // ==================== 人脸识别 ====================

    public RecognitionResult recognize() {
        if (!isCameraOpen()) {
            return RecognitionResult.noCamera();
        }

        FrameResult frameResult = captureAndDetect();
        if (frameResult == null || frameResult.faces.size() == 0) {
            return RecognitionResult.noFace();
        }

        Rect mainFace = null;
        double maxArea = 0;
        for (int i = 0; i < frameResult.faces.size(); i++) {
            Rect r = frameResult.faces.get(i);
            double area = r.width() * r.height();
            if (area > maxArea) {
                maxArea = area;
                mainFace = r;
            }
        }

        if (mainFace == null) {
            frameResult.release();
            return RecognitionResult.noFace();
        }

        Mat faceMat = extractFace(frameResult.frame, mainFace);
        RecognitionResult result = recognizeFace(faceMat);
        faceMat.release();
        frameResult.release();

        return result;
    }

    private RecognitionResult recognizeFace(Mat faceMat) {
        List<UserFaceData> enabledFaces = faceDataRepository.findAllEnabled();
        if (enabledFaces.isEmpty()) {
            return RecognitionResult.noModel();
        }

        int bestLabel = -1;
        double bestConfidence = Double.MAX_VALUE;

        for (UserFaceData faceData : enabledFaces) {
            Path modelPath = Path.of(faceData.getModelPath());
            if (!Files.exists(modelPath)) continue;

            try {
                LBPHFaceRecognizer recognizer = LBPHFaceRecognizer.create();
                // recognizer.read() 不支持中文路径，先复制到临时目录再读取
                Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "qiyunge_face");
                Files.createDirectories(tempDir);
                Path tempModel = tempDir.resolve("read_user_" + faceData.getUserId() + "_model.yml");
                Files.copy(modelPath, tempModel, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                recognizer.read(tempModel.toString());
                Files.deleteIfExists(tempModel);

                IntPointer label = new IntPointer(1);
                DoublePointer confidence = new DoublePointer(1);
                recognizer.predict(faceMat, label, confidence);

                double conf = confidence.get(0);
                if (conf < bestConfidence && conf < RECOGNITION_THRESHOLD) {
                    bestConfidence = conf;
                    bestLabel = label.get(0);
                }

                recognizer.close();
            } catch (Exception e) {
                System.err.println("[FaceRecognition] Model error for user " + faceData.getUserId() + ": " + e.getMessage());
            }
        }

        if (bestLabel != -1) {
            return RecognitionResult.success(bestLabel, bestConfidence);
        }
        return RecognitionResult.unknown();
    }

    // ==================== 实时识别流 ====================

    public void startRecognitionLoop(Consumer<RecognitionResult> onResult,
                                      Consumer<FrameResult> onFrame) {
        if (!isCameraOpen()) {
            if (onResult != null) onResult.accept(RecognitionResult.noCamera());
            return;
        }

        isProcessing.set(true);
        executor.execute(() -> {
            RecognitionResult.Status lastStatus = null;
            boolean hadFace = false;

            while (isProcessing.get() && isCameraOpen()) {
                try {
                    FrameResult frame = captureAndDetect();

                    if (frame != null && onFrame != null) {
                        onFrame.accept(frame);
                    }

                    boolean hasFace = frame != null && frame.faces.size() > 0;

                    if (hasFace) {
                        Rect mainFace = null;
                        double maxArea = 0;
                        for (int i = 0; i < frame.faces.size(); i++) {
                            Rect r = frame.faces.get(i);
                            double area = r.width() * r.height();
                            if (area > maxArea) {
                                maxArea = area;
                                mainFace = r;
                            }
                        }

                        if (mainFace != null) {
                            Mat faceMat = extractFace(frame.frame, mainFace);
                            RecognitionResult result = recognizeFace(faceMat);
                            faceMat.release();

                            if (onResult != null && result.status != lastStatus) {
                                lastStatus = result.status;
                                onResult.accept(result);
                            }
                        }
                    } else if (hadFace) {
                        lastStatus = RecognitionResult.Status.NO_FACE;
                        if (onResult != null) onResult.accept(RecognitionResult.noFace());
                    }
                    hadFace = hasFace;

                    if (frame != null) {
                        frame.release();
                    }

                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[FaceRecognition] Loop error: " + e.getMessage());
                }
            }
        });
    }

    public void stopRecognitionLoop() {
        isProcessing.set(false);
    }

    // ==================== 用户模型管理 ====================

    public boolean hasFaceData(int userId) {
        return faceDataRepository.existsByUserId(userId);
    }

    public Optional<UserFaceData> getFaceData(int userId) {
        return faceDataRepository.findByUserId(userId);
    }

    public Path getFaceDataBasePath() {
        return faceDataPath;
    }

    public boolean deleteFaceData(int userId) {
        Optional<UserFaceData> dataOpt = faceDataRepository.findByUserId(userId);
        if (dataOpt.isPresent()) {
            UserFaceData data = dataOpt.get();
            try {
                if (data.getModelPath() != null) {
                    Files.deleteIfExists(Path.of(data.getModelPath()));
                }
                if (data.getFaceImagePath() != null) {
                    Path dir = Path.of(data.getFaceImagePath());
                    if (Files.exists(dir)) {
                        Files.walk(dir).sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                            });
                    }
                }
            } catch (Exception e) {
                System.err.println("[FaceRecognition] Failed to delete face files: " + e.getMessage());
            }
            return faceDataRepository.deleteByUserId(userId);
        }
        return false;
    }

    public void shutdown() {
        stopRecognitionLoop();
        closeCamera();
        executor.shutdown();
        java2dConverter.close();
        matConverter.close();
        System.out.println("[FaceRecognition] Service shutdown");
    }

    // ==================== 内部类 ====================

    public static class FrameResult {
        public final Mat frame;
        public final RectVector faces;

        public FrameResult(Mat frame, RectVector faces) {
            this.frame = frame;
            this.faces = faces;
        }

        public void release() {
            if (frame != null) frame.release();
            if (faces != null) faces.close();
        }
    }

    public static class RecognitionResult {
        public enum Status { SUCCESS, NO_CAMERA, NO_FACE, NO_MODEL, UNKNOWN }

        public final Status status;
        public final int userId;
        public final double confidence;

        private RecognitionResult(Status status, int userId, double confidence) {
            this.status = status;
            this.userId = userId;
            this.confidence = confidence;
        }

        public static RecognitionResult success(int userId, double confidence) {
            return new RecognitionResult(Status.SUCCESS, userId, confidence);
        }

        public static RecognitionResult noCamera() {
            return new RecognitionResult(Status.NO_CAMERA, -1, -1);
        }

        public static RecognitionResult noFace() {
            return new RecognitionResult(Status.NO_FACE, -1, -1);
        }

        public static RecognitionResult noModel() {
            return new RecognitionResult(Status.NO_MODEL, -1, -1);
        }

        public static RecognitionResult unknown() {
            return new RecognitionResult(Status.UNKNOWN, -1, -1);
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }
    }
}
