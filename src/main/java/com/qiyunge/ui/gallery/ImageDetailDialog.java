package com.qiyunge.ui.gallery;

import com.qiyunge.app.AppContext;
import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.domain.entity.ImageAlbum;
import com.qiyunge.ui.components.AppButton;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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
    private final AppContext appContext;
    private int currentIndex;
    private final Runnable onFavoriteCallback;
    private final Runnable onDeleteCallback;
    private final OnAddToAlbumCallback onAddToAlbumCallback;

    private ImageView largeImageView;
    private Label titleLabel;
    private Label categoryValueLabel;
    private Label sizeValueLabel;
    private Label resolutionValueLabel;
    private Label dateValueLabel;
    private Label albumValueLabel;
    private Label pageLabel;
    private HBox tagsBox;
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

    public ImageDetailDialog(AppContext appContext, List<GalleryImage> images, int startIndex,
                              Runnable onFavoriteCallback, Runnable onDeleteCallback) {
        this(appContext, images, startIndex, onFavoriteCallback, onDeleteCallback, null);
    }

    public ImageDetailDialog(AppContext appContext, List<GalleryImage> images, int startIndex,
                              Runnable onFavoriteCallback, Runnable onDeleteCallback,
                              OnAddToAlbumCallback onAddToAlbumCallback) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("images list cannot be null or empty");
        }
        if (startIndex < 0 || startIndex >= images.size()) {
            startIndex = 0;
        }

        this.appContext = appContext;
        this.images = images;
        this.currentIndex = startIndex;
        this.onFavoriteCallback = onFavoriteCallback;
        this.onDeleteCallback = onDeleteCallback;
        this.onAddToAlbumCallback = onAddToAlbumCallback;

        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setWidth(1000);
        dialog.setHeight(700);

        Scene scene = new Scene(createContent());
        scene.setFill(Color.TRANSPARENT);
        var themeUrl = getClass().getResource("/styles/theme.css");
        if (themeUrl != null) scene.getStylesheets().add(themeUrl.toExternalForm());
        var compUrl = getClass().getResource("/styles/components.css");
        if (compUrl != null) scene.getStylesheets().add(compUrl.toExternalForm());
        var galleryUrl = getClass().getResource("/css/gallery.css");
        if (galleryUrl != null) scene.getStylesheets().add(galleryUrl.toExternalForm());

        appContext.getThemeService().applyTheme(scene);

        dialog.setScene(scene);
    }

    private VBox createContent() {
        VBox root = new VBox();
        root.getStyleClass().add("gallery-detail-dialog");

        // ============ 主体：两栏布局 ============
        HBox mainContent = new HBox();
        mainContent.getStyleClass().add("gallery-detail-main");

        // --- 左侧：图片查看区 ---
        StackPane imageArea = new StackPane();
        imageArea.getStyleClass().add("gallery-detail-image-area");
        HBox.setHgrow(imageArea, Priority.ALWAYS);

        largeImageView = new ImageView();
        largeImageView.setPreserveRatio(true);
        largeImageView.getStyleClass().add("gallery-detail-image");
        largeImageView.setSmooth(true);

        StackPane imageContainer = new StackPane(largeImageView);
        imageContainer.setAlignment(Pos.CENTER);
        largeImageView.fitWidthProperty().bind(imageContainer.widthProperty());
        largeImageView.fitHeightProperty().bind(imageContainer.heightProperty());

        ScrollPane imageScrollPane = new ScrollPane(imageContainer);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.getStyleClass().add("gallery-detail-image-scroll");

        // 底部毛玻璃状态栏
        HBox statusBar = new HBox();
        statusBar.getStyleClass().add("gallery-detail-status-bar");

        pageLabel = new Label();
        pageLabel.getStyleClass().add("gallery-detail-status-text");

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        Label zoomLabel = new Label("100%");
        zoomLabel.getStyleClass().add("gallery-detail-status-text");

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        Button fitBtn = new Button("适应窗口");
        fitBtn.getStyleClass().add("gallery-detail-fit-btn");

        statusBar.getChildren().addAll(pageLabel, spacer1, zoomLabel, spacer2, fitBtn);
        StackPane.setAlignment(statusBar, Pos.BOTTOM_CENTER);

        imageArea.getChildren().addAll(imageScrollPane, statusBar);

        // --- 分隔线 ---
        Region separator = new Region();
        separator.getStyleClass().add("gallery-detail-separator");

        // --- 右侧：信息面板（360px）---
        VBox infoPanel = new VBox();
        infoPanel.getStyleClass().add("gallery-detail-info-panel");
        infoPanel.setPrefWidth(360);
        infoPanel.setMinWidth(360);

        // 关闭按钮（右上角）
        Button closeBtn = new Button("...");
        closeBtn.getStyleClass().add("gallery-detail-close-btn");
        closeBtn.setOnAction(e -> dialog.close());
        HBox closeBox = new HBox(closeBtn);
        closeBox.setAlignment(Pos.TOP_RIGHT);

        // 标题
        titleLabel = new Label();
        titleLabel.getStyleClass().add("gallery-detail-title");
        titleLabel.setWrapText(true);

        // 标签区域
        tagsBox = new HBox(8);
        tagsBox.getStyleClass().add("gallery-detail-tags");

        // 分隔线
        Region divider = new Region();
        divider.getStyleClass().add("gallery-detail-divider");

        // 信息行
        VBox infoRows = new VBox(12);
        infoRows.getStyleClass().add("gallery-detail-info-rows");

        HBox catRow = createInfoRow("分类");
        categoryValueLabel = (Label) catRow.getChildren().get(2);
        HBox sizeRow = createInfoRow("大小");
        sizeValueLabel = (Label) sizeRow.getChildren().get(2);
        HBox resRow = createInfoRow("分辨率");
        resolutionValueLabel = (Label) resRow.getChildren().get(2);
        HBox dateRow = createInfoRow("上传");
        dateValueLabel = (Label) dateRow.getChildren().get(2);
        HBox albumRow = createInfoRow("所属图集");
        albumValueLabel = (Label) albumRow.getChildren().get(2);

        infoRows.getChildren().addAll(catRow, sizeRow, resRow, dateRow, albumRow);

        Region infoSpacer = new Region();
        VBox.setVgrow(infoSpacer, Priority.ALWAYS);

        // 操作按钮（垂直堆叠）
        VBox actionBox = new VBox(8);
        actionBox.getStyleClass().add("gallery-detail-actions");

        favBtn = new AppButton("收藏", AppButton.Style.PRIMARY);
        favBtn.setMaxWidth(Double.MAX_VALUE);
        favBtn.setOnAction(e -> {
            GalleryImage image = images.get(currentIndex);
            image.setFavorited(!image.isFavorited());
            updateFavoriteButton();
            if (onFavoriteCallback != null) onFavoriteCallback.run();
        });

        albumBtn = new AppButton("加入图集", AppButton.Style.OUTLINE);
        albumBtn.setMaxWidth(Double.MAX_VALUE);
        albumBtn.setOnAction(e -> {
            if (onAddToAlbumCallback == null) return;
            GalleryImage currentImage = images.get(currentIndex);
            List<ImageAlbum> inAlbums = onAddToAlbumCallback.getAlbumsForImage(currentImage.getId());

            if (!inAlbums.isEmpty()) {
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
                List<ImageAlbum> allAlbums = onAddToAlbumCallback.getAlbums();
                if (allAlbums.isEmpty()) return;
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

        downloadBtn = new AppButton("下载", AppButton.Style.SECONDARY);
        downloadBtn.setMaxWidth(Double.MAX_VALUE);
        downloadBtn.setOnAction(e -> {
            // TODO: 下载功能
        });

        deleteBtn = new AppButton("删除", AppButton.Style.DANGER);
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> {
            if (onDeleteCallback != null) onDeleteCallback.run();
            dialog.close();
        });

        actionBox.getChildren().addAll(favBtn, albumBtn, downloadBtn, deleteBtn);

        infoPanel.getChildren().addAll(closeBox, titleLabel, tagsBox, divider, infoRows, infoSpacer, actionBox);
        VBox.setVgrow(infoPanel, Priority.ALWAYS);

        mainContent.getChildren().addAll(imageArea, separator, infoPanel);
        HBox.setHgrow(mainContent, Priority.ALWAYS);

        // ============ 底部导航栏 ============
        HBox navBar = new HBox(8);
        navBar.getStyleClass().add("gallery-detail-nav-bar");

        prevBtn = new AppButton("\u2039 上一张", AppButton.Style.GHOST);
        prevBtn.setOnAction(e -> navigate(-1));

        nextBtn = new AppButton("下一张 \u203A", AppButton.Style.GHOST);
        nextBtn.setOnAction(e -> navigate(1));

        navBar.getChildren().addAll(prevBtn, nextBtn);

        root.getChildren().addAll(mainContent, navBar);
        updateContent();
        return root;
    }

    private HBox createInfoRow(String labelText) {
        HBox row = new HBox();
        row.getStyleClass().add("gallery-detail-info-row");
        row.setAlignment(Pos.CENTER);

        Label label = new Label(labelText);
        label.getStyleClass().add("gallery-detail-info-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label value = new Label();
        value.getStyleClass().add("gallery-detail-info-value");

        row.getChildren().addAll(label, spacer, value);
        return row;
    }

    private Label createTag(String text) {
        Label tag = new Label(text);
        tag.getStyleClass().add("gallery-detail-tag");
        return tag;
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
        categoryValueLabel.setText(image.getCategory() != null ? image.getCategory() : "其他");
        sizeValueLabel.setText(image.getDisplayFileSize());
        resolutionValueLabel.setText(image.getDisplayResolution());
        dateValueLabel.setText(image.getCreatedAt() != null ? image.getCreatedAt() : "未知");
        albumValueLabel.setText("未分组");

        pageLabel.setText((currentIndex + 1) + " / " + images.size());

        tagsBox.getChildren().clear();
        if (image.getCategory() != null && !image.getCategory().isBlank()) {
            tagsBox.getChildren().add(createTag(image.getCategory()));
        }
        if (image.getSubCategory() != null && !image.getSubCategory().isBlank()) {
            tagsBox.getChildren().add(createTag(image.getSubCategory()));
        }

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
