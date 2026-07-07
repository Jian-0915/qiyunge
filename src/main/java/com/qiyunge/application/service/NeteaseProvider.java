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
 * 网易云音乐提供者，使用 dataiqs.com 聚合接口获取真实音乐数据。
 */
public class NeteaseProvider implements MusicProvider {

    private static final String API_URL = "https://dataiqs.com/api/netease/music/";
    private final HttpClient httpClient;

    public NeteaseProvider() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    }

    @Override
    public String getProviderId() { return "netease"; }

    @Override
    public String getProviderName() { return "网易云"; }

    @Override
    public List<Song> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Collections.emptyList();
        try {
            String encoded = URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8);
            String url = API_URL + "?type=song&msg=" + encoded;

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[Netease] HTTP " + response.statusCode());
            if (response.statusCode() != 200) {
                System.err.println("[Netease] HTTP 错误: " + response.statusCode());
                return Collections.emptyList();
            }

            return parseResponse(response.body());
        } catch (Exception e) {
            System.err.println("[Netease] 搜索失败: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<PlayableSource> resolvePlayableSource(Song song) {
        return defaultResolvePlayableSource(song, "mp3");
    }

    private List<Song> parseResponse(String json) {
        List<Song> songs = new ArrayList<>();
        try {
            // 解析 dataiqs 返回的 JSON
            // 格式: {"code":200,"text":"解析成功","data":{"id":...,"name":"...","singername":"...","song_url":"..."}}
            // 也可能是列表格式

            // 先检查 code
            if (!json.contains("\"code\":200") && !json.contains("\"code\": 200")) {
                System.err.println("[Netease] API 返回非成功状态: " + json.substring(0, Math.min(100, json.length())));
                return songs;
            }

            // 尝试解析 data 对象
            int dataStart = json.indexOf("\"data\"");
            if (dataStart < 0) {
                System.err.println("[Netease] 响应中无 data 字段");
                return songs;
            }

            // 找到 data 后的对象或数组
            int objStart = json.indexOf('{', dataStart);
            if (objStart < 0) return songs;

            // 找到匹配的 }
            int depth = 0;
            int objEnd = -1;
            for (int i = objStart; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') depth++;
                else if (c == '}') {
                    depth--;
                    if (depth == 0) { objEnd = i; break; }
                }
            }
            if (objEnd < 0) return songs;

            String dataObj = json.substring(objStart, objEnd + 1);
            Song song = parseSong(dataObj);
            if (song != null) songs.add(song);

            // 如果 data 是数组，解析多个
            if (json.indexOf('[', dataStart) > 0 && json.indexOf('[', dataStart) < objStart + 10) {
                // data 是数组，重新解析
                int arrStart = json.indexOf('[', dataStart);
                int arrEnd = findMatchingBracket(json, arrStart, '[', ']');
                if (arrEnd > arrStart) {
                    songs.clear();
                    String array = json.substring(arrStart + 1, arrEnd);
                    // 按对象分割
                    int d = 0, start = -1;
                    for (int i = 0; i < array.length(); i++) {
                        char c = array.charAt(i);
                        if (c == '{') { if (d == 0) start = i; d++; }
                        else if (c == '}') {
                            d--;
                            if (d == 0 && start >= 0) {
                                Song s = parseSong(array.substring(start, i + 1));
                                if (s != null) songs.add(s);
                                start = -1;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Netease] 解析失败: " + e.getMessage());
        }
        return songs;
    }

    private Song parseSong(String json) {
        try {
            String name = extractString(json, "\"name\"");
            if (name.isEmpty()) name = extractString(json, "\"title\"");
            String singer = extractString(json, "\"singername\"");
            if (singer.isEmpty()) singer = extractString(json, "\"artist\"");
            String songUrl = extractString(json, "\"song_url\"");
            if (songUrl.isEmpty()) songUrl = extractString(json, "\"url\"");
            String cover = extractString(json, "\"cover\"");
            if (cover.isEmpty()) cover = extractString(json, "\"pic\"");
            int id = extractInt(json, "\"id\"");

            if (name.isEmpty() || songUrl.isEmpty()) return null;

            Song song = new Song();
            song.setId(id);
            song.setTitle(name);
            song.setArtist(singer.isEmpty() ? "未知歌手" : singer);
            song.setUrl(songUrl);
            song.setCoverUrl(cover);
            song.setFormat("mp3");
            song.setSource("netease");
            song.setSourceId(String.valueOf(id));
            song.setDuration(0); // 暂时未知
            return song;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractString(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return "";
        int quoteStart = json.indexOf('"', colon);
        if (quoteStart < 0) return "";
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd < 0) return "";
        return json.substring(quoteStart + 1, quoteEnd).replace("\\/", "/").replace("\\\"", "\"").replace("\\\\", "\\");
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

    private int findMatchingBracket(String s, int openPos, char open, char close) {
        int depth = 0;
        for (int i = openPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
