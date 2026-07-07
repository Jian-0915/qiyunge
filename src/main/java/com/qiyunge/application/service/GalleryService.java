package com.qiyunge.application.service;

import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.domain.entity.ImageAlbum;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.repository.GalleryImageRepository;
import com.qiyunge.infrastructure.repository.ImageAlbumItemRepository;
import com.qiyunge.infrastructure.repository.ImageAlbumRepository;
import com.qiyunge.infrastructure.repository.UserImagePreferenceRepository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 图库业务服务：图片查询、上传、收藏、删除。
 */
public class GalleryService {

    private final GalleryImageRepository imageRepository;
    private final UserImagePreferenceRepository preferenceRepository;
    private final ImageAlbumRepository albumRepository;
    private final ImageAlbumItemRepository albumItemRepository;
    private final DatabaseManager dbManager;
    private final Path galleryDir;

    public GalleryService(GalleryImageRepository imageRepository,
                          UserImagePreferenceRepository preferenceRepository,
                          ImageAlbumRepository albumRepository,
                          ImageAlbumItemRepository albumItemRepository,
                          DatabaseManager dbManager,
                          Path galleryDir) {
        this.imageRepository = imageRepository;
        this.preferenceRepository = preferenceRepository;
        this.albumRepository = albumRepository;
        this.albumItemRepository = albumItemRepository;
        this.dbManager = dbManager;
        this.galleryDir = galleryDir;
    }

    /** 获取全部图片 */
    public List<GalleryImage> getAllImages() {
        return imageRepository.findAll();
    }

    /** 按分类获取 */
    public List<GalleryImage> getImagesByCategory(String category) {
        if (category == null || "全部".equals(category)) {
            return imageRepository.findAll();
        }
        return imageRepository.findByCategory(category);
    }

    /** 搜索图片 */
    public List<GalleryImage> searchImages(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return imageRepository.findAll();
        }
        return imageRepository.search(keyword.trim());
    }

    /** 获取用户收藏的图片列表 */
    public List<GalleryImage> getFavoriteImages(int userId) {
        Set<Integer> favIds = preferenceRepository.findFavoriteImageIds(userId);
        if (favIds == null || favIds.isEmpty()) return Collections.emptyList();
        return imageRepository.findByIds(new ArrayList<>(favIds)).stream()
            .peek(img -> img.setFavorited(true))
            .toList();
    }

    /** 按分类获取用户收藏的图片列表 */
    public List<GalleryImage> getFavoriteImagesByCategory(int userId, String category) {
        Set<Integer> favIds = preferenceRepository.findFavoriteImageIds(userId);
        if (favIds == null || favIds.isEmpty()) return Collections.emptyList();
        return imageRepository.findByIds(new ArrayList<>(favIds)).stream()
            .peek(img -> img.setFavorited(true))
            .filter(img -> category == null || "全部".equals(category) || category.equals(img.getCategory()))
            .toList();
    }

    /** 按 ID 获取 */
    public GalleryImage getImageById(int id) {
        return imageRepository.findById(id);
    }

    /**
     * 上传本地图片到图库。
     * @param sourceFile 源文件
     * @param category 分类
     * @param title 标题（为空时使用文件名）
     */
    public GalleryImage uploadImage(File sourceFile, String category, String title) throws Exception {
        // 校验文件
        if (sourceFile == null) throw new IllegalArgumentException("源文件不能为空");
        // 校验文件格式
        String fileName = sourceFile.getName().toLowerCase();
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")
            && !fileName.endsWith(".webp") && !fileName.endsWith(".gif")) {
            throw new IllegalArgumentException("不支持的图片格式，仅支持 JPG/PNG/WEBP/GIF");
        }

        // 确保目录存在
        Files.createDirectories(galleryDir);

        // 生成唯一文件名
        String uniqueName = System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "_" + sourceFile.getName();
        Path targetPath = galleryDir.resolve(uniqueName);

        // 复制文件
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // 获取文件大小
        long fileSize = Files.size(targetPath);

        // 构建实体
        GalleryImage image = new GalleryImage();
        image.setTitle(title != null && !title.isBlank() ? title : sourceFile.getName());
        image.setCategory(category != null ? category : "其他");
        image.setUrl(targetPath.toUri().toString());
        image.setLocalPath(targetPath.toString());
        image.setThumbnailUrl(targetPath.toUri().toString());
        image.setFileSize(fileSize);

        // 提取图片宽高
        try {
            javafx.scene.image.Image fxImage = new javafx.scene.image.Image(targetPath.toUri().toString(), true);
            if (fxImage.getWidth() > 0 && fxImage.getHeight() > 0) {
                image.setWidth((int) fxImage.getWidth());
                image.setHeight((int) fxImage.getHeight());
            }
        } catch (Exception ex) {
            System.err.println("[GalleryService] 提取图片尺寸失败: " + ex.getMessage());
        }

        image.setSource("local");

        return imageRepository.create(image);
    }

    /** 收藏/取消收藏 */
    public boolean toggleFavorite(int userId, int imageId) {
        if (preferenceRepository.isPreferenced(userId, imageId)) {
            preferenceRepository.removePreference(userId, imageId);
            return false;
        } else {
            preferenceRepository.addPreference(userId, imageId);
            return true;
        }
    }

    /** 判断是否已收藏 */
    public boolean isFavorited(int userId, int imageId) {
        return preferenceRepository.isPreferenced(userId, imageId);
    }

    /** 获取用户收藏的图片 ID 集合 */
    public Set<Integer> getFavoriteImageIds(int userId) {
        return preferenceRepository.findFavoriteImageIds(userId);
    }

    /** 批量标记收藏状态 */
    public void markFavoriteStatus(int userId, List<GalleryImage> images) {
        Set<Integer> favIds = getFavoriteImageIds(userId);
        for (GalleryImage image : images) {
            image.setFavorited(favIds.contains(image.getId()));
        }
    }

    /**
     * 删除图片（外键 CASCADE 自动清理收藏和图集关联）。
     */
    public boolean deleteImage(int id) {
        GalleryImage image = imageRepository.findById(id);
        if (image == null) return false;

        // 删除本地文件
        if (image.getLocalPath() != null) {
            try {
                Files.deleteIfExists(Path.of(image.getLocalPath()));
            } catch (Exception e) {
                System.err.println("[GalleryService] 删除图片文件失败: " + e.getMessage());
            }
        }

        return imageRepository.delete(id);
    }

    /** 统计总数 */
    public int countAll() {
        return imageRepository.countAll();
    }

    // ==================== 图集功能 ====================

    /** 获取所有图集 */
    public List<ImageAlbum> getAllAlbums() {
        return albumRepository.findAll();
    }

    /** 按 ID 获取图集 */
    public ImageAlbum getAlbumById(int id) {
        return albumRepository.findById(id);
    }

    /** 新建图集 */
    public ImageAlbum createAlbum(String name, String description) {
        ImageAlbum album = new ImageAlbum(name, description);
        return albumRepository.create(album);
    }

    /** 更新图集 */
    public boolean updateAlbum(ImageAlbum album) {
        return albumRepository.update(album);
    }

    /** 删除图集 */
    public boolean deleteAlbum(int albumId) {
        return albumRepository.delete(albumId);
    }

    /** 添加图片到图集 */
    public boolean addImageToAlbum(int albumId, int imageId) {
        return albumItemRepository.addItem(albumId, imageId);
    }

    /** 从图集移除图片 */
    public boolean removeImageFromAlbum(int albumId, int imageId) {
        return albumItemRepository.removeItem(albumId, imageId);
    }

    /** 获取图集中的图片列表（带收藏状态） */
    public List<GalleryImage> getAlbumImages(int albumId, int userId) {
        List<GalleryImage> images = albumItemRepository.findImagesByAlbumId(albumId);
        markFavoriteStatus(userId, images);
        return images;
    }

    /** 判断图片是否在图集中 */
    public boolean isImageInAlbum(int albumId, int imageId) {
        return albumItemRepository.isInAlbum(albumId, imageId);
    }

    /** 查询图片所属的所有图集 */
    public List<ImageAlbum> getAlbumsForImage(int imageId) {
        List<Integer> albumIds = albumItemRepository.findAlbumIdsByImageId(imageId);
        List<ImageAlbum> result = new ArrayList<>();
        for (int albumId : albumIds) {
            ImageAlbum album = albumRepository.findById(albumId);
            if (album != null) result.add(album);
        }
        return result;
    }


    /** 从在线图片保存到本地图库 */
    public GalleryImage uploadImageFromOnline(GalleryImage onlineImage) {
        GalleryImage image = new GalleryImage();
        image.setTitle(onlineImage.getTitle() != null ? onlineImage.getTitle() : "在线图片");
        image.setCategory(onlineImage.getCategory() != null && !onlineImage.getCategory().isBlank() ? onlineImage.getCategory() : "其他");
        image.setUrl(onlineImage.getUrl());
        image.setLocalPath(onlineImage.getLocalPath());
        image.setThumbnailUrl(onlineImage.getThumbnailUrl() != null ? onlineImage.getThumbnailUrl() : onlineImage.getUrl());
        image.setWidth(onlineImage.getWidth());
        image.setHeight(onlineImage.getHeight());
        image.setFileSize(onlineImage.getFileSize());
        image.setSubCategory(onlineImage.getSubCategory());
        image.setSource(onlineImage.getSource() != null ? onlineImage.getSource() : "local");
        return imageRepository.create(image);
    }
    /** 批量收藏 */
    public void batchFavorite(int userId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;
        dbManager.withTransaction(conn -> {
            for (int imageId : imageIds) {
                if (!preferenceRepository.isPreferenced(userId, imageId)) {
                    preferenceRepository.addPreference(userId, imageId);
                }
            }
        });
    }

    /** 批量取消收藏 */
    public void batchUnfavorite(int userId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;
        dbManager.withTransaction(conn -> {
            for (int imageId : imageIds) {
                preferenceRepository.removePreference(userId, imageId);
            }
        });
    }

    /** 批量删除图片 */
    public void batchDeleteImages(List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;
        dbManager.withTransaction(conn -> {
            for (int imageId : imageIds) {
                deleteImage(imageId);
            }
        });
    }

    /** 批量加入图集 */
    public void batchAddToAlbum(int albumId, List<Integer> imageIds) {
        if (imageIds == null || imageIds.isEmpty()) return;
        dbManager.withTransaction(conn -> {
            for (int imageId : imageIds) {
                albumItemRepository.addItem(albumId, imageId);
            }
        });
    }
}
