package com.qiyunge.application.service;

import com.qiyunge.infrastructure.storage.AppStorage;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 网易云 API 本地进程管理器。
 * 负责自动检测端口、启动 Node 服务、健康检查、关闭释放。
 */
public class NeteaseApiProcessManager {

    private static final int[] PORTS = {3000, 3001, 3002};
    private static final int STARTUP_TIMEOUT_SECONDS = 15;
    private static final String HEALTH_CHECK_PATH = "/search?keywords=test&type=1&limit=1";

    private final Path toolsDir;
    private final Path apiDir;
    private final Path runtimeDir;
    private final Path pidFile;
    private final Path logsDir;
    private Process process;
    private ProcessHandle processHandle;
    private int usedPort = -1;
    private boolean ownProcess = false;
    private volatile boolean running = false;

    public NeteaseApiProcessManager(AppStorage appStorage) {
        this.toolsDir = appStorage.getToolsDir();
        this.apiDir = toolsDir.resolve("NeteaseCloudMusicApi").toAbsolutePath().normalize();
        this.runtimeDir = appStorage.getAppDataPath().resolve("runtime");
        this.pidFile = runtimeDir.resolve("netease-api.pid");
        this.logsDir = appStorage.getLogsPath();
    }

    /**
     * 启动本地网易云 API 服务。
     * @return 实际 baseUrl（如 http://127.0.0.1:3000），失败返回 empty
     */
    public Optional<String> start() {
        System.out.println("[NeteaseApiManager] 启动本地网易云 API 服务...");

        for (int port : PORTS) {
            // 1. 检测端口是否已有健康服务
            String baseUrl = "http://127.0.0.1:" + port;
            if (isHealthy(baseUrl)) {
                System.out.println("[NeteaseApiManager] 端口 " + port + " 已有健康服务，直接复用");
                this.usedPort = port;
                this.running = true;
                adoptRecordedProcess();
                return Optional.of(baseUrl);
            }

            // 2. 端口空闲，尝试启动
            if (isPortAvailable(port)) {
                System.out.println("[NeteaseApiManager] 尝试在端口 " + port + " 启动服务...");
                if (startNodeProcess(port)) {
                    this.usedPort = port;
                    this.ownProcess = true;
                    this.running = true;
                    return Optional.of(baseUrl);
                }
            } else {
                System.out.println("[NeteaseApiManager] 端口 " + port + " 被占用且不是有效服务，跳过");
            }
        }

        System.err.println("[NeteaseApiManager] 所有端口均不可用，网易云服务启动失败");
        return Optional.empty();
    }

    /** 停止服务：只关闭自己启动的进程 */
    public void stop() {
        if (!ownProcess || process == null) {
            if (ownProcess && processHandle != null) {
                destroyProcessTree(processHandle);
                cleanupPidFile();
                System.out.println("[NeteaseApiManager] 已关闭记录的本地网易云 API 进程");
            } else {
                System.out.println("[NeteaseApiManager] 服务为外部进程，不关闭");
            }
            resetState();
            return;
        }
        System.out.println("[NeteaseApiManager] 关闭本地网易云 API 服务 (PID: " + process.pid() + ")...");
        destroyProcessTree(process.toHandle());
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                System.out.println("[NeteaseApiManager] 进程未正常退出，强制终止");
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
        }
        cleanupPidFile();
        resetState();
        System.out.println("[NeteaseApiManager] 服务已关闭");
    }

    /** 重启服务 */
    public Optional<String> restart() {
        stop();
        return start();
    }

    private void resetState() {
        process = null;
        processHandle = null;
        usedPort = -1;
        ownProcess = false;
        running = false;
    }

    /**
     * 异步启动服务，不阻塞调用线程。
     * 启动完成后通过回调通知结果。
     */
    public void startAsync(java.util.function.Consumer<Optional<String>> onReady) {
        Thread t = new Thread(() -> {
            Optional<String> result = start();
            if (onReady != null) {
                onReady.accept(result);
            }
        }, "NeteaseApi-Startup");
        t.setDaemon(true);
        t.start();
    }

    /** 获取当前使用的 baseUrl */
    public Optional<String> getBaseUrl() {
        if (usedPort > 0) {
            return Optional.of("http://127.0.0.1:" + usedPort);
        }
        return Optional.empty();
    }

    /** 检查服务是否仍在健康运行 */
    public boolean isRunning() {
        Optional<String> baseUrl = getBaseUrl();
        return baseUrl.isPresent() && isHealthy(baseUrl.get());
    }

    // ===== 私有方法 =====

    private boolean isPortAvailable(int port) {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean isHealthy(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl + HEALTH_CHECK_PATH);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(3000);
            int code = conn.getResponseCode();
            if (code != 200) {
                try { conn.getErrorStream(); } catch (Exception ignored) {}
                return false;
            }
            // 读取响应体，检查是否包含 result
            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString().contains("result");
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean startNodeProcess(int port) {
        Path startScript = apiDir.resolve("start.js");
        if (!Files.exists(startScript)) {
            System.err.println("[NeteaseApiManager] 启动脚本不存在: " + startScript);
            return false;
        }

        ProcessBuilder pb = new ProcessBuilder("node", startScript.toString());
        pb.directory(apiDir.toFile());
        pb.environment().put("HOST", "127.0.0.1");
        pb.environment().put("PORT", String.valueOf(port));
        pb.redirectErrorStream(true);

        // 日志输出到文件
        try { Files.createDirectories(logsDir); } catch (IOException ignored) {}
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logsDir.resolve("netease-api.log").toFile()));

        try {
            process = pb.start();
            processHandle = process.toHandle();
            writePidFile(process.pid());
            System.out.println("[NeteaseApiManager] Node 进程已启动 (PID: " + process.pid() + "), 等待健康检查...");

            // 等待健康检查通过
            String baseUrl = "http://127.0.0.1:" + port;
            long deadline = System.currentTimeMillis() + STARTUP_TIMEOUT_SECONDS * 1000;
            while (System.currentTimeMillis() < deadline) {
                if (isHealthy(baseUrl)) {
                    System.out.println("[NeteaseApiManager] 健康检查通过: " + baseUrl);
                    return true;
                }
                Thread.sleep(500);
            }
            System.err.println("[NeteaseApiManager] 健康检查超时");
            destroyProcessTree(process.toHandle());
            cleanupPidFile();
            return false;
        } catch (Exception e) {
            System.err.println("[NeteaseApiManager] 启动失败: " + e.getMessage());
            return false;
        }
    }

    private void adoptRecordedProcess() {
        Optional<ProcessHandle> recorded = readRecordedProcess();
        if (recorded.isPresent()) {
            this.processHandle = recorded.get();
            this.ownProcess = true;
            System.out.println("[NeteaseApiManager] 认领上次启动的残留进程 PID: " + processHandle.pid());
        } else {
            this.ownProcess = false;
        }
    }

    private Optional<ProcessHandle> readRecordedProcess() {
        try {
            if (!Files.exists(pidFile)) return Optional.empty();
            String text = Files.readString(pidFile).trim();
            if (text.isEmpty()) return Optional.empty();
            long pid = Long.parseLong(text);
            Optional<ProcessHandle> handle = ProcessHandle.of(pid);
            if (handle.isPresent() && handle.get().isAlive() && isNeteaseApiProcess(handle.get())) {
                return handle;
            }
            cleanupPidFile();
            return Optional.empty();
        } catch (Exception e) {
            cleanupPidFile();
            return Optional.empty();
        }
    }

    private boolean isNeteaseApiProcess(ProcessHandle handle) {
        try {
            String commandLine = handle.info().commandLine().orElse("").toLowerCase();
            String toolsPath = apiDir.toString().toLowerCase();
            return commandLine.contains("node") && commandLine.contains("start.js")
                && commandLine.contains(toolsPath);
        } catch (Exception e) {
            return false;
        }
    }

    private void writePidFile(long pid) {
        try {
            Files.createDirectories(runtimeDir);
            Files.writeString(pidFile, String.valueOf(pid), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("[NeteaseApiManager] 写入 PID 文件失败: " + e.getMessage());
        }
    }

    private void cleanupPidFile() {
        try {
            Files.deleteIfExists(pidFile);
        } catch (IOException ignored) {}
    }

    private void destroyProcessTree(ProcessHandle root) {
        root.descendants()
            .sorted((a, b) -> Long.compare(b.pid(), a.pid()))
            .forEach(this::destroyHandle);
        destroyHandle(root);
    }

    private void destroyHandle(ProcessHandle handle) {
        if (!handle.isAlive()) return;
        handle.destroy();
        try {
            handle.onExit().get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            if (handle.isAlive()) {
                handle.destroyForcibly();
            }
        }
    }
}
