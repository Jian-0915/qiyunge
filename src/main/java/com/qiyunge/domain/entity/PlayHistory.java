package com.qiyunge.domain.entity;

import java.time.LocalDateTime;

public class PlayHistory {
    private int id;
    private int userId;
    private int songId;
    private LocalDateTime playedAt;

    // Joined fields
    private String songTitle;
    private String songArtist;
    private double songDuration;

    public PlayHistory() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getSongId() { return songId; }
    public void setSongId(int songId) { this.songId = songId; }
    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
    public String getSongTitle() { return songTitle; }
    public void setSongTitle(String songTitle) { this.songTitle = songTitle; }
    public String getSongArtist() { return songArtist; }
    public void setSongArtist(String songArtist) { this.songArtist = songArtist; }
    public double getSongDuration() { return songDuration; }
    public void setSongDuration(double songDuration) { this.songDuration = songDuration; }
}
