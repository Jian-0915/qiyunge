package com.qiyunge.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiyunge.domain.entity.Song;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 网易云 API 客户端：封装对本地 NeteaseCloudMusicApi 的 HTTP 调用。
 */
public class NeteaseApiClient {

    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NeteaseApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /** 搜索歌曲 */
    public List<Song> search(String keyword, int limit) {
        List<Song> songs = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = baseUrl + "/search?keywords=" + encoded + "&type=1&limit=" + limit;
            String json = httpGet(url);
            if (json == null || json.isEmpty()) {
                System.err.println("[NeteaseClient] HTTP 返回空");
                return songs;
            }
            System.out.println("[NeteaseClient] 响应前200字: " + json.substring(0, Math.min(200, json.length())));

            JsonNode root = objectMapper.readTree(json);
            JsonNode songNodes = root.path("result").path("songs");
            if (!songNodes.isArray()) {
                System.err.println("[NeteaseClient] 响应中无 result 字段");
                return songs;
            }

            for (JsonNode songNode : songNodes) {
                Song song = parseSong(songNode);
                if (song != null) songs.add(song);
            }
        } catch (Exception e) {
            System.err.println("[NeteaseClient] 搜索失败: " + e.getMessage());
        }
        return songs;
    }

    /** 获取歌曲播放地址 */
    public String getSongUrl(long songId) {
        try {
            String url = baseUrl + "/song/url/v1?id=" + songId + "&level=standard";
            String json = httpGet(url);
            String resolved = extractFirstPlayableUrl(json);
            if (resolved != null && !resolved.isBlank()) return resolved;

            String fallbackUrl = baseUrl + "/song/url?id=" + songId;
            String fallbackJson = httpGet(fallbackUrl);
            resolved = extractFirstPlayableUrl(fallbackJson);
            if (resolved == null || resolved.isBlank()) {
                System.err.println("[NeteaseClient] 歌曲无可用播放链接: " + songId);
            }
            return resolved;
        } catch (Exception e) {
            System.err.println("[NeteaseClient] 获取URL失败: " + e.getMessage());
            return null;
        }
    }

    /** 获取歌词 */
    public String getLyric(long songId) {
        try {
            String url = baseUrl + "/lyric?id=" + songId;
            String json = httpGet(url);
            if (json == null) return null;
            JsonNode root = objectMapper.readTree(json);
            String lyric = root.path("lrc").path("lyric").asText("");
            return lyric.isBlank() ? null : lyric;
        } catch (Exception e) {
            System.err.println("[NeteaseClient] 获取歌词失败: " + e.getMessage());
            return null;
        }
    }

    // ===== 私有方法 =====

    private String httpGet(String url) {
        try {
            URI uri = URI.create(url);
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code != 200) {
                // 消费错误流以释放连接
                try { conn.getErrorStream(); } catch (Exception ignored) {}
                return null;
            }
            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (Exception e) {
            System.err.println("[NeteaseClient] HTTP GET 失败: " + e.getMessage());
            return null;
        }
    }

    private String extractFirstPlayableUrl(String json) throws Exception {
        if (json == null || json.isBlank()) return null;
        JsonNode root = objectMapper.readTree(json);
        JsonNode data = root.path("data");
        if (!data.isArray() || data.isEmpty()) return null;
        JsonNode first = data.get(0);
        String url = first.path("url").asText(null);
        int code = first.path("code").asInt(0);
        if (url == null || url.isBlank() || code >= 400) return null;
        return url;
    }

    private Song parseSong(JsonNode node) {
        try {
            long id = node.path("id").asLong(0);
            String name = node.path("name").asText("");
            String artist = extractArtist(node);
            JsonNode albumNode = node.has("al") ? node.path("al") : node.path("album");
            String album = albumNode.path("name").asText("");
            String coverUrl = albumNode.path("picUrl").asText("");
            int duration = node.has("dt") ? node.path("dt").asInt(0) : node.path("duration").asInt(0);

            if (name.isEmpty()) return null;

            Song song = new Song();
            song.setId((int) id);
            song.setTitle(name);
            song.setArtist(artist.isEmpty() ? "未知歌手" : artist);
            song.setAlbum(album);
            song.setCoverUrl(coverUrl);
            song.setDuration(duration / 1000.0); // 转为秒
            song.setFormat("mp3");
            song.setSource("netease");
            song.setSourceId(String.valueOf(id));
            return song;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractArtist(JsonNode node) {
        JsonNode artists = node.has("ar") ? node.path("ar") : node.path("artists");
        if (!artists.isArray() || artists.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (JsonNode artistNode : artists) {
            String name = artistNode.path("name").asText("");
            if (!name.isBlank()) {
                if (!sb.isEmpty()) sb.append(" / ");
                sb.append(name);
            }
        }
        return sb.toString();
    }
}
