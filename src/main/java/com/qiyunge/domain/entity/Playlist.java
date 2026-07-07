package com.qiyunge.domain.entity;

import java.time.LocalDateTime;

/**
 * 歌单实体：用户创建的自定义歌曲集合。
 */
public class Playlist {
    private int id;
    private int userId;
    private String name;
    private String description;
    private String coverUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 瞬态字段：歌曲数量（通过 JOIN 查询计算）
    private int songCount;

    public Playlist() {}

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }

    @Override
    public String toString() {
        return name + " (" + songCount + " 首)";
    }
}
