package com.qiyunge.domain.entity;

/**
 * 图库图片实体。
 */
public class GalleryImage {

    private int id;
    private String title;
    private String category;
    private String subCategory;
    private String url;
    private String localPath;
    private String thumbnailUrl;
    private int width;
    private int height;
    private long fileSize;
    private String source;
    private String createdAt;

    /** 运行时标记，不入库 */
    private boolean favorited;

    public GalleryImage() {
    }

    public GalleryImage(String title, String category, String url) {
        this.title = title;
        this.category = category;
        this.url = url;
        this.source = "local";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSubCategory() { return subCategory; }
    public void setSubCategory(String subCategory) { this.subCategory = subCategory; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isFavorited() { return favorited; }
    public void setFavorited(boolean favorited) { this.favorited = favorited; }

    /**
     * 获取显示用的文件大小文本。
     */
    public String getDisplayFileSize() {
        if (fileSize <= 0) return "未知";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    /**
     * 获取显示用的分辨率文本。
     */
    public String getDisplayResolution() {
        if (width <= 0 || height <= 0) return "未知";
        return width + " × " + height;
    }
}
