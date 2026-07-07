package com.qiyunge.ui.gallery;

import com.qiyunge.app.AppContext;
import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.domain.entity.ImageAlbum;
import com.qiyunge.application.service.AsyncExecutor;
import com.qiyunge.application.service.GalleryService;
import com.qiyunge.infrastructure.util.FileDownloader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 图库视图模型。
 */
public class GalleryViewModel {

    private final AppContext appContext;
    private final AsyncExecutor asyncExecutor;
    private final GalleryService galleryService;

    private final ObservableList<GalleryImage> images = FXCollections.observableArrayList();
    private final ObservableList<ImageAlbum> albums = FXCollections.observableArrayList();
    private final ObservableList<GalleryImage> albumImages = FXCollections.observableArrayList();
    private final ObservableList<GalleryImage> onlineImages = FXCollections.observableArrayList();
    private final StringProperty selectedCategory = new SimpleStringProperty("全部");
    private final StringProperty searchKeyword = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final java.util.List<Runnable> dataChangeListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public GalleryViewModel(AppContext appContext) {
        this.appContext = appContext;
        this.asyncExecutor = appContext.getAsyncExecutor();
        this.galleryService = appContext.getGalleryService();
    }

    public void addDataChangeListener(Runnable listener) {
        dataChangeListeners.add(listener);
    }

    private void notifyDataChanged() {
        dataChangeListeners.forEach(Runnable::run);
    }

    /** 加载图片列表 */
    public void loadImages() {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                int userId = appContext.getUserSession().getUserId();
                List<GalleryImage> list;

                String keyword = searchKeyword.get();
                if (keyword != null && !keyword.trim().isEmpty()) {
                    list = galleryService.searchImages(keyword);
                } else {
                    list = galleryService.getImagesByCategory(selectedCategory.get());
                }

                galleryService.markFavoriteStatus(userId, list);

                Platform.runLater(() -> {
                    images.setAll(list);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("加载图片失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 加载收藏图片列表 */
    public void loadFavoriteImages() {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                int userId = appContext.getUserSession().getUserId();
                List<GalleryImage> list = galleryService.getFavoriteImages(userId);
                Platform.runLater(() -> {
                    images.setAll(list);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("加载收藏图片失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 按分类加载收藏图片列表 */
    public void loadFavoriteImagesByCategory(String category) {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                int userId = appContext.getUserSession().getUserId();
                List<GalleryImage> list = galleryService.getFavoriteImagesByCategory(userId, category);
                Platform.runLater(() -> {
                    images.setAll(list);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("加载收藏图片失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 切换分类 */
    public void filterByCategory(String category) {
        selectedCategory.set(category);
        searchKeyword.set("");
        loadImages();
    }

    /** 搜索 */
    public void search(String keyword) {
        searchKeyword.set(keyword);
        loadImages();
    }

    /** 排序图片 */
    public void sortImages(String sortKey) {
        List<GalleryImage> list = new ArrayList<>(images);
        switch (sortKey) {
            case "时间↓" -> list.sort((a, b) -> {
                String da = a.getCreatedAt() != null ? a.getCreatedAt() : "";
                String db = b.getCreatedAt() != null ? b.getCreatedAt() : "";
                return db.compareTo(da);
            });
            case "时间↑" -> list.sort((a, b) -> {
                String da = a.getCreatedAt() != null ? a.getCreatedAt() : "";
                String db = b.getCreatedAt() != null ? b.getCreatedAt() : "";
                return da.compareTo(db);
            });
            case "名称" -> list.sort((a, b) -> {
                String na = a.getTitle() != null ? a.getTitle() : "";
                String nb = b.getTitle() != null ? b.getTitle() : "";
                return na.compareToIgnoreCase(nb);
            });
            case "大小↓" -> list.sort((a, b) -> Long.compare(b.getFileSize(), a.getFileSize()));
            case "大小↑" -> list.sort((a, b) -> Long.compare(a.getFileSize(), b.getFileSize()));
            default -> {}
        }
        images.setAll(list);
    }

    /** 上传图片 */
    public void uploadImage(File file, String category, String title) {
        if (file == null) return;
        loading.set(true);
        errorMessage.set("正在上传：" + file.getName());
        asyncExecutor.execute(() -> {
            try {
                galleryService.uploadImage(file, category, title);
                Platform.runLater(() -> {
                    errorMessage.set("上传成功：" + file.getName());
                    loading.set(false);
                    loadImages();
                    notifyDataChanged();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("上传失败: " + e.getMessage());
                    loading.set(false);
                    appContext.getDialogService().showError("上传失败", e.getMessage());
                });
            }
        });
    }

    /** 收藏/取消收藏 */
    public void toggleFavorite(GalleryImage image) {
        if (image == null) return;
        int userId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                boolean nowFav = galleryService.toggleFavorite(userId, image.getId());
                Platform.runLater(() -> {
                    image.setFavorited(nowFav);
                    notifyDataChanged();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("收藏操作失败: " + e.getMessage());
                });
            }
        });
    }

    /** 删除图片 */
    public void deleteImage(int imageId) {
        asyncExecutor.execute(() -> {
            try {
                galleryService.deleteImage(imageId);
                Platform.runLater(() -> {
                    errorMessage.set("已删除图片");
                    loadImages();
                    notifyDataChanged();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    appContext.getDialogService().showError("删除失败", e.getMessage());
                });
            }
        });
    }

    // ==================== 图集功能 ====================

    /** 加载图集列表 */
    public void loadAlbums() {
        asyncExecutor.execute(() -> {
            try {
                List<ImageAlbum> list = galleryService.getAllAlbums();
                Platform.runLater(() -> albums.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("加载图集失败: " + e.getMessage()));
            }
        });
    }

    /** 新建图集 */
    public void createAlbum(String name, String description) {
        asyncExecutor.execute(() -> {
            try {
                galleryService.createAlbum(name, description);
                Platform.runLater(() -> {
                    loadAlbums();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("创建图集失败: " + e.getMessage()));
            }
        });
    }

    /** 删除图集 */
    public void deleteAlbum(int albumId) {
        asyncExecutor.execute(() -> {
            try {
                galleryService.deleteAlbum(albumId);
                Platform.runLater(() -> {
                    loadAlbums();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("删除图集失败: " + e.getMessage()));
            }
        });
    }

    /** 加载图集中的图片 */
    public void loadAlbumImages(int albumId) {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                int userId = appContext.getUserSession().getUserId();
                List<GalleryImage> list = galleryService.getAlbumImages(albumId, userId);
                Platform.runLater(() -> {
                    albumImages.setAll(list);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("加载图集图片失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 添加图片到图集 */
    public void addImageToAlbum(int albumId, int imageId) {
        asyncExecutor.execute(() -> {
            try {
                galleryService.addImageToAlbum(albumId, imageId);
                Platform.runLater(() -> notifyDataChanged());
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("添加到图集失败: " + e.getMessage()));
            }
        });
    }

    /** 从图集移除图片 */
    public void removeImageFromAlbum(int albumId, int imageId) {
        asyncExecutor.execute(() -> {
            try {
                galleryService.removeImageFromAlbum(albumId, imageId);
                Platform.runLater(() -> notifyDataChanged());
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("从图集移除失败: " + e.getMessage()));
            }
        });
    }

    /** 查询图片所属的所有图集 */
    public List<ImageAlbum> getAlbumsForImage(int imageId) {
        return galleryService.getAlbumsForImage(imageId);
    }

    // ==================== 批量操作 ====================

    /** 批量收藏 */
    public void batchFavorite(List<GalleryImage> selectedImages) {
        int userId = appContext.getUserSession().getUserId();
        List<Integer> ids = selectedImages.stream().map(GalleryImage::getId).toList();
        asyncExecutor.execute(() -> {
            try {
                galleryService.batchFavorite(userId, ids);
                Platform.runLater(() -> {
                    for (GalleryImage img : selectedImages) {
                        img.setFavorited(true);
                    }
                    errorMessage.set("已收藏 " + ids.size() + " 张图片");
                    notifyDataChanged();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("批量收藏失败: " + e.getMessage()));
            }
        });
    }

    /** 批量删除 */
    public void batchDelete(List<GalleryImage> selectedImages) {
        List<Integer> ids = selectedImages.stream().map(GalleryImage::getId).toList();
        asyncExecutor.execute(() -> {
            try {
                galleryService.batchDeleteImages(ids);
                Platform.runLater(() -> {
                    errorMessage.set("已删除 " + ids.size() + " 张图片");
                    loadImages();
                    notifyDataChanged();
                });
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("批量删除失败: " + e.getMessage()));
            }
        });
    }

    /** 批量加入图集 */
    public void batchAddToAlbum(int albumId, List<GalleryImage> selectedImages) {
        List<Integer> ids = selectedImages.stream().map(GalleryImage::getId).toList();
        asyncExecutor.execute(() -> {
            try {
                galleryService.batchAddToAlbum(albumId, ids);
                Platform.runLater(() -> errorMessage.set("已加入图集 " + ids.size() + " 张图片"));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("批量加入图集失败: " + e.getMessage()));
            }
        });
    }


    // ==================== 在线寻图 ====================

    /** 搜索在线图片 */
    public void searchOnline(String keyword) {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                List<GalleryImage> results = appContext.getOnlineImageService().search(keyword);
                Platform.runLater(() -> {
                    onlineImages.setAll(results);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("在线搜索失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 刷新在线图片（使用随机页码获取新内容） */
    public void refreshOnline(String keyword) {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                List<GalleryImage> results = appContext.getOnlineImageService().searchWithRandomPage(keyword);
                Platform.runLater(() -> {
                    onlineImages.setAll(results);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("在线搜索失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 按平台搜索在线图片 */
    public void searchOnlineByProvider(String keyword, String providerId) {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                List<GalleryImage> results = appContext.getOnlineImageService().searchByProvider(keyword, providerId);
                Platform.runLater(() -> {
                    onlineImages.setAll(results);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("在线搜索失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 按平台刷新在线图片（使用随机页码获取新内容） */
    public void refreshOnlineByProvider(String keyword, String providerId) {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                List<GalleryImage> results = appContext.getOnlineImageService().searchByProviderWithRandomPage(keyword, providerId);
                Platform.runLater(() -> {
                    onlineImages.setAll(results);
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("在线搜索失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 下载在线图片到本地 */
    public void downloadOnlineImage(GalleryImage image, String category) {
        if (image == null || image.getUrl() == null || image.getUrl().isBlank()) return;
        if (category != null && !"全部".equals(category)) {
            image.setCategory(category);
        }
        loading.set(true);
        errorMessage.set("正在下载：" + safeTitle(image));
        asyncExecutor.execute(() -> {
            try {
                saveOnlineImage(image, false);
                Platform.runLater(() -> {
                    errorMessage.set("下载成功：" + safeTitle(image));
                    loading.set(false);
                    loadImages();
                    notifyDataChanged();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("下载失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    /** 收藏在线图片（先下载再收藏） */
    public void favoriteOnlineImage(GalleryImage image, String category) {
        if (image == null || image.getUrl() == null || image.getUrl().isBlank()) return;
        if (category != null && !"全部".equals(category)) {
            image.setCategory(category);
        }
        int userId = appContext.getUserSession().getUserId();
        loading.set(true);
        errorMessage.set("正在收藏：" + safeTitle(image));
        asyncExecutor.execute(() -> {
            try {
                GalleryImage saved = saveOnlineImage(image, true);
                if (saved != null) {
                    galleryService.toggleFavorite(userId, saved.getId());
                    image.setFavorited(true);
                    Platform.runLater(() -> {
                        errorMessage.set("已收藏：" + safeTitle(image));
                        loading.set(false);
                        loadImages();
                        notifyDataChanged();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("收藏失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    private GalleryImage saveOnlineImage(GalleryImage image, boolean favorite) throws Exception {
        byte[] imageData = FileDownloader.downloadBytes(image.getUrl());
        if (imageData == null || imageData.length == 0) {
            throw new IllegalStateException("无法下载图片");
        }

        // 按分类创建子目录：.qiyunge/cache/gallery/{分类}/
        String category = image.getCategory() != null && !image.getCategory().isEmpty() ? image.getCategory() : "其他";
        Path categoryDir = appContext.getAppStorage().getGalleryCachePath().resolve(category);
        Files.createDirectories(categoryDir);

        String fileName = System.currentTimeMillis() + (favorite ? "_fav" : "_online") + guessExtension(image.getUrl());
        Path localFile = categoryDir.resolve(fileName);
        Files.write(localFile, imageData);

        GalleryImage localImage = new GalleryImage();
        localImage.setTitle(safeTitle(image));
        localImage.setCategory(category);
        localImage.setSubCategory(image.getSubCategory());
        localImage.setUrl(localFile.toUri().toString());
        localImage.setLocalPath(localFile.toString());
        localImage.setThumbnailUrl(localFile.toUri().toString());
        localImage.setFileSize(imageData.length);
        localImage.setWidth(image.getWidth());
        localImage.setHeight(image.getHeight());
        localImage.setSource("local");
        return galleryService.uploadImageFromOnline(localImage);
    }

    private String safeTitle(GalleryImage image) {
        return image.getTitle() != null && !image.getTitle().isBlank() ? image.getTitle() : "在线图片";
    }

    private String guessExtension(String url) {
        if (url == null) return ".jpg";
        String path = url;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) path = path.substring(0, queryIndex);
        path = path.toLowerCase();
        if (path.endsWith(".png")) return ".png";
        if (path.endsWith(".webp")) return ".webp";
        if (path.endsWith(".gif")) return ".gif";
        if (path.endsWith(".jpeg")) return ".jpeg";
        return ".jpg";
    }
    // ===== Getter =====

    public ObservableList<GalleryImage> getImages() { return images; }
    public ObservableList<ImageAlbum> getAlbums() { return albums; }
    public ObservableList<GalleryImage> getAlbumImages() { return albumImages; }
    public StringProperty selectedCategoryProperty() { return selectedCategory; }
    public StringProperty searchKeywordProperty() { return searchKeyword; }
    public StringProperty errorMessageProperty() { return errorMessage; }
    public ObservableList<GalleryImage> getOnlineImages() { return onlineImages; }
    public BooleanProperty loadingProperty() { return loading; }
}
