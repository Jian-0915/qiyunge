package com.qiyunge.domain.entity;

import java.time.LocalDateTime;

public class Song {
    private int id;
    private String title;
    private String artist;
    private String album;
    private double duration;
    private String url;
    private String coverUrl;
    private String lyricUrl;
    private String source;
    private String sourceId;
    private String format;  // mp3, m4a, wav, flac, aac, ogg
    private String codec;   // MP3, AAC, PCM, FLAC, Vorbis, Opus
    private LocalDateTime createdAt;

    // Transient: whether current user has favorited this song
    private boolean favorited;

    public Song() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public double getDuration() { return duration; }
    public void setDuration(double duration) { this.duration = duration; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getLyricUrl() { return lyricUrl; }
    public void setLyricUrl(String lyricUrl) { this.lyricUrl = lyricUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }

    public String getDisplayTitle() {
        return title != null ? title : "未知歌曲";
    }

    public String getDisplayArtist() {
        return artist != null ? artist : "未知歌手";
    }

    public String getDurationText() {
        if (duration <= 0) return "--:--";
        int min = (int) (duration / 60);
        int sec = (int) (duration % 60);
        return min + ":" + String.format("%02d", sec);
    }

    /**
     * 显示格式标签，如 "MP3"、"AAC"、"FLAC"。
     * 优先显示 codec，其次 format，最后从后缀推断。
     */
    public String getFormatLabel() {
        if (codec != null && !codec.isEmpty()) return codec;
        if (format != null && !format.isEmpty()) return format.toUpperCase();
        // Fallback from URL extension
        if (url != null) {
            int dot = url.lastIndexOf('.');
            if (dot > 0) {
                String ext = url.substring(dot + 1).split("[?#]")[0].toLowerCase();
                return switch (ext) {
                    case "mp3" -> "MP3";
                    case "m4a", "mp4" -> "M4A";
                    case "flac" -> "FLAC";
                    case "wav" -> "WAV";
                    case "ogg", "oga" -> "OGG";
                    case "wma" -> "WMA";
                    case "aac" -> "AAC";
                    default -> ext.toUpperCase();
                };
            }
        }
        return "--";
    }
}
