package com.qiyunge.domain.entity;

import java.time.LocalDateTime;

/**
 * 用户人脸数据实体
 */
public class UserFaceData {
    private int id;
    private int userId;
    private String modelPath;       // LBPH 模型文件路径
    private String faceImagePath;   // 人脸样本图片目录
    private int sampleCount;        // 采集样本数量
    private boolean enabled;        // 是否启用人脸登录
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserFaceData() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }

    public String getFaceImagePath() { return faceImagePath; }
    public void setFaceImagePath(String faceImagePath) { this.faceImagePath = faceImagePath; }

    public int getSampleCount() { return sampleCount; }
    public void setSampleCount(int sampleCount) { this.sampleCount = sampleCount; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
