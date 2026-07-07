package com.qiyunge.infrastructure.util;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 路径工具类：文件 URL 与本地路径互转。
 */
public final class PathUtils {

    private PathUtils() {}

    /**
     * 将 file:// URL 解析为本地 Path，文件不存在返回 null。
     */
    public static Path toLocalPath(String url) {
        if (url == null || !url.toLowerCase().startsWith("file:")) return null;
        try {
            URI uri = URI.create(url);
            String decodedPath = URLDecoder.decode(uri.getSchemeSpecificPart(), StandardCharsets.UTF_8);
            while (decodedPath.startsWith("/")) decodedPath = decodedPath.substring(1);
            Path path = Path.of(decodedPath);
            if (Files.exists(path)) return path;
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
