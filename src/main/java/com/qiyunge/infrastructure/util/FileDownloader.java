package com.qiyunge.infrastructure.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * HTTP 文件下载工具：支持同步/异步下载，带进度回调。
 * 下载到临时文件后原子重命名，失败自动清理。
 */
public final class FileDownloader {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    private FileDownloader() {
    }

    /**
     * 同步下载文件到指定路径。
     *
     * @param url        下载地址
     * @param targetPath 目标路径
     * @throws IOException 下载失败时抛出
     */
    public static void download(String url, Path targetPath) throws IOException {
        download(url, targetPath, null);
    }

    /**
     * 同步下载文件到指定路径，带进度回调。
     *
     * @param url             下载地址
     * @param targetPath      目标路径
     * @param progressCallback 进度回调（0.0 ~ 1.0），可为 null
     * @throws IOException 下载失败时抛出
     */
    public static void download(String url, Path targetPath, Consumer<Double> progressCallback) throws IOException {
        if (url == null || url.isBlank()) {
            throw new IOException("下载地址为空");
        }

        // 确保父目录存在
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        // 临时文件
        Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

            HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("HTTP 错误码: " + response.statusCode());
            }

            long contentLength = response.headers().firstValue("Content-Length").map(Long::parseLong).orElse(-1L);

            try (InputStream is = response.body()) {
                if (progressCallback != null && contentLength > 0) {
                    copyWithProgress(is, tempPath, contentLength, progressCallback);
                } else {
                    Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            // 原子重命名
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        } catch (Exception e) {
            // 清理临时文件
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
            }
            if (e instanceof IOException) {
                throw (IOException) e;
            }
            throw new IOException("下载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载字节数据（用于封面等小文件）。
     *
     * @param url 下载地址
     * @return 字节数组，失败返回 null
     */
    public static byte[] downloadBytes(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                return null;
            }
            return response.body();
        } catch (Exception e) {
            System.err.println("[FileDownloader] 下载字节失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 异步下载文件。
     *
     * @param url              下载地址
     * @param targetPath       目标路径
     * @param progressCallback 进度回调，可为 null
     * @param onComplete       完成回调（参数为是否成功）
     */
    public static void downloadAsync(String url, Path targetPath,
                                      Consumer<Double> progressCallback,
                                      Consumer<Boolean> onComplete) {
        CompletableFuture.runAsync(() -> {
            try {
                download(url, targetPath, progressCallback);
                if (onComplete != null) {
                    onComplete.accept(true);
                }
            } catch (Exception e) {
                System.err.println("[FileDownloader] 异步下载失败: " + e.getMessage());
                if (onComplete != null) {
                    onComplete.accept(false);
                }
            }
        });
    }

    /**
     * 带进度复制的输入流到文件。
     */
    private static void copyWithProgress(InputStream is, Path target, long total, Consumer<Double> callback) throws IOException {
        byte[] buffer = new byte[8192];
        long read = 0;
        int n;
        try (var out = Files.newOutputStream(target)) {
            while ((n = is.read(buffer)) != -1) {
                out.write(buffer, 0, n);
                read += n;
                if (total > 0) {
                    callback.accept((double) read / total);
                }
            }
        }
        if (callback != null) {
            callback.accept(1.0);
        }
    }
}
