package com.qiyunge.domain.entity;

public class PomodoroTaskTemplate {
    private int id;
    private int userId;
    private String title;
    private int estimatedPomodoros;
    private String tag;
    private String createdAt;

    public PomodoroTaskTemplate() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getEstimatedPomodoros() { return estimatedPomodoros; }
    public void setEstimatedPomodoros(int estimatedPomodoros) { this.estimatedPomodoros = estimatedPomodoros; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
