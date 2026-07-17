package com.qiyunge.ui.music;

import com.qiyunge.ui.components.ItemDialog;

/**
 * 新建/编辑曲笺对话框。
 */
public class PlaylistDialog extends ItemDialog {

    public PlaylistDialog(String title, String defaultName) {
        super(title, "曲笺名称", "输入曲笺名称", defaultName, "创建");
    }

    public String getPlaylistName() { return getItemName(); }
    public String getPlaylistDescription() { return getItemDescription(); }
}
