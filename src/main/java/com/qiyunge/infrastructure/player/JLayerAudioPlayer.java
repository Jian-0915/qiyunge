package com.qiyunge.infrastructure.player;

import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.player.Player;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * JLayer MP3 播放器包装器：支持播放/暂停/停止/音量。
 * 基于 javazoom.jl.player.Player（阻塞式 API），在独立线程中播放。
 * 用于本地 MP3 文件播放（JavaFX GStreamer 不支持 MP3）。
 */
public class JLayerAudioPlayer {

    private final Object lock = new Object();
    private Player player;
    private FileInputStream audioStream;
    private Thread playThread;
    private volatile boolean paused = false;
    private volatile boolean stopped = false;
    private volatile long pausedPosition = 0;
    private volatile long startPosition = 0;
    private volatile long playStartTime = 0; // System.currentTimeMillis when play/resume started
    private volatile long estimatedDurationMs = 0; // 文件预估时长（ms），用于 seek 时计算字节偏移

    private Consumer<Double> onProgress;
    private Runnable onEndOfMedia;
    private Runnable onError;

    private final String filePath;

    public JLayerAudioPlayer(String filePath) {
        this.filePath = filePath;
    }

    /**
     * 在新线程中开始播放。play() 是阻塞的，所以必须在独立线程调用。
     */
    public void playAsync() {
        stopped = false;
        paused = false;
        startPosition = pausedPosition;
        playStartTime = System.currentTimeMillis();

        playThread = new Thread(() -> {
            try {
                synchronized (lock) {
                    audioStream = new FileInputStream(filePath);
                    // 如果之前有 seek 操作，跳过对应字节数，使 Player 从目标位置播放
                    if (startPosition > 0 && estimatedDurationMs > 0) {
                        long totalBytes = new java.io.File(filePath).length();
                        long bytesToSkip = (long) ((double) totalBytes * startPosition / estimatedDurationMs);
                        if (bytesToSkip > 0) {
                            try {
                                long skipped = audioStream.skip(bytesToSkip);
                                if (skipped < bytesToSkip) {
                                    System.err.println("[JLayer] Seek offset incomplete: " + skipped + "/" + bytesToSkip);
                                }
                            } catch (java.io.IOException e) {
                                System.err.println("[JLayer] Seek skip error: " + e.getMessage());
                            }
                        }
                    }
                    player = new Player(audioStream);
                }
                player.play();
                // play() 返回说明播放结束（自然结束或被 stop）
                if (!stopped && onEndOfMedia != null) {
                    javafx.application.Platform.runLater(onEndOfMedia);
                }
            } catch (FileNotFoundException e) {
                System.err.println("JLayer: file not found: " + filePath);
                System.err.println("  Absolute path: " + new java.io.File(filePath).getAbsolutePath());
                if (onError != null) javafx.application.Platform.runLater(onError);
            } catch (JavaLayerException e) {
                if (!stopped) {
                    System.err.println("JLayer decode error: " + e.getMessage());
                    System.err.println("  File: " + filePath);
                    // Print file header for diagnosis
                    try {
                        java.io.FileInputStream fis = new java.io.FileInputStream(filePath);
                        byte[] hdr = new byte[4];
                        int n = fis.read(hdr);
                        fis.close();
                        if (n > 0) {
                            System.err.println("  File header: 0x"
                                    + java.util.HexFormat.of().formatHex(hdr, 0, n));
                        }
                    } catch (Exception ignored) {}
                    if (onError != null) javafx.application.Platform.runLater(onError);
                }
            } finally {
                closeStream();
            }
        }, "JLayer-Playback");
        playThread.setDaemon(true);
        playThread.start();
    }

    /**
     * 暂停播放。通过关闭流来中断阻塞的 play()，记录已播放位置。
     * 注意：JLayer Player 不支持真正的暂停，这里通过记录位置 + 重新打开文件模拟。
     */
    public void pause() {
        synchronized (lock) {
            if (player == null || stopped) return;
            paused = true;
            // 计算已播放时长
            pausedPosition = startPosition + (System.currentTimeMillis() - playStartTime);
            // 关闭流以中断 play() 阻塞
            stopped = true; // 临时标记，防止触发 onEndOfMedia
            closeStreamInternal();
            if (playThread != null) {
                try { playThread.join(500); } catch (InterruptedException ignored) {}
            }
            stopped = false; // 恢复标记
        }
    }

    /**
     * 从暂停位置恢复播放。
     */
    public void resume() {
        if (!paused) return;
        paused = false;
        playAsync();
    }

    /**
     * 停止播放并重置位置。
     */
    public void stop() {
        synchronized (lock) {
            stopped = true;
            paused = false;
            pausedPosition = 0;
            closeStreamInternal();
        }
        if (playThread != null) {
            try { playThread.join(500); } catch (InterruptedException ignored) {}
        }
    }

    /**
     * 跳转到指定比例位置（0.0 ~ 1.0）。
     * 需要重新打开文件从头播放到目标位置。
     * 简化实现：如果当前正在播放，先停止，再从新位置开始。
     */
    public void seek(double ratio, long estimatedDurationMs) {
        if (estimatedDurationMs <= 0) return;
        long targetPosition = (long) (ratio * estimatedDurationMs);
        boolean wasPlaying = !paused && !stopped && playThread != null && playThread.isAlive();

        this.estimatedDurationMs = estimatedDurationMs;

        synchronized (lock) {
            stopped = true;
            closeStreamInternal();
        }
        if (playThread != null) {
            try { playThread.join(500); } catch (InterruptedException ignored) {}
        }

        pausedPosition = targetPosition;
        if (wasPlaying) {
            playAsync();
        } else {
            paused = true;
            startPosition = targetPosition;
        }
    }

    /**
     * 获取当前播放位置（毫秒）。
     */
    public long getCurrentPositionMs() {
        if (stopped) return pausedPosition;
        if (paused) return pausedPosition;
        return startPosition + (System.currentTimeMillis() - playStartTime);
    }

    /**
     * 通过 Java Sound API 的 FloatControl.MASTER_GAIN 调节系统音量。
     * 优先通过反射获取 JLayer Player 内部的 SourceDataLine，失败后兜底遍历所有 Mixer。
     */
    public void setVolume(double volume) {
        try {
            float vol = (float) Math.max(0.0, Math.min(1.0, volume));
            float dB = vol <= 0.0001f ? -80.0f : (float) (Math.log10(vol) * 20.0);

            boolean applied = false;

            // 方法1：反射获取 Player 内部 AudioDevice 的 source 字段（SourceDataLine）
            if (player != null) {
                try {
                    java.lang.reflect.Field audioField = javazoom.jl.player.Player.class.getDeclaredField("audio");
                    audioField.setAccessible(true);
                    Object audioDevice = audioField.get(player);
                    if (audioDevice != null) {
                        java.lang.reflect.Field sourceField = audioDevice.getClass().getDeclaredField("source");
                        sourceField.setAccessible(true);
                        javax.sound.sampled.SourceDataLine source = (javax.sound.sampled.SourceDataLine) sourceField.get(audioDevice);
                        if (source != null && source.isOpen()
                            && source.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                            javax.sound.sampled.FloatControl gain = (javax.sound.sampled.FloatControl)
                                source.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                            applied = true;
                        }
                    }
                } catch (NoSuchFieldException ignored) {
                    // 不是 JavaSoundAudioDevice，继续兜底
                }
            }

            if (applied) return;

            // 方法2：遍历所有 Mixer 的已打开 SourceDataLine
            for (javax.sound.sampled.Mixer.Info mixerInfo : javax.sound.sampled.AudioSystem.getMixerInfo()) {
                javax.sound.sampled.Mixer mixer = javax.sound.sampled.AudioSystem.getMixer(mixerInfo);
                for (javax.sound.sampled.Line line : mixer.getSourceLines()) {
                    if (line instanceof javax.sound.sampled.SourceDataLine && line.isOpen()) {
                        if (line.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) {
                            javax.sound.sampled.FloatControl gain = (javax.sound.sampled.FloatControl)
                                line.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[JLayer] Volume control failed: " + e.getMessage());
        }
    }

    public void setOnProgress(Consumer<Double> callback) {
        this.onProgress = callback;
    }

    public void setOnEndOfMedia(Runnable callback) {
        this.onEndOfMedia = callback;
    }

    public void setOnError(Runnable callback) {
        this.onError = callback;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void close() {
        stop();
    }

    private void closeStream() {
        synchronized (lock) {
            closeStreamInternal();
        }
    }

    private void closeStreamInternal() {
        if (player != null) {
            player.close();
            player = null;
        }
        if (audioStream != null) {
            try {
                audioStream.close();
            } catch (IOException ignored) {
            }
            audioStream = null;
        }
    }
}
