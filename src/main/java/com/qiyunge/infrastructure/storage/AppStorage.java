package com.qiyunge.infrastructure.storage;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AppStorage {

    private final Path projectRoot;
    private final Path appDataPath;
    private final Path databasePath;
    private final Path cachePath;
    private final Path musicCachePath;
    private final Path musicAudioPath;
    private final Path musicCoverPath;
    private final Path musicLyricPath;
    private final Path galleryCachePath;
    private final Path faceDataPath;
    private final Path logsPath;
    private final Path backupsPath;
    private final Path configPath;
    private final Path toolsDir;

    public AppStorage() {
        // 基于代码源位置定位项目根目录，不再依赖 user.dir
        projectRoot = findProjectRoot();
        appDataPath = projectRoot.resolve(".qiyunge");
        databasePath = appDataPath.resolve("qiyunge.db");
        cachePath = appDataPath.resolve("cache");
        musicCachePath = cachePath.resolve("music");
        musicAudioPath = musicCachePath.resolve("audio");
        musicCoverPath = musicCachePath.resolve("covers");
        musicLyricPath = musicCachePath.resolve("lyrics");
        galleryCachePath = cachePath.resolve("gallery");
        faceDataPath = appDataPath.resolve("face_data");
        logsPath = appDataPath.resolve("logs");
        backupsPath = appDataPath.resolve("backups");
        configPath = appDataPath.resolve("config.json");
        toolsDir = projectRoot.resolve("tools");
    }

    /**
     * 通过代码源位置推导项目根目录。
     * IDE 运行时 classes 在 target/classes 或 build/classes 下，向上两级即为项目根；
     * 打包后 jar 直接在项目根目录或其子目录下。
     */
    private static Path findProjectRoot() {
        try {
            Path codeSource = Paths.get(
                AppStorage.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            ).toAbsolutePath();

            // IDE 运行：classes 在 target/classes 下，向上两级即为项目根
            String pathStr = codeSource.toString().replace('\\', '/');
            if (pathStr.endsWith("/target/classes") || pathStr.endsWith("/build/classes") ||
                pathStr.endsWith("/target/classes/") || pathStr.endsWith("/build/classes/")) {
                return codeSource.getParent().getParent();
            }

            // 打包运行：直接取 EXE/JAR 所在目录作为项目根
            Path jarDir = codeSource.toFile().isFile() ? codeSource.getParent() : codeSource;
            System.out.println("[AppStorage] 项目根目录: " + jarDir);
            return jarDir;
        } catch (Exception e) {
            Path fallback = Paths.get(System.getProperty("user.dir"));
            System.out.println("[AppStorage] 路径检测失败，回退到 user.dir: " + fallback);
            return fallback;
        }
    }

    public void ensureDirectories() {
        createDirIfNotExists(appDataPath);
        createDirIfNotExists(cachePath);
        createDirIfNotExists(musicCachePath);
        createDirIfNotExists(musicAudioPath);
        createDirIfNotExists(musicCoverPath);
        createDirIfNotExists(musicLyricPath);
        createDirIfNotExists(galleryCachePath);
        createDirIfNotExists(faceDataPath);
        createDirIfNotExists(logsPath);
        createDirIfNotExists(backupsPath);
    }

    private void createDirIfNotExists(Path path) {
        File dir = path.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public Path getProjectRoot() { return projectRoot; }
    public Path getToolsDir() { return toolsDir; }
    public Path getAppDataPath() { return appDataPath; }
    public Path getDatabasePath() { return databasePath; }
    public Path getCachePath() { return cachePath; }
    public Path getMusicCachePath() { return musicCachePath; }
    public Path getMusicAudioPath() { return musicAudioPath; }
    public Path getMusicCoverPath() { return musicCoverPath; }
    public Path getMusicLyricPath() { return musicLyricPath; }
    public Path getGalleryCachePath() { return galleryCachePath; }
    public Path getFaceDataPath() { return faceDataPath; }
    public Path getLogsPath() { return logsPath; }
    public Path getBackupsPath() { return backupsPath; }
    public Path getConfigPath() { return configPath; }
}
