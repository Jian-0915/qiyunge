package com.qiyunge.infrastructure.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * LRC 歌词解析器。
 * 支持标准 LRC 格式：[mm:ss.xx]歌词文本
 * 自动检测文件编码（UTF-8 / GBK）。
 */
public class LyricParser {

    private static final Pattern LRC_LINE = Pattern.compile("\\[(\\d{2}):(\\d{2})[.:](\\d{2,3})\\](.*)");

    /** 歌词行 */
    public static class LyricLine {
        private final double timeMs; // 毫秒
        private final String text;

        public LyricLine(double timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text;
        }
        public double getTimeMs() { return timeMs; }
        public String getText() { return text; }
        @Override
        public String toString() { return String.format("[%5.2f] %s", timeMs / 1000.0, text); }
    }

    /** 从文件解析歌词 */
    public static List<LyricLine> parse(Path lrcPath) throws IOException {
        byte[] bytes = Files.readAllBytes(lrcPath);
        String content = detectAndDecode(bytes);
        return parseContent(content);
    }

    /** 从字符串解析歌词 */
    public static List<LyricLine> parseContent(String content) {
        List<LyricLine> lines = new ArrayList<>();
        if (content == null || content.isEmpty()) return lines;
        for (String raw : content.split("\\r?\\n")) {
            raw = raw.trim();
            if (raw.isEmpty()) continue;
            Matcher m = LRC_LINE.matcher(raw);
            if (m.matches()) {
                int min = Integer.parseInt(m.group(1));
                int sec = Integer.parseInt(m.group(2));
                String msStr = m.group(3);
                int ms = msStr.length() == 2 ? Integer.parseInt(msStr) * 10 : Integer.parseInt(msStr);
                double timeMs = min * 60000.0 + sec * 1000.0 + ms;
                String text = m.group(4).trim();
                if (!text.isEmpty()) {
                    lines.add(new LyricLine(timeMs, text));
                }
            }
        }
        lines.sort((a, b) -> Double.compare(a.timeMs, b.timeMs));
        return lines;
    }

    /** 检测编码并解码字节数组 */
    private static String detectAndDecode(byte[] bytes) {
        // 先尝试 UTF-8
        String utf8 = new String(bytes, StandardCharsets.UTF_8);
        if (!containsReplacementChars(utf8)) return utf8;
        // 回退 GBK
        try {
            return new String(bytes, Charset.forName("GBK"));
        } catch (Exception e) {
            return utf8;
        }
    }

    private static boolean containsReplacementChars(String s) {
        return s.contains("\uFFFD");
    }

    /** 根据音频文件路径查找同名 LRC 文件 */
    public static Path findLyricFile(Path audioPath) {
        if (audioPath == null) return null;
        String name = audioPath.toString();
        // 替换扩展名为 .lrc
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            String lrcName = name.substring(0, dot) + ".lrc";
            Path lrcPath = Path.of(lrcName);
            if (Files.exists(lrcPath)) return lrcPath;
        }
        return null;
    }
}
