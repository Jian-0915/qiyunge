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
import java.util.*;

/**
 * 外部音乐 API 聚合适配器。
 * 通过配置 API 地址即可接入不同平台（网易云、QQ音乐、酷狗、咪咕等）。
 * 当 API 不可用时自动降级到备用数据。
 */
public class ExternalMusicApiProvider implements MusicProvider {

    private final String providerId;
    private final String providerName;
    private final String apiUrl;
    private final HttpClient httpClient;

    public ExternalMusicApiProvider(String providerId, String providerName, String apiUrl) {
        this.providerId = providerId;
        this.providerName = providerName;
        this.apiUrl = apiUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public String getProviderId() { return providerId; }

    @Override
    public String getProviderName() { return providerName; }

    @Override
    public List<Song> search(String keyword) {
        if (apiUrl == null || apiUrl.isEmpty()) {
            return getFallbackSongs(keyword);
        }
        try {
            String encoded = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            String url = apiUrl.replace("{keyword}", encoded);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.out.println("[" + providerId + "] HTTP " + response.statusCode() + ", 使用备用数据");
                return getFallbackSongs(keyword);
            }
            String body = response.body();
            if (body.contains("\"error\"") || body.contains("\"failed\"")) {
                System.out.println("[" + providerId + "] API 返回错误, 使用备用数据");
                return getFallbackSongs(keyword);
            }
            // 尝试解析 JSON（通用解析）
            return parseGenericResult(body);
        } catch (Exception e) {
            System.err.println("[" + providerId + "] 搜索失败: " + e.getMessage());
            return getFallbackSongs(keyword);
        }
    }

    @Override
    public Optional<PlayableSource> resolvePlayableSource(Song song) {
        return defaultResolvePlayableSource(song, song.getFormat());
    }

    /** 通用 JSON 解析：尝试提取包含歌曲信息的数组 */
    private List<Song> parseGenericResult(String json) {
        List<Song> songs = new ArrayList<>();
        // 尝试找到 JSON 数组
        int arrStart = json.indexOf('[');
        int arrEnd = json.lastIndexOf(']');
        if (arrStart >= 0 && arrEnd > arrStart) {
            String array = json.substring(arrStart + 1, arrEnd);
            // 按对象分割
            int depth = 0, objStart = -1;
            for (int i = 0; i < array.length(); i++) {
                char c = array.charAt(i);
                if (c == '{') { if (depth == 0) objStart = i; depth++; }
                else if (c == '}') {
                    depth--;
                    if (depth == 0 && objStart >= 0) {
                        Song song = parseGenericTrack(array.substring(objStart, i + 1));
                        if (song != null) songs.add(song);
                        objStart = -1;
                    }
                }
            }
        }
        if (songs.isEmpty()) return getFallbackSongs("");
        return songs;
    }

    private Song parseGenericTrack(String json) {
        try {
            Song song = new Song();
            song.setId(extractInt(json, "\"id\":") != 0 ? extractInt(json, "\"id\":") : extractInt(json, "\"id\" :"));
            song.setTitle(extractString(json, "\"name\":") != null && !extractString(json, "\"name\":").isEmpty()
                ? extractString(json, "\"name\":") : extractString(json, "\"title\":"));
            song.setArtist(extractString(json, "\"artist\":") != null && !extractString(json, "\"artist\":").isEmpty()
                ? extractString(json, "\"artist\":") : extractString(json, "\"artist_name\":"));
            song.setUrl(extractString(json, "\"url\":") != null && !extractString(json, "\"url\":").isEmpty()
                ? extractString(json, "\"url\":") : extractString(json, "\"audio\":"));
            song.setCoverUrl(extractString(json, "\"cover\":") != null && !extractString(json, "\"cover\":").isEmpty()
                ? extractString(json, "\"cover\":") : extractString(json, "\"image\":"));
            song.setDuration(extractDouble(json, "\"duration\":"));
            song.setFormat("mp3");
            song.setSource(providerId);
            song.setSourceId(String.valueOf(song.getId()));
            if (song.getTitle() == null || song.getTitle().isEmpty()) return null;
            return song;
        } catch (Exception e) {
            return null;
        }
    }

    /** 备用数据 */
    private List<Song> getFallbackSongs(String keyword) {
        List<Song> songs = new ArrayList<>();
        String[][] data = {
            {"Cloud Melody", "Sky Singer", "210"},
            {"Digital Dreams", "Electro Wave", "195"},
            {"Neon Lights", "City Pulse", "220"},
            {"Velvet Sunset", "Chill Horizon", "240"},
            {"Crystal Rain", "Nature Sound", "200"},
        };
        for (int i = 0; i < data.length; i++) {
            Song song = new Song();
            song.setId(1000 + i);
            song.setTitle(data[i][0]);
            song.setArtist(data[i][1]);
            song.setDuration(Double.parseDouble(data[i][2]));
            song.setFormat("mp3");
            song.setSource(providerId);
            song.setSourceId(providerId + "_" + (1000 + i));
            song.setUrl("https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Tours/Enthusiast/Tours_-_01_-_Enthusiast.mp3");
            songs.add(song);
        }
        return songs;
    }

    private String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int quoteStart = json.indexOf('"', idx + key.length());
        if (quoteStart < 0) return "";
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return "";
        return json.substring(quoteStart + 1, quoteEnd).replace("\\/", "/").replace("\\\"", "\"");
    }

    private int extractInt(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        int valStart = idx + key.length();
        while (valStart < json.length() && !Character.isDigit(json.charAt(valStart))) valStart++;
        int valEnd = valStart;
        while (valEnd < json.length() && Character.isDigit(json.charAt(valEnd))) valEnd++;
        try { return Integer.parseInt(json.substring(valStart, valEnd)); } catch (Exception e) { return 0; }
    }

    private double extractDouble(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return 0;
        int valStart = idx + key.length();
        while (valStart < json.length() && !Character.isDigit(json.charAt(valStart)) && json.charAt(valStart) != '.') valStart++;
        int valEnd = valStart;
        while (valEnd < json.length() && (Character.isDigit(json.charAt(valEnd)) || json.charAt(valEnd) == '.')) valEnd++;
        try { return Double.parseDouble(json.substring(valStart, valEnd)); } catch (Exception e) { return 0; }
    }
}
