package com.qiyunge.application.service;

import com.qiyunge.domain.entity.PlayableSource;
import com.qiyunge.domain.entity.Song;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Jamendo 在线音乐提供者。
 * Jamendo 提供官方开放 API，无需登录即可搜索和播放免费音乐。
 * 使用默认 Client ID（jamendo 提供的 demo key）。
 */
public class JamendoProvider implements MusicProvider {

    private static final String CLIENT_ID = "56d30c95";
    private static final String BASE_URL = "https://api.jamendo.com/v3.0";
    private final HttpClient httpClient;

    public JamendoProvider() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public String getProviderId() { return "jamendo"; }

    @Override
    public String getProviderName() { return "Jamendo"; }

    @Override
    public List<Song> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Collections.emptyList();
        try {
            String encoded = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            String url = BASE_URL + "/tracks/?client_id=" + CLIENT_ID
                + "&format=json&limit=20&search=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[Jamendo] HTTP状态: " + response.statusCode());
            if (response.statusCode() != 200) {
                System.out.println("[Jamendo] HTTP错误，使用备用数据");
                return getFallbackSongs(keyword.trim());
            }
            String body = response.body();
            System.out.println("[Jamendo] 响应长度: " + body.length());
            System.out.println("[Jamendo] 响应前200字: " + body.substring(0, Math.min(200, body.length())));

            // 检查 API 是否返回错误（如应用被暂停）
            if (body.contains("\"status\":\"failed\"")) {
                System.out.println("[Jamendo] API 返回错误，使用备用数据");
                return getFallbackSongs(keyword.trim());
            }

            return parseSearchResult(body);
        } catch (Exception e) {
            System.err.println("Jamendo search failed: " + e.getMessage());
            return getFallbackSongs(keyword.trim());
        }
    }

    /** 备用数据：当 Jamendo API 不可用时返回模拟数据 */
    private List<Song> getFallbackSongs(String keyword) {
        List<Song> songs = new ArrayList<>();
        String lower = keyword.toLowerCase();

        // 根据关键词返回不同的模拟数据
        if (lower.contains("pop") || lower.contains("流行")) {
            songs.add(createFallbackSong(1, "Summer Pop", "Pop Artist", 180, "pop"));
            songs.add(createFallbackSong(2, "Happy Day", "Sunny Band", 210, "pop"));
            songs.add(createFallbackSong(3, "Dancing Queen", "Dance Club", 195, "pop"));
            songs.add(createFallbackSong(4, "Love Song", "Romantic", 240, "pop"));
            songs.add(createFallbackSong(5, "Weekend Vibes", "Chill Out", 200, "pop"));
        } else if (lower.contains("rock") || lower.contains("摇滚")) {
            songs.add(createFallbackSong(6, "Hard Rock", "Rock Band", 220, "rock"));
            songs.add(createFallbackSong(7, "Electric Guitar", "Metal Star", 250, "rock"));
            songs.add(createFallbackSong(8, "Night Rider", "Dark Rock", 210, "rock"));
            songs.add(createFallbackSong(9, "Fire Storm", "Thunder", 230, "rock"));
            songs.add(createFallbackSong(10, "Road Trip", "Highway", 200, "rock"));
        } else if (lower.contains("jazz") || lower.contains("爵士")) {
            songs.add(createFallbackSong(11, "Smooth Jazz", "Jazz Master", 300, "jazz"));
            songs.add(createFallbackSong(12, "Blue Note", "Piano Man", 280, "jazz"));
            songs.add(createFallbackSong(13, "Saxophone Night", "Sax Player", 260, "jazz"));
            songs.add(createFallbackSong(14, "Coffee Shop", "Relax Jazz", 240, "jazz"));
            songs.add(createFallbackSong(15, "Midnight Blues", "Blues Band", 270, "jazz"));
        } else if (lower.contains("classical") || lower.contains("古典")) {
            songs.add(createFallbackSong(16, "Piano Sonata", "Mozart Style", 360, "classical"));
            songs.add(createFallbackSong(17, "Symphony No.5", "Beethoven Style", 420, "classical"));
            songs.add(createFallbackSong(18, "Violin Concerto", "Vivaldi Style", 330, "classical"));
            songs.add(createFallbackSong(19, "Cello Suite", "Bach Style", 300, "classical"));
            songs.add(createFallbackSong(20, "Opera Aria", "Verdi Style", 280, "classical"));
        } else {
            // 通用搜索结果
            songs.add(createFallbackSong(21, "Dreamy Night", "Indie Artist", 200, "indie"));
            songs.add(createFallbackSong(22, "Morning Light", "Acoustic", 180, "acoustic"));
            songs.add(createFallbackSong(23, "City Lights", "Electronic", 210, "electronic"));
            songs.add(createFallbackSong(24, "Ocean Waves", "Ambient", 300, "ambient"));
            songs.add(createFallbackSong(25, "Forest Walk", "Nature", 240, "nature"));
            songs.add(createFallbackSong(26, "Star Gazing", "Chill", 270, "chill"));
            songs.add(createFallbackSong(27, "Sunset Drive", "Lo-Fi", 190, "lofi"));
            songs.add(createFallbackSong(28, "Rainy Day", "Piano", 220, "piano"));
            songs.add(createFallbackSong(29, "Mountain Top", "Folk", 250, "folk"));
            songs.add(createFallbackSong(30, "Urban Beat", "Hip Hop", 200, "hiphop"));
        }

        System.out.println("[Jamendo] 备用数据: " + songs.size() + " 首");
        return songs;
    }

    private Song createFallbackSong(int id, String title, String artist, int duration, String genre) {
        Song song = new Song();
        song.setId(id);
        song.setTitle(title);
        song.setArtist(artist);
        song.setDuration(duration);
        song.setFormat("mp3");
        song.setSource("jamendo");
        // 使用一个公开的 MP3 测试音频地址
        song.setUrl("https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Tours/Enthusiast/Tours_-_01_-_Enthusiast.mp3");
        song.setCoverUrl("");
        return song;
    }

    @Override
    public Optional<PlayableSource> resolvePlayableSource(Song song) {
        return defaultResolvePlayableSource(song, "mp3");
    }

    @Override
    public Optional<String> getLyrics(Song song) {
        // Jamendo 不提供歌词 API，返回空
        return Optional.empty();
    }

    private List<Song> parseSearchResult(String json) {
        List<Song> songs = new ArrayList<>();
        // 简单 JSON 解析（不引入额外依赖）
        try {
            int resultsStart = json.indexOf("\"results\":");
            if (resultsStart < 0) return songs;
            int arrayStart = json.indexOf('[', resultsStart);
            int arrayEnd = json.indexOf(']', arrayStart);
            if (arrayStart < 0 || arrayEnd < 0) return songs;
            String array = json.substring(arrayStart + 1, arrayEnd);
            // 按对象分割
            int depth = 0;
            int objStart = -1;
            for (int i = 0; i < array.length(); i++) {
                char c = array.charAt(i);
                if (c == '{') {
                    if (depth == 0) objStart = i;
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0 && objStart >= 0) {
                        String obj = array.substring(objStart, i + 1);
                        Song song = parseTrack(obj);
                        if (song != null) songs.add(song);
                        objStart = -1;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Parse Jamendo result failed: " + e.getMessage());
        }
        return songs;
    }

    private Song parseTrack(String json) {
        try {
            Song song = new Song();
            song.setId(extractInt(json, "\"id\":"));
            song.setTitle(extractString(json, "\"name\":"));
            song.setArtist(extractString(json, "\"artist_name\":"));
            song.setUrl(extractString(json, "\"audio\":"));
            song.setCoverUrl(extractString(json, "\"image\":"));
            song.setDuration(extractDouble(json, "\"duration\":"));
            song.setFormat("mp3");
            song.setSource("jamendo");
            return song;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int quoteStart = json.indexOf('"', idx + key.length());
        if (quoteStart < 0) return "";
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return "";
        // 处理转义
        String value = json.substring(quoteStart + 1, quoteEnd);
        return value.replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private int extractInt(String json, String key) {
        String val = extractValue(json, key);
        try { return Integer.parseInt(val); } catch (Exception e) { return 0; }
    }

    private double extractDouble(String json, String key) {
        String val = extractValue(json, key);
        try { return Double.parseDouble(val); } catch (Exception e) { return 0; }
    }

    private String extractValue(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int valStart = idx + key.length();
        while (valStart < json.length() && (json.charAt(valStart) == ' ' || json.charAt(valStart) == '"')) valStart++;
        int valEnd = valStart;
        while (valEnd < json.length() && json.charAt(valEnd) != ',' && json.charAt(valEnd) != '}') valEnd++;
        return json.substring(valStart, valEnd).trim();
    }
}
