package com.qiyunge.ui.gallery;

import com.qiyunge.ui.components.ItemDialog;

/**
 * 新建/编辑图集对话框。
 */
public class AlbumDialog extends ItemDialog {

    public AlbumDialog(String title, String defaultName) {
        super(title, "图集名称", "输入图集名称", defaultName, "创建");
    }

    public String getAlbumName() { return getItemName(); }
    public String getAlbumDescription() { return getItemDescription(); }
}
