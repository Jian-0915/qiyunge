package com.qiyunge.app;

import com.qiyunge.ui.shell.MainShell;
import com.qiyunge.ui.login.ChangePasswordView;
import com.qiyunge.ui.login.LoginView;
import com.qiyunge.ui.splash.SplashView;
import javafx.animation.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AppLauncher extends Application {

    private Stage primaryStage;
    private AppContext appContext;

    @Override
    public void init() {
        // 尽早重定向日志到文件，确保所有输出可追踪
        redirectOutputToFile();
    }

    private void redirectOutputToFile() {
        try {
            // 基于代码源位置定位项目根目录（与 AppStorage 一致）
            Path codeSource = Paths.get(
                AppLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toAbsolutePath();
            Path projectRoot = codeSource.toFile().isFile() ? codeSource.getParent() : codeSource;

            // IDE 运行时向上两级
            String pathStr = codeSource.toString().replace('\\', '/');
            if (pathStr.endsWith("/target/classes") || pathStr.endsWith("/build/classes")) {
                projectRoot = codeSource.getParent().getParent();
            }

            Path logDir = projectRoot.resolve(".qiyunge").resolve("logs");
            Files.createDirectories(logDir);
            Path logFile = logDir.resolve("boot.log");

            // 追加模式，保留历史启动日志
            FileOutputStream fos = new FileOutputStream(logFile.toFile(), true);
            PrintStream logStream = new PrintStream(fos, true);

            // 时间戳分隔线
            logStream.println("\n========== 栖云阁启动 " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                " ==========");

            // 同时输出到控制台和文件
            PrintStream dualOut = new TeePrintStream(System.out, logStream);
            PrintStream dualErr = new TeePrintStream(System.err, logStream);

            System.setOut(dualOut);
            System.setErr(dualErr);
        } catch (Exception e) {
            // 日志重定向失败不影响启动
            System.err.println("[BOOT] 日志重定向失败: " + e.getMessage());
        }
    }

    /** 双路输出流：同时写控制台和文件 */
    private static class TeePrintStream extends PrintStream {
        private final PrintStream secondary;
        TeePrintStream(PrintStream primary, PrintStream secondary) {
            super(primary, true);
            this.secondary = secondary;
        }
        @Override public void write(int b) {
            super.write(b);
            secondary.write(b);
        }
        @Override public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            secondary.write(buf, off, len);
        }
        @Override public void flush() {
            super.flush();
            secondary.flush();
        }
    }

    @Override
    public void start(Stage stage) {
        long appStart = System.currentTimeMillis();
        System.out.println("[BOOT] ===== 栖云阁启动 =====");

        this.primaryStage = stage;
        primaryStage.setTitle("栖云阁");
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setWidth(520);
        primaryStage.setHeight(340);
        primaryStage.setResizable(false);

        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/images/icon.png")));
        } catch (Exception ignored) {}

        System.out.println("[BOOT] Stage 配置完成: " + (System.currentTimeMillis() - appStart) + "ms");

        // 先显示加载页面
        SplashView splashView = new SplashView();
        Scene splashScene = new Scene(splashView, 520, 340);
        splashScene.setFill(null);
        splashScene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        primaryStage.setScene(splashScene);
        primaryStage.centerOnScreen();
        primaryStage.show();

        System.out.println("[BOOT] Splash 显示完成: " + (System.currentTimeMillis() - appStart) + "ms");

        // 异步执行初始化
        Thread initThread = new Thread(() -> {
            long initStart = System.currentTimeMillis();
            System.out.println("[BOOT] 初始化线程开始: " + (System.currentTimeMillis() - appStart) + "ms");

            appContext = new AppContext();
            appContext.initialize((progress, status, detail) -> {
                long elapsed = System.currentTimeMillis() - initStart;
                System.out.println("[BOOT] [" + elapsed + "ms] " + (int)(progress * 100) + "% - " + status + " (" + detail + ")");
                Platform.runLater(() -> splashView.updateProgress(progress, status, detail));
            });

            long initTotal = System.currentTimeMillis() - initStart;
            System.out.println("[BOOT] 初始化线程完成: " + initTotal + "ms");

            // 初始化完成后切换到登录页面
            Platform.runLater(() -> {
                long totalElapsed = System.currentTimeMillis() - appStart;
                System.out.println("[BOOT] 切换到登录页面: " + totalElapsed + "ms");
                appContext.setPrimaryStage(primaryStage);
                showLoginView();
            });
        }, "App-Init");
        initThread.setDaemon(true);
        initThread.start();
    }

    public void showLoginView() {
        appContext.getUserSession().logout();
        LoginView loginView = new LoginView(appContext);
        loginView.setOnLoginSuccess(() -> {
            loginView.playGateOpenAnimation(() -> showMainShell());
        });
        loginView.setOnNavigateToChangePassword(() -> showChangePasswordView());
        Scene scene = new Scene(loginView, 1000, 640);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
        appContext.getThemeService().applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.setWidth(1000);
        primaryStage.setHeight(640);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(520);
        primaryStage.setMaximized(false);
        primaryStage.centerOnScreen();
    }

    public void showChangePasswordView() {
        ChangePasswordView changeView = new ChangePasswordView(appContext);
        changeView.setOnPasswordChanged(() -> {
            appContext.getDialogService().showInfo("密码已修改", "密码修改成功，请重新登录。");
            showLoginView();
        });
        changeView.setOnCancel(() -> showMainShell());
        Scene scene = new Scene(changeView, 1000, 640);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
        appContext.getThemeService().applyTheme(scene);
        primaryStage.setScene(scene);
    }

    public void showMainShell() {
        // 网易云 API 在 AppContext.initialize() 中已启动，仅在未运行时异步重启，避免阻塞 UI 线程
        if (appContext.getNeteaseApiProcessManager() != null) {
            appContext.getAsyncExecutor().execute(() -> {
                if (!appContext.getNeteaseApiProcessManager().isRunning()) {
                    appContext.getNeteaseApiProcessManager().restart();
                }
            });
        }

        MainShell mainShell = new MainShell(appContext);
        mainShell.setOpacity(0);
        mainShell.setScaleX(0.985);
        mainShell.setScaleY(0.985);
        mainShell.setOnLogout(() -> {
            var userOpt = appContext.getUserService().findById(appContext.getUserSession().getUserId());
            userOpt.ifPresent(appContext.getAuditLogService()::logLogout);

            // 停止音乐播放
            appContext.getMusicPlayerService().stopCurrent();

            // 停止网易云 API 进程
            if (appContext.getNeteaseApiProcessManager() != null) {
                appContext.getNeteaseApiProcessManager().stop();
            }

            // 关闭人脸识别摄像头（登录页切换时复用，退出登录时释放）
            if (appContext.getFaceRecognitionService() != null) {
                appContext.getFaceRecognitionService().stopRecognitionLoop();
                appContext.getFaceRecognitionService().closeCamera();
            }

            showLoginView();
        });
        Scene scene = new Scene(mainShell.getRoot(), 1200, 750);
        scene.getStylesheets().add(getClass().getResource("/styles/theme.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/styles/components.css").toExternalForm());
        appContext.getThemeService().applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);

        // Fade in + subtle scale
        ParallelTransition enter = new ParallelTransition();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(260), mainShell);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(260), mainShell);
        scaleIn.setFromX(0.985);
        scaleIn.setFromY(0.985);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);

        enter.getChildren().addAll(fadeIn, scaleIn);
        enter.play();
    }

    @Override
    public void stop() {
        if (appContext != null) {
            appContext.shutdown();
        }
        Platform.exit();
    }
}
