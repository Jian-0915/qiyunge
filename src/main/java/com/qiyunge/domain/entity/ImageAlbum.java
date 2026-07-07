package com.qiyunge.domain.entity;

/**
 * 图集实体：用户创建的自定义图片集合。
 */
public class ImageAlbum {

    private int id;
    private String name;
    private String description;
    private int coverImageId;
    private String createdAt;

    // 瞬态字段：图片数量（通过 JOIN 查询计算）
    private int imageCount;

    public ImageAlbum() {}

    public ImageAlbum(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCoverImageId() { return coverImageId; }
    public void setCoverImageId(int coverImageId) { this.coverImageId = coverImageId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public int getImageCount() { return imageCount; }
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }

    @Override
    public String toString() {
        return name + " (" + imageCount + " 张)";
    }
}
