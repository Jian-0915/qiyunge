package com.qiyunge.application.service;

import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.player.JLayerAudioPlayer;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 音乐播放服务：全局单例，跨页面持续播放。
 * 本地 MP3 使用 JLayer，其他格式回退到 JavaFX MediaPlayer。
 */
public class MusicPlayerService {

    /** 播放模式枚举 */
    public enum PlayMode { SEQUENTIAL, SINGLE_LOOP, SHUFFLE }

    private final com.qiyunge.app.AppContext appContext;

    private final ObservableList<Song> queue = FXCollections.observableArrayList();
    private final ObjectProperty<Song> currentSong = new SimpleObjectProperty<>();
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final DoubleProperty volume = new SimpleDoubleProperty(0.7);
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty currentTimeText = new SimpleStringProperty("0:00");
    private final StringProperty totalTimeText = new SimpleStringProperty("0:00");
    private final StringProperty errorMessage = new SimpleStringProperty("");

    // 当前播放时间（毫秒），用于歌词同步
    private final DoubleProperty currentTimeMs = new SimpleDoubleProperty(0);

    // 播放模式
    private final ObjectProperty<PlayMode> playMode = new SimpleObjectProperty<>(PlayMode.SEQUENTIAL);
    private volatile List<Integer> shuffleOrder;
    private volatile int shuffleIndex = 0;

    // 播放器实例
    private MediaPlayer fxPlayer;
    private JLayerAudioPlayer jlayerPlayer;
    private boolean useJLayer = false;
    private volatile boolean stopping = false;
    private volatile int currentIndex = -1;
    private Timer progressTimer;
    private volatile int lastNotifiedSongId = -1; // 基于 ID 去重
    private volatile long lastNotifyTime = 0; // 上次通知时间戳

    // 回调列表（支持多个监听者，线程安全）
    private final List<Consumer<Song>> playbackReadyListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<Song>> playbackErrorListeners = new CopyOnWriteArrayList<>();

    // 临时文件跟踪
    private final java.util.List<java.io.File> tempAliasFiles = new java.util.ArrayList<>();

    public MusicPlayerService(com.qiyunge.app.AppContext appContext) {
        this.appContext = appContext;
        // 监听音量属性变化，同步到播放器实例
        volume.addListener((obs, old, val) -> {
            double vol = val.doubleValue();
            try {
                if (useJLayer && jlayerPlayer != null) jlayerPlayer.setVolume(vol);
                else if (fxPlayer != null) fxPlayer.setVolume(vol);
            } catch (Exception ignored) {}
        });
    }

    // ===== 播放核心 =====

    public synchronized void play(Song song) {
        if (song == null) return;

        // 防止同一首歌重复触发 playbackReady（JavaFX MediaPlayer.onReady 可能多次触发）

        // 如果是在线歌曲且没有直接 URL，先解析播放地址
        if (song.getSource() != null && !"local".equals(song.getSource())) {
            if (song.getUrl() == null || song.getUrl().isEmpty()) {
                // 通过 AppContext 获取 ProviderRegistry 解析
                if (appContext != null) {
                    java.util.Optional<com.qiyunge.domain.entity.PlayableSource> source =
                        appContext.getMusicProviderRegistry().resolvePlayableSource(song);
                    if (source.isPresent()) {
                        song.setUrl(source.get().getUrl());
                    }
                }
            }
        }

        if (song.getUrl() == null) return;
        stopCurrent();
        errorMessage.set("");
        currentSong.set(song);

        String url = song.getUrl();
        boolean isRemote = url.startsWith("http://") || url.startsWith("https://");

        if (isRemote) {
            // 在线音乐直接使用 JavaFX MediaPlayer
            playWithJavaFX(song, url);
        } else {
            // 本地音乐：原有逻辑
            Path localPath = toLocalPath(url);
            boolean isRealMp3 = "mp3".equals(song.getFormat());
            if (localPath != null && isRealMp3) {
                playWithJLayer(song, localPath, url);
            } else {
                playWithJavaFX(song, prepareJavaFxUrl(url, localPath));
            }
        }
    }

    /** 设置队列并播放指定索引（用于从歌曲列表/曲笺播放） */
    public void playQueue(List<Song> songs, int startIndex) {
        if (songs == null || songs.isEmpty()) return;
        if (startIndex < 0 || startIndex >= songs.size()) {
            startIndex = 0;
        }
        queue.setAll(songs);
        playAtIndexInternal(startIndex);
    }

    /** 在当前队列中跳转到指定索引播放（不重建队列，用于流音台点击） */
    public void playAtIndex(int index) {
        if (queue.isEmpty() || index < 0 || index >= queue.size()) return;
        playAtIndexInternal(index);
    }

    /** 内部：设置 currentIndex 并播放，处理 SHUFFLE 模式 */
    private void playAtIndexInternal(int index) {
        currentIndex = index;
        if (playMode.get() == PlayMode.SHUFFLE) {
            adjustShuffleForIndex(index);
        }
        play(queue.get(index));
    }

    /** 调整 shuffleOrder 使指定索引成为下一个播放位置 */
    private void adjustShuffleForIndex(int index) {
        if (shuffleOrder == null) generateShuffleOrder();
        shuffleOrder.remove(Integer.valueOf(index));
        shuffleOrder.add(0, index);
        shuffleIndex = 0;
    }

    public synchronized void togglePause() {
        try {
            if (useJLayer && jlayerPlayer != null) {
                if (playing.get()) {
                    jlayerPlayer.pause();
                    playing.set(false);
                } else {
                    jlayerPlayer.resume();
                    playing.set(true);
                }
            } else if (fxPlayer != null) {
                if (playing.get()) {
                    fxPlayer.pause();
                } else {
                    fxPlayer.play();
                }
            }
        } catch (Exception e) {
            System.err.println("Toggle pause failed: " + e.getMessage());
        }
    }

    public synchronized void playNext() {
        if (appContext != null && appContext.isShuttingDown()) return;
        if (queue.isEmpty()) return;
        switch (playMode.get()) {
            case SEQUENTIAL -> currentIndex = (currentIndex + 1) % queue.size();
            case SINGLE_LOOP -> { /* 保持 currentIndex 不变 */ }
            case SHUFFLE -> {
                shuffleIndex++;
                if (shuffleOrder == null || shuffleIndex >= shuffleOrder.size()) {
                    generateShuffleOrder();
                    shuffleIndex = 0;
                }
                currentIndex = shuffleOrder.get(shuffleIndex);
                break;
            }
        }
        play(queue.get(currentIndex));
    }

    public synchronized void playPrevious() {
        if (appContext != null && appContext.isShuttingDown()) return;
        if (queue.isEmpty()) return;
        switch (playMode.get()) {
            case SEQUENTIAL -> currentIndex = currentIndex > 0 ? currentIndex - 1 : queue.size() - 1;
            case SINGLE_LOOP -> { /* 保持 currentIndex 不变 */ }
            case SHUFFLE -> {
                shuffleIndex = shuffleIndex > 0 ? shuffleIndex - 1 : shuffleOrder.size() - 1;
                currentIndex = shuffleOrder.get(shuffleIndex);
                break;
            }
        }
        play(queue.get(currentIndex));
    }

    public synchronized void stopCurrent() {
        stopping = true;
        errorMessage.set("");
        if (progressTimer != null) { progressTimer.cancel(); progressTimer = null; }
        if (jlayerPlayer != null) { jlayerPlayer.close(); jlayerPlayer = null; }
        if (fxPlayer != null) { fxPlayer.stop(); fxPlayer.dispose(); fxPlayer = null; }
        playing.set(false);
        progress.set(0);
        currentTimeText.set("0:00");
        currentTimeMs.set(0);
    }

    public void seek(double ratio) {
        try {
            if (useJLayer && jlayerPlayer != null) {
                Song song = currentSong.get();
                long durMs = song != null && song.getDuration() > 0
                        ? (long) (song.getDuration() * 1000) : 0;
                jlayerPlayer.seek(ratio, durMs);
            } else if (fxPlayer != null && fxPlayer.getTotalDuration() != null) {
                fxPlayer.seek(javafx.util.Duration.seconds(ratio * fxPlayer.getTotalDuration().toSeconds()));
            }
        } catch (Exception e) {
            System.err.println("Seek failed: " + e.getMessage());
        }
    }

    public void setVolume(double vol) {
        volume.set(vol);
        try {
            if (useJLayer && jlayerPlayer != null) jlayerPlayer.setVolume(vol);
            else if (fxPlayer != null) fxPlayer.setVolume(vol);
        } catch (Exception ignored) {}
    }

    // ===== 播放模式 =====

    /** 切换播放模式：顺序 -> 单曲循环 -> 随机 -> 顺序 */
    public void cyclePlayMode() {
        PlayMode[] modes = PlayMode.values();
        playMode.set(modes[(playMode.get().ordinal() + 1) % modes.length]);
    }

    public void setPlayMode(PlayMode mode) { playMode.set(mode); }

    private void generateShuffleOrder() {
        shuffleOrder = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) shuffleOrder.add(i);
        Collections.shuffle(shuffleOrder, new Random());
        shuffleIndex = 0;
    }

    // ===== 回调（支持多个监听者） =====

    public void addOnPlaybackReady(Consumer<Song> listener) { playbackReadyListeners.add(listener); }
    public void addOnPlaybackError(Consumer<Song> listener) { playbackErrorListeners.add(listener); }

    private void notifyPlaybackReady(Song song) {
        if (song == null) return;
        // 防抖：同一首歌在 2 秒内只通知一次（防止 JLayer + JavaFX 双触发或 onReady 多次触发）
        long now = System.currentTimeMillis();
        if (song.getId() == lastNotifiedSongId && (now - lastNotifyTime) < 2000) {
            return;
        }
        lastNotifiedSongId = song.getId() > 0 ? song.getId() : -1;
        lastNotifyTime = now;
        for (Consumer<Song> l : playbackReadyListeners) l.accept(song);
    }

    private void notifyPlaybackError(Song song) {
        for (Consumer<Song> l : playbackErrorListeners) l.accept(song);
    }

    // ===== Properties =====

    public ObjectProperty<Song> currentSongProperty() { return currentSong; }
    public Song getCurrentSong() { return currentSong.get(); }
    public BooleanProperty playingProperty() { return playing; }
    public boolean isPlaying() { return playing.get(); }
    public DoubleProperty volumeProperty() { return volume; }
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty currentTimeTextProperty() { return currentTimeText; }
    public StringProperty totalTimeTextProperty() { return totalTimeText; }
    public StringProperty errorMessageProperty() { return errorMessage; }
    public DoubleProperty currentTimeMsProperty() { return currentTimeMs; }
    public ObservableList<Song> getQueue() { return queue; }
    public int getCurrentIndex() { return currentIndex; }
    public ObjectProperty<PlayMode> playModeProperty() { return playMode; }
    public PlayMode getPlayMode() { return playMode.get(); }

    // ===== JLayer 播放 =====

    private void playWithJLayer(Song song, Path localPath, String originalUrl) {
        useJLayer = true;
        stopping = false;
        try {
            String filePath = localPath.toAbsolutePath().toString();
            jlayerPlayer = new JLayerAudioPlayer(filePath);
            jlayerPlayer.setOnEndOfMedia(this::playNext);
            jlayerPlayer.setOnError(() -> {
                String msg = "播放失败: " + song.getDisplayTitle() + "（音频解码错误）";
                System.err.println("JLayer: " + msg);
                errorMessage.set(msg);
                playing.set(false);
                notifyPlaybackError(song);
            });

            double dur = song.getDuration();
            if (dur > 0) totalTimeText.set(formatTime(dur));

            jlayerPlayer.playAsync();
            playing.set(true);
            notifyPlaybackReady(song);

            // 进度轮询
            progressTimer = new Timer(true);
            progressTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    javafx.application.Platform.runLater(() -> {
                        if (stopping || jlayerPlayer == null) { cancel(); return; }
                        double dur = song.getDuration();
                        long posMs = jlayerPlayer.getCurrentPositionMs();
                        double posSec = posMs / 1000.0;
                        currentTimeText.set(formatTime(posSec));
                        currentTimeMs.set(posMs);
                        if (dur > 0) {
                            double ratio = Math.min(posSec / dur, 1.0);
                            progress.set(ratio);
                        }
                    });
                }
            }, 100, 200);

        } catch (Exception e) {
            System.err.println("JLayer failed, falling back to JavaFX: " + e.getMessage());
            useJLayer = false;
            playWithJavaFX(song, prepareJavaFxUrl(originalUrl, localPath));
        }
    }

    // ===== JavaFX 播放 =====

    private void playWithJavaFX(Song song, String url) {
        useJLayer = false;
        stopping = false;
        try {
            Media media = new Media(url);
            fxPlayer = new MediaPlayer(media);
            fxPlayer.setVolume(volume.get());

            fxPlayer.setOnReady(() -> {
                if (stopping) return;
                double dur = fxPlayer.getTotalDuration().toSeconds();
                song.setDuration(dur);
                totalTimeText.set(formatTime(dur));
                fxPlayer.play();
                playing.set(true);
                notifyPlaybackReady(song);
            });

            fxPlayer.setOnPlaying(() -> playing.set(true));
            fxPlayer.setOnPaused(() -> playing.set(false));
            fxPlayer.setOnEndOfMedia(() -> playNext());

            fxPlayer.currentTimeProperty().addListener((obs, old, val) -> {
                if (fxPlayer.getTotalDuration() != null) {
                    double total = fxPlayer.getTotalDuration().toSeconds();
                    double current = val.toSeconds();
                    progress.set(total > 0 ? current / total : 0);
                    currentTimeText.set(formatTime(current));
                    currentTimeMs.set(current * 1000);
                }
            });

            fxPlayer.setOnError(() -> {
                String errMsg = fxPlayer.getError() != null ? fxPlayer.getError().toString() : "unknown";
                String msg = "播放失败: " + song.getDisplayTitle() + "（" + errMsg + "）";
                System.err.println("MediaPlayer error: " + errMsg);
                System.err.println("  Song: " + song.getTitle());
                System.err.println("  URL: " + url);
                errorMessage.set(msg);
                playing.set(false);
                notifyPlaybackError(song);
            });

        } catch (Exception e) {
            String msg = "播放失败: " + song.getDisplayTitle() + "（" + e.getMessage() + "）";
            errorMessage.set(msg);
            playing.set(false);
            notifyPlaybackError(song);
        }
    }

    // ===== URL / 文件路径工具 =====

    private Path toLocalPath(String url) {
        if (url == null || !url.toLowerCase().startsWith("file:")) return null;
        try {
            URI uri = URI.create(url);
            String decodedPath = URLDecoder.decode(uri.getSchemeSpecificPart(), StandardCharsets.UTF_8);
            while (decodedPath.startsWith("/")) decodedPath = decodedPath.substring(1);
            Path path = Path.of(decodedPath);
            if (Files.exists(path)) return path;
            System.err.println("Local file not found: " + path.toAbsolutePath());
            return null;
        } catch (Exception e) {
            System.err.println("Failed to parse local path from URL: " + url + " - " + e.getMessage());
            return null;
        }
    }

    /** 为 JavaFX 准备 URL：伪 MP3 文件创建 .m4a 别名 */
    private String prepareJavaFxUrl(String url, Path localPath) {
        if (localPath == null) return url;
        try {
            Path playablePath = localPath;
            if (hasMp3Extension(localPath) && isMp4Container(localPath)) {
                playablePath = createM4aAlias(localPath);
            }
            return playablePath.toUri().toASCIIString();
        } catch (Exception e) {
            return url;
        }
    }

    private boolean hasMp3Extension(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mp3");
    }

    private boolean isMp4Container(Path path) {
        byte[] header = readHeader(path, 16);
        return header.length >= 8 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p';
    }

    private byte[] readHeader(Path path, int size) {
        byte[] header = new byte[size];
        try (InputStream in = Files.newInputStream(path)) {
            int read = in.read(header);
            if (read <= 0) return new byte[0];
            if (read == size) return header;
            byte[] exact = new byte[read];
            System.arraycopy(header, 0, exact, 0, read);
            return exact;
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private Path createM4aAlias(Path source) throws IOException {
        long modified = Files.getLastModifiedTime(source).toMillis();
        long size = Files.size(source);
        String aliasName = Integer.toHexString(source.toAbsolutePath().toString().hashCode())
            + "-" + size + "-" + modified + ".m4a";
        Path aliasDir = Path.of(System.getProperty("java.io.tmpdir"), "qiyunge-media-alias");
        Files.createDirectories(aliasDir);
        Path alias = aliasDir.resolve(aliasName);
        if (!Files.exists(alias) || Files.size(alias) != size) {
            Files.copy(source, alias, StandardCopyOption.REPLACE_EXISTING);
        }
        alias.toFile().deleteOnExit();
        tempAliasFiles.add(alias.toFile());
        return alias;
    }

    // ===== 工具 =====

    public void cleanupTempFiles() {
        for (java.io.File file : tempAliasFiles) {
            try {
                if (file.exists()) file.delete();
            } catch (Exception ignored) {}
        }
        tempAliasFiles.clear();
    }

    private String formatTime(double seconds) {
        if (seconds <= 0) return "0:00";
        int min = (int) (seconds / 60);
        int sec = (int) (seconds % 60);
        return min + ":" + String.format("%02d", sec);
    }
}
