package com.qiyunge.domain.entity;

public class PomodoroTask {
    private int id;
    private int userId;
    private String title;
    private int estimatedPomodoros;
    private int completedPomodoros;
    private String tag;
    private int isCompleted;
    private int sortOrder;
    private String taskDate;
    private String createdAt;
    private String updatedAt;

    public PomodoroTask() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getEstimatedPomodoros() { return estimatedPomodoros; }
    public void setEstimatedPomodoros(int estimatedPomodoros) { this.estimatedPomodoros = estimatedPomodoros; }
    public int getCompletedPomodoros() { return completedPomodoros; }
    public void setCompletedPomodoros(int completedPomodoros) { this.completedPomodoros = completedPomodoros; }
    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }
    public int getIsCompleted() { return isCompleted; }
    public void setIsCompleted(int isCompleted) { this.isCompleted = isCompleted; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    public String getTaskDate() { return taskDate; }
    public void setTaskDate(String taskDate) { this.taskDate = taskDate; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}
