package com.qiyunge.ui.music;

/**
 * 音乐模块空状态组件。
 * 提供多种诗意化空状态的工厂方法。
 */
public class EmptyState extends com.qiyunge.ui.components.EmptyState {

    /** 本地音乐为空状态：轩中空寂。 */
    public static EmptyState localEmpty() {
        return new EmptyState("♫", "轩中空寂", "还没有音乐\n导入几首歌，听雨轩就会开始有声音");
    }

    /** 在线音乐为空状态：云海茫茫。 */
    public static EmptyState onlineEmpty() {
        return new EmptyState("☁", "云海茫茫", "输入关键词搜索在线音乐\nJamendo 海量音乐等你发现");
    }

    /** 搜索无结果状态：未寻得。 */
    public static EmptyState noSearchResults() {
        return new EmptyState("🔍", "未寻得", "没有找到匹配的歌曲\n换个关键词试试");
    }

    /** 藏音为空状态。 */
    public static EmptyState favoritesEmpty() {
        return new EmptyState("♥", "藏音空空", "还没有收藏任何歌曲\n点击歌曲的收藏按钮，将喜欢的音乐珍藏于此");
    }

    /** 余音为空状态。 */
    public static EmptyState historyEmpty() {
        return new EmptyState("⏳", "余音未起", "还没有播放记录\n播放过的歌曲会在这里留下足迹");
    }

    /** 曲笺为空状态。 */
    public static EmptyState playlistsEmpty() {
        return new EmptyState("♪", "曲笺未书", "还没有创建任何歌单\n点击下方按钮创建你的第一个曲笺");
    }

    private EmptyState(String icon, String title, String message) {
        super(icon, title, message, true);
    }
}
