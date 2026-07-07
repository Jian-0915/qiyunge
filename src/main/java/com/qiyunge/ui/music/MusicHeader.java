package com.qiyunge.ui.music;

import com.qiyunge.app.AppContext;
import com.qiyunge.ui.components.ModuleHeader;

/**
 * 音乐模块顶部标题栏：复用通用 ModuleHeader。
 */
public class MusicHeader extends ModuleHeader {

    public MusicHeader(AppContext appContext, MusicViewModel viewModel) {
        super("♫", "听雨轩", "搜索、播放、收藏你喜欢的音乐");
    }
}
