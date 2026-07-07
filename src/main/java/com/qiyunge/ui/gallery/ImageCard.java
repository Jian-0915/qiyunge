package com.qiyunge.ui.gallery;

import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.infrastructure.util.FileDownloader;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.ByteArrayInputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 图片卡片组件：缩略图 + 标题 + 分类 + 收藏图标。
 * 优先使用 JavaFX Image 直接加载 URL，失败后回退到 HttpClient 字节下载。
 */
public class ImageCard extends StackPane {

    private final GalleryImage image;
    private final ImageView imageView;
    private final Label favIcon;
    private final Runnable onClickCallback;
    private final Runnable onFavoriteCallback;
    private final java.util.concurrent.atomic.AtomicBoolean fallbackStarted = new java.util.concurrent.atomic.AtomicBoolean(false);
    private boolean selected = false;

    public ImageCard(GalleryImage image, Runnable onClickCallback, Runnable onFavoriteCallback) {
        this.image = image;
        this.onClickCallback = onClickCallback;
        this.onFavoriteCallback = onFavoriteCallback;

        this.getStyleClass().add("gallery-image-card");
        this.setPrefSize(200, 220);

        // 图片
        imageView = new ImageView();
        imageView.getStyleClass().add("gallery-card-image");
        imageView.setFitWidth(200);
        imageView.setFitHeight(160);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        loadImage();

        // 收藏图标（右上角）
        favIcon = new Label(image.isFavorited() ? "\u2665" : "\u2661");
        favIcon.getStyleClass().add("gallery-fav-icon");
        favIcon.setStyle(image.isFavorited()
            ? "-fx-text-fill: -primary; -fx-font-size: 18;"
            : "-fx-text-fill: white; -fx-font-size: 18; -fx-opacity: 0;");
        StackPane.setAlignment(favIcon, Pos.TOP_RIGHT);
        StackPane.setMargin(favIcon, new Insets(8));

        // 悬停显示收藏图标
        this.setOnMouseEntered(e -> {
            if (!image.isFavorited()) {
                favIcon.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-opacity: 0.9;");
            }
        });
        this.setOnMouseExited(e -> {
            if (!image.isFavorited()) {
                favIcon.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-opacity: 0;");
            }
        });

        // 收藏点击
        favIcon.setOnMouseClicked(e -> {
            e.consume();
            image.setFavorited(!image.isFavorited());
            updateFavoriteIcon();
            if (onFavoriteCallback != null) onFavoriteCallback.run();
        });

        // 底部信息
        Label titleLabel = new Label(image.getTitle() != null ? image.getTitle() : "未命名");
        titleLabel.getStyleClass().add("gallery-card-title");
        titleLabel.setMaxWidth(200);
        titleLabel.setWrapText(true);

        // 底部分类/作者信息：风景 / by 作者
        String categoryText = image.getCategory() != null ? image.getCategory() : "其他";
        String subCategory = image.getSubCategory();
        if (subCategory != null && !subCategory.isBlank()) {
            categoryText = categoryText + " / " + subCategory;
        }
        Label categoryLabel = new Label(categoryText);
        categoryLabel.getStyleClass().add("gallery-card-category");

        VBox infoBox = new VBox(2, titleLabel, categoryLabel);
        infoBox.getStyleClass().add("gallery-card-info");
        infoBox.setAlignment(Pos.CENTER_LEFT);

        // 整体布局
        VBox cardContent = new VBox(0, imageView, infoBox);
        StackPane root = new StackPane(cardContent, favIcon);
        this.getChildren().add(root);

        // 点击事件
        this.setOnMouseClicked(e -> {
            if (e.isControlDown()) {
                setSelected(!selected);
            } else if (onClickCallback != null) {
                onClickCallback.run();
            }
        });
    }

    private void loadImage() {
        String thumbnailUrl = image.getThumbnailUrl();
        String mainUrl = image.getUrl();
        String displayUrl = thumbnailUrl != null && !thumbnailUrl.isBlank() ? thumbnailUrl : mainUrl;

        if (displayUrl == null || displayUrl.isBlank()) {
            System.err.println("[ImageCard] 图片URL为空");
            return;
        }

        // JavaFX Image 对部分 CDN 图片会静默失败，失败后回退到 HttpClient 字节下载。
        try {
            Image img = new Image(displayUrl, true); // true = 后台加载
            img.errorProperty().addListener((obs, old, err) -> {
                if (err) {
                    System.err.println("[ImageCard] JavaFX Image 加载失败: " + displayUrl);
                    loadImageByHttp(displayUrl);
                }
            });
            imageView.setImage(img);
            CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() -> {
                if (img.getProgress() < 1 || img.isError() || imageView.getImage() == null) {
                    loadImageByHttp(displayUrl);
                }
            });
        } catch (Exception e) {
            System.err.println("[ImageCard] 加载图片失败: " + e.getMessage());
            loadImageByHttp(displayUrl);
        }
    }

    private void loadImageByHttp(String displayUrl) {
        if (!fallbackStarted.compareAndSet(false, true)) return;

        CompletableFuture.supplyAsync(() -> FileDownloader.downloadBytes(displayUrl))
            .thenAccept(bytes -> {
                if (bytes == null || bytes.length == 0) {
                    System.err.println("[ImageCard] HttpClient 下载图片失败: " + displayUrl);
                    return;
                }
                Platform.runLater(() -> {
                    try {
                        imageView.setImage(new Image(new ByteArrayInputStream(bytes)));
                    } catch (Exception e) {
                        System.err.println("[ImageCard] 字节图片渲染失败: " + e.getMessage());
                    }
                });
            });
    }

    private void updateFavoriteIcon() {
        if (image.isFavorited()) {
            favIcon.setText("\u2665");
            favIcon.setStyle("-fx-text-fill: -primary; -fx-font-size: 18;");
        } else {
            favIcon.setText("\u2661");
            favIcon.setStyle("-fx-text-fill: white; -fx-font-size: 18; -fx-opacity: 0;");
        }
    }

    public GalleryImage getImage() {
        return image;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            this.setStyle("-fx-border-color: -primary; -fx-border-width: 2px; -fx-border-radius: 12px;");
        } else {
            this.setStyle(null);
        }
    }
}
