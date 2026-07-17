package com.qiyunge.ui.entertainment;

import com.qiyunge.app.AppContext;
import com.qiyunge.ui.components.ModuleHeader;

/**
 * 百趣园模块顶部标题栏：复用通用 ModuleHeader。
 */
public class EntertainmentHeader extends ModuleHeader {

    public EntertainmentHeader(AppContext appContext) {
        super("\u2666", "闲云馆", "放松一下，享受乐趣");
    }
}
