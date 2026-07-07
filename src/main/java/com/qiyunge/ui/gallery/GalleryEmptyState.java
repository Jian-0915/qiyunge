package com.qiyunge.ui.gallery;

import com.qiyunge.ui.components.AppButton;
import com.qiyunge.ui.components.EmptyState;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

/**
 * 图库模块空状态组件。
 * 提供多种诗意化空状态 + 上传按钮扩展。
 */
public class GalleryEmptyState extends EmptyState {

    private Runnable onUpload;
    private Runnable onExplore;

    /** 光影墙为空状态：廊中无影。 */
    public static GalleryEmptyState allImagesEmpty() {
        return new GalleryEmptyState("✨", "廊中无影", "还没有收藏任何图片\n上传或采风，拾光廊就会开始有光影", true);
    }

    /** 风物卷为空状态：卷中无物。 */
    public static GalleryEmptyState categoriesEmpty() {
        return new GalleryEmptyState("◆", "卷中无物", "该分类下暂无图片\n试试其他分类，或上传图片到此分类");
    }

    /** 择景为空状态：尚未择景。 */
    public static GalleryEmptyState favoritesEmpty() {
        return new GalleryEmptyState("♥", "尚未择景", "还没有收藏任何图片\n点击图片上的收藏按钮，将喜欢的光影珍藏于此");
    }

    /** 流光为空状态：流光未至。 */
    public static GalleryEmptyState recentEmpty() {
        return new GalleryEmptyState("⏳", "流光未至", "还没有最近浏览的图片\n浏览过的图片会在这里留下足迹");
    }

    /** 搜索无结果状态：未寻得。 */
    public static GalleryEmptyState noSearchResults() {
        return new GalleryEmptyState("🔍", "未寻得", "没有找到匹配的图片\n换个关键词试试");
    }

    public void setOnUpload(Runnable onUpload) { this.onUpload = onUpload; }
    public void setOnExplore(Runnable onExplore) { this.onExplore = onExplore; }

    private GalleryEmptyState(String icon, String title, String message) {
        super(icon, title, message, true);
    }

    private GalleryEmptyState(String icon, String title, String message, boolean showUploadButton) {
        super(icon, title, message, true);
        if (showUploadButton) {
            HBox btnRow = new HBox(12);
            btnRow.setAlignment(Pos.CENTER);

            AppButton uploadBtn = new AppButton("上传图片", AppButton.Style.PRIMARY);
            uploadBtn.setOnAction(e -> { if (onUpload != null) onUpload.run(); });

            AppButton exploreBtn = new AppButton("探索在线图片", AppButton.Style.OUTLINE);
            exploreBtn.setOnAction(e -> { if (onExplore != null) onExplore.run(); });

            btnRow.getChildren().addAll(uploadBtn, exploreBtn);
            this.getChildren().add(btnRow);
        }
    }
}
