package com.qiyunge.ui.gallery;

import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.domain.entity.ImageAlbum;
import com.qiyunge.ui.components.AppButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Optional;

/**
 * 图片详情弹窗：大图预览 + 信息面板 + 翻页。
 */
public class ImageDetailDialog {

    private final Stage dialog;
    private final List<GalleryImage> images;
    private int currentIndex;
    private final Runnable onFavoriteCallback;
    private final Runnable onDeleteCallback;
    private final OnAddToAlbumCallback onAddToAlbumCallback;

    private ImageView largeImageView;
    private Label titleLabel;
    private Label categoryLabel;
    private Label sizeLabel;
    private Label resolutionLabel;
    private Label dateLabel;
    private AppButton favBtn;
    private AppButton deleteBtn;
    private AppButton downloadBtn;
    private AppButton albumBtn;
    private AppButton prevBtn;
    private AppButton nextBtn;

    public interface OnAddToAlbumCallback {
        List<ImageAlbum> getAlbums();
        void addToAlbum(int albumId, GalleryImage image);
        void removeFromAlbum(int albumId, GalleryImage image);
        List<ImageAlbum> getAlbumsForImage(int imageId);
    }

    public ImageDetailDialog(List<GalleryImage> images, int startIndex,
                              Runnable onFavoriteCallback, Runnable onDeleteCallback) {
        this(images, startIndex, onFavoriteCallback, onDeleteCallback, null);
    }

    public ImageDetailDialog(List<GalleryImage> images, int startIndex,
                              Runnable onFavoriteCallback, Runnable onDeleteCallback,
                              OnAddToAlbumCallback onAddToAlbumCallback) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("images list cannot be null or empty");
        }
        if (startIndex < 0 || startIndex >= images.size()) {
            startIndex = 0;
        }

        this.images = images;
        this.currentIndex = startIndex;
        this.onFavoriteCallback = onFavoriteCallback;
        this.onDeleteCallback = onDeleteCallback;
        this.onAddToAlbumCallback = onAddToAlbumCallback;

        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setWidth(1000);
        dialog.setHeight(700);

        Scene scene = new Scene(createContent());
        var themeUrl = getClass().getResource("/styles/theme.css");
        if (themeUrl != null) scene.getStylesheets().add(themeUrl.toExternalForm());
        var galleryUrl = getClass().getResource("/css/gallery.css");
        if (galleryUrl != null) scene.getStylesheets().add(galleryUrl.toExternalForm());
        dialog.setScene(scene);
    }

    private VBox createContent() {
        VBox root = new VBox();
        root.getStyleClass().add("gallery-detail-dialog");
        root.setPadding(new Insets(0));

        // 关闭按钮
        Button closeBtn = new Button("\u00D7"); // ×
        closeBtn.getStyleClass().add("gallery-close-btn");
        closeBtn.setOnAction(e -> dialog.close());
        HBox topBar = new HBox(closeBtn);
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(12, 16, 0, 0));

        // 左侧大图（包裹在 ScrollPane 中，限制显示区域）
        largeImageView = new ImageView();
        largeImageView.setPreserveRatio(true);
        largeImageView.getStyleClass().add("gallery-detail-image");
        largeImageView.setSmooth(true);

        ScrollPane imageScrollPane = new ScrollPane(largeImageView);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");

        // 右侧信息面板
        titleLabel = new Label();
        titleLabel.getStyleClass().add("gallery-detail-title");
        titleLabel.setWrapText(true);

        categoryLabel = new Label();
        categoryLabel.getStyleClass().add("gallery-detail-info");

        sizeLabel = new Label();
        sizeLabel.getStyleClass().add("gallery-detail-info");

        resolutionLabel = new Label();
        resolutionLabel.getStyleClass().add("gallery-detail-info");

        dateLabel = new Label();
        dateLabel.getStyleClass().add("gallery-detail-info");

        // 操作按钮
        favBtn = new AppButton("收藏", AppButton.Style.OUTLINE);
        favBtn.setOnAction(e -> {
            GalleryImage image = images.get(currentIndex);
            image.setFavorited(!image.isFavorited());
            updateFavoriteButton();
            if (onFavoriteCallback != null) onFavoriteCallback.run();
        });

        downloadBtn = new AppButton("下载", AppButton.Style.SECONDARY);
        downloadBtn.setOnAction(e -> {
            // TODO: 下载功能
        });

        deleteBtn = new AppButton("删除", AppButton.Style.DANGER);
        deleteBtn.setOnAction(e -> {
            if (onDeleteCallback != null) onDeleteCallback.run();
            dialog.close();
        });

        albumBtn = new AppButton("图集", AppButton.Style.OUTLINE);
        albumBtn.setOnAction(e -> {
            if (onAddToAlbumCallback == null) return;
            GalleryImage currentImage = images.get(currentIndex);
            List<ImageAlbum> inAlbums = onAddToAlbumCallback.getAlbumsForImage(currentImage.getId());

            if (!inAlbums.isEmpty()) {
                // 当前图片已加入图集 → 显示移出选项
                ChoiceDialog<ImageAlbum> choiceDialog = new ChoiceDialog<>(inAlbums.get(0), inAlbums);
                choiceDialog.setTitle("移出图集");
                choiceDialog.setHeaderText("选择要移出的图集");
                choiceDialog.setContentText("图集：");
                Optional<ImageAlbum> result = choiceDialog.showAndWait();
                result.ifPresent(album -> {
                    onAddToAlbumCallback.removeFromAlbum(album.getId(), currentImage);
                    updateAlbumButton();
                });
            } else {
                // 当前图片未加入任何图集 → 显示加入选项
                List<ImageAlbum> allAlbums = onAddToAlbumCallback.getAlbums();
                if (allAlbums.isEmpty()) return;
                // 过滤掉已加入的图集
                List<ImageAlbum> availableAlbums = allAlbums.stream()
                    .filter(a -> inAlbums.stream().noneMatch(ia -> ia.getId() == a.getId()))
                    .toList();
                if (availableAlbums.isEmpty()) return;
                ChoiceDialog<ImageAlbum> choiceDialog = new ChoiceDialog<>(availableAlbums.get(0), availableAlbums);
                choiceDialog.setTitle("加入图集");
                choiceDialog.setHeaderText("选择要加入的图集");
                choiceDialog.setContentText("图集：");
                Optional<ImageAlbum> result = choiceDialog.showAndWait();
                result.ifPresent(album -> {
                    onAddToAlbumCallback.addToAlbum(album.getId(), currentImage);
                    updateAlbumButton();
                });
            }
        });

        HBox actionBox = new HBox(12, favBtn, albumBtn, downloadBtn, deleteBtn);
        actionBox.setAlignment(Pos.CENTER_LEFT);

        VBox infoPanel = new VBox(16, titleLabel, categoryLabel, sizeLabel, resolutionLabel, dateLabel,
            new Region(), actionBox);
        infoPanel.setPadding(new Insets(24));
        infoPanel.setPrefWidth(360);
        infoPanel.setMinWidth(360);
        VBox.setVgrow(infoPanel, Priority.ALWAYS);

        // 左右分栏
        HBox mainContent = new HBox(24, imageScrollPane, infoPanel);
        mainContent.setPadding(new Insets(0, 24, 0, 24));
        HBox.setHgrow(imageScrollPane, Priority.ALWAYS);

        // 翻页控件
        prevBtn = new AppButton("\u2039 上一张", AppButton.Style.GHOST);
        prevBtn.setOnAction(e -> navigate(-1));

        nextBtn = new AppButton("下一张 \u203A", AppButton.Style.GHOST);
        nextBtn.setOnAction(e -> navigate(1));

        HBox navBar = new HBox(12, prevBtn, nextBtn);
        navBar.setAlignment(Pos.CENTER);
        navBar.setPadding(new Insets(16));

        root.getChildren().addAll(topBar, mainContent, navBar);
        updateContent();
        return root;
    }

    private void navigate(int delta) {
        int newIndex = currentIndex + delta;
        if (newIndex >= 0 && newIndex < images.size()) {
            currentIndex = newIndex;
            updateContent();
        }
    }

    private void updateContent() {
        GalleryImage image = images.get(currentIndex);
        titleLabel.setText(image.getTitle() != null ? image.getTitle() : "未命名");
        categoryLabel.setText("分类：" + (image.getCategory() != null ? image.getCategory() : "其他"));
        sizeLabel.setText("大小：" + image.getDisplayFileSize());
        resolutionLabel.setText("分辨率：" + image.getDisplayResolution());
        dateLabel.setText("上传：" + (image.getCreatedAt() != null ? image.getCreatedAt() : "未知"));

        try {
            if (image.getUrl() != null && !image.getUrl().isBlank()) {
                largeImageView.setImage(new Image(image.getUrl(), true));
            } else if (image.getLocalPath() != null) {
                largeImageView.setImage(new Image("file:" + image.getLocalPath(), true));
            }
        } catch (Exception e) {
            System.err.println("[ImageDetailDialog] 加载大图失败: " + e.getMessage());
        }

        updateFavoriteButton();
        updateAlbumButton();

        prevBtn.setDisable(currentIndex <= 0);
        nextBtn.setDisable(currentIndex >= images.size() - 1);
    }

    private void updateFavoriteButton() {
        GalleryImage image = images.get(currentIndex);
        if (image.isFavorited()) {
            favBtn.setText("已收藏");
            favBtn.getStyleClass().remove("outline");
        } else {
            favBtn.setText("收藏");
            if (!favBtn.getStyleClass().contains("outline")) {
                favBtn.getStyleClass().add("outline");
            }
        }
    }

    private void updateAlbumButton() {
        if (onAddToAlbumCallback == null) return;
        GalleryImage image = images.get(currentIndex);
        List<ImageAlbum> inAlbums = onAddToAlbumCallback.getAlbumsForImage(image.getId());
        if (inAlbums.isEmpty()) {
            albumBtn.setText("加入");
            albumBtn.setStyle("");
        } else {
            albumBtn.setText("移出 (" + inAlbums.size() + ")");
        }
    }

    public void show() {
        dialog.showAndWait();
    }
}
