package com.qiyunge.ui.gallery;

import com.qiyunge.app.AppContext;
import com.qiyunge.ui.components.ModuleHeader;

/**
 * 图库模块顶部标题栏：复用通用 ModuleHeader。
 */
public class GalleryHeader extends ModuleHeader {

    public GalleryHeader(AppContext appContext) {
        super("✨", "拾光廊", "浏览、收藏和管理你的图片");
    }
}
