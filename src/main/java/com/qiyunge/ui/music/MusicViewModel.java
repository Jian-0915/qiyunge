package com.qiyunge.ui.music;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.MusicPlayerService;
import com.qiyunge.application.service.MusicProvider;
import com.qiyunge.application.service.MusicService;
import com.qiyunge.application.service.OnlineMusicService;
import com.qiyunge.application.service.PlaylistService;
import com.qiyunge.domain.entity.PlayHistory;
import com.qiyunge.domain.entity.Playlist;
import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.util.LyricParser;
import com.qiyunge.infrastructure.util.LyricParser.LyricLine;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MusicViewModel {

    private final AppContext appContext;
    private final MusicService musicService;
    private final MusicPlayerService playerService;
    private final PlaylistService playlistService;
    private final com.qiyunge.application.service.AsyncExecutor asyncExecutor;

    private final ObservableList<Song> songs = FXCollections.observableArrayList();
    private final FilteredList<Song> filteredSongs;
    private SortedList<Song> sortedSongs;

    // 曲笺和余音列表
    private final ObservableList<Playlist> playlists = FXCollections.observableArrayList();
    private final ObservableList<PlayHistory> playHistoryList = FXCollections.observableArrayList();
    private final ObservableList<Song> favoriteSongs = FXCollections.observableArrayList();
    private final ObservableList<Song> playlistSongs = FXCollections.observableArrayList();

    private final StringProperty keyword = new SimpleStringProperty("");
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final IntegerProperty songCount = new SimpleIntegerProperty(0);

    // ===== 在线音乐 =====
    private final OnlineMusicService onlineMusicService;
    private final ObservableList<Song> onlineSongs = FXCollections.observableArrayList();
    private final StringProperty searchMode = new SimpleStringProperty("local"); // "local" | "online"
    private final StringProperty currentFilterProvider = new SimpleStringProperty(null); // null = 全部平台

    private Runnable onDataChanged;

    public MusicViewModel(AppContext appContext) {
        this.appContext = appContext;
        this.musicService = appContext.getMusicService();
        this.playerService = appContext.getMusicPlayerService();
        this.playlistService = appContext.getPlaylistService();
        this.asyncExecutor = appContext.getAsyncExecutor();
        this.onlineMusicService = appContext.getOnlineMusicService();
        this.filteredSongs = new FilteredList<>(songs, s -> true);
        this.sortedSongs = new SortedList<>(filteredSongs);

        keyword.addListener((obs, old, val) -> applyFilter());

        // 播放成功后记录历史 + 刷新余音列表
        playerService.addOnPlaybackReady(song -> {
            int userId = appContext.getUserSession().getUserId();
            musicService.recordPlay(userId, song.getId());
            loadPlayHistory();
        });

        // 歌曲切换时自动加载封面
        playerService.currentSongProperty().addListener((obs, old, song) -> {
            if (song != null) {
                loadCover(song);
            } else {
                coverImage.set(null);
            }
        });

        loadSongs();
    }

    public void setOnDataChanged(Runnable callback) { this.onDataChanged = callback; }

    // ===== 歌曲列表 =====

    public void loadSongs() {
        loading.set(true);
        asyncExecutor.execute(() -> {
            try {
                List<Song> list = musicService.findAllSongs();
                // 按添加时间降序排列
                list.sort((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                int userId = appContext.getUserSession().getUserId();
                musicService.markFavoriteStatus(userId, list);
                Platform.runLater(() -> {
                    songs.setAll(list);
                    songCount.set(list.size());
                    applyFilter();
                    loading.set(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    errorMessage.set("加载歌曲失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    public void search(String keyword) {
        this.keyword.set(keyword != null ? keyword.trim() : "");
    }

    public ObservableList<Song> getOnlineSongs() { return onlineSongs; }
    public StringProperty searchModeProperty() { return searchMode; }
    public String getSearchMode() { return searchMode.get(); }
    public void setSearchMode(String mode) { searchMode.set(mode); }

    /** 平台筛选属性（null = 全部平台） */
    public StringProperty currentFilterProviderProperty() { return currentFilterProvider; }
    public String getCurrentFilterProvider() { return currentFilterProvider.get(); }
    public void setCurrentFilterProvider(String providerId) { currentFilterProvider.set(providerId); }

    /** 获取所有在线提供者列表（用于 UI 筛选，排除本地） */
    public List<MusicProvider> getOnlineProviders() {
        return onlineMusicService.getRegistry().getAllProvidersSorted().stream()
            .filter(p -> !"local".equals(p.getProviderId()))
            .toList();
    }

    /** 将歌曲加入播放队列 */
    public void addToQueue(Song song) {
        playerService.getQueue().add(song);
    }

    /** 在线歌曲试听：使用在线结果列表作为播放队列。 */
    public void playOnlineSong(Song song) {
        if (song == null) return;
        List<Song> queue = new ArrayList<>(onlineSongs);
        int index = queue.indexOf(song);
        if (index < 0) {
            queue = Collections.singletonList(song);
            index = 0;
        }
        playerService.playQueue(queue, index);
        if (onDataChanged != null) onDataChanged.run();
    }

    /** 在线歌曲加入队列。 */
    public void addOnlineToQueue(Song song) {
        if (song == null) return;
        playerService.getQueue().add(song);
        errorMessage.set("已加入播放队列：" + song.getDisplayTitle());
    }

    /**
     * 收藏在线歌曲：先入库（去重），再添加收藏关系。
     */
    public void favoriteOnlineSong(Song song) {
        if (song == null) return;
        int userId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                musicService.favoriteOnlineSong(userId, song);
                Platform.runLater(() -> {
                    song.setFavorited(true);
                    errorMessage.set("已加入藏音：" + song.getDisplayTitle());
                    // 刷新藏音列表
                    loadFavoriteSongs();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    appContext.getDialogService().showError("收藏失败", "无法收藏该歌曲: " + e.getMessage());
                });
            }
        });
    }

    /**
     * 下载在线歌曲到本地曲库并收藏。
     */
    public void downloadSong(Song song) {
        if (song == null) return;
        int userId = appContext.getUserSession().getUserId();
        errorMessage.set("正在下载：" + song.getDisplayTitle());
        asyncExecutor.execute(() -> {
            try {
                musicService.downloadAndFavorite(userId, song);
                Platform.runLater(() -> {
                    song.setFavorited(true);
                    errorMessage.set("已下载到本地曲库：" + song.getDisplayTitle());
                    // 刷新本地歌曲列表和藏音列表
                    loadSongs();
                    loadFavoriteSongs();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    appContext.getDialogService().showError("下载失败", "无法下载该歌曲: " + e.getMessage());
                    errorMessage.set("");
                });
            }
        });
    }

    /** 在线搜索（支持平台筛选） */
    public void searchOnline(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;
        loading.set(true);
        errorMessage.set("");
        asyncExecutor.execute(() -> {
            try {
                List<Song> results = onlineMusicService.search(keyword.trim(), currentFilterProvider.get());
                System.out.println("[ViewModel] 在线搜索结果: " + results.size() + " 首");
                Platform.runLater(() -> {
                    onlineSongs.setAll(results);
                    loading.set(false);
                    if (results.isEmpty()) {
                        errorMessage.set("未找到相关歌曲，换个关键词试试");
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    errorMessage.set("在线搜索失败: " + e.getMessage());
                    loading.set(false);
                });
            }
        });
    }

    private void applyFilter() {
        String kw = keyword.get().toLowerCase();
        filteredSongs.setPredicate(song ->
            kw.isEmpty() || song.getTitle().toLowerCase().contains(kw)
                || (song.getArtist() != null && song.getArtist().toLowerCase().contains(kw))
        );
    }

    // TODO: FileChooser 应在 View 层创建，ViewModel 不应依赖 JavaFX 控件。
    //       后续重构时应将文件选择逻辑移至 View 层，此方法改为接收已选中的文件列表。
    public void importLocalFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("导入本地音乐");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("音频文件", "*.mp3", "*.m4a", "*.wav", "*.flac"),
            new FileChooser.ExtensionFilter("MP3", "*.mp3"),
            new FileChooser.ExtensionFilter("所有文件", "*.*")
        );
        List<File> files = chooser.showOpenMultipleDialog(appContext.getPrimaryStage());
        if (files == null || files.isEmpty()) return;
        asyncExecutor.execute(() -> {
            try {
                for (File file : files) musicService.importLocalFile(file);
                loadSongs();
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("导入失败: " + e.getMessage()));
            }
        });
    }

    public void playSong(Song song) {
        List<Song> allSongs = new ArrayList<>(songs);
        int index = allSongs.indexOf(song);
        if (index < 0) {
            playerService.playQueue(Collections.singletonList(song), 0);
            if (onDataChanged != null) onDataChanged.run();
            return;
        }
        playerService.playQueue(allSongs, Math.max(0, index));
        if (onDataChanged != null) onDataChanged.run();
    }

    public void toggleFavorite(Song song) {
        int userId = appContext.getUserSession().getUserId();
        if (song.isFavorited()) {
            musicService.removeFavorite(userId, song.getId());
            song.setFavorited(false);
        } else {
            musicService.addFavorite(userId, song.getId());
            song.setFavorited(true);
        }
        // 同步左侧表格中同一首歌的收藏状态
        syncFavoriteStatus(song.getId(), song.isFavorited());
        // 刷新收藏列表
        loadFavoriteSongs();
        // 通知表格重新渲染
        refreshTable();
    }

    /** 将收藏状态同步到左侧歌曲列表中相同 ID 的 Song 对象 */
    private void syncFavoriteStatus(int songId, boolean favorited) {
        for (Song s : songs) {
            if (s.getId() == songId) {
                s.setFavorited(favorited);
                break;
            }
        }
    }

    /** 触发表格刷新，使 Cell 重新渲染（如收藏状态变化） */
    public void refreshTable() {
        // 通过替换列表内容触发 TableView 重新渲染所有 Cell
        List<Song> copy = new ArrayList<>(songs);
        songs.setAll(copy);
    }

    public void deleteSong(Song song) {
        musicService.deleteSong(song.getId());
        songs.remove(song);
        songCount.set(songs.size());
    }

    // ===== 批量操作 =====

    public void batchAddToQueue(List<Song> songs) {
        if (songs == null || songs.isEmpty()) return;
        playerService.getQueue().addAll(songs);
    }

    public void batchFavorite(List<Song> songs) {
        int userId = appContext.getUserSession().getUserId();
        for (Song s : songs) {
            if (!s.isFavorited()) {
                musicService.addFavorite(userId, s.getId());
                s.setFavorited(true);
            }
        }
        loadFavoriteSongs();
    }

    public void batchDelete(List<Song> songs) {
        for (Song s : songs) musicService.deleteSong(s.getId());
        loadSongs();
    }

    // ===== 藏音 =====

    public void loadFavoriteSongs() {
        int userId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                java.util.Set<Integer> favIds = musicService.getFavoriteSongIds(userId);
                List<Song> allSongs = musicService.findAllSongs();
                List<Song> favList = new ArrayList<>();
                for (Song s : allSongs) {
                    if (favIds.contains(s.getId())) {
                        s.setFavorited(true); // 标记为已收藏，确保 toggleFavorite 能正确判断
                        favList.add(s);
                    }
                }
                Platform.runLater(() -> favoriteSongs.setAll(favList));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("加载藏音失败: " + e.getMessage()));
            }
        });
    }

    public void playFavoriteSongs() {
        if (favoriteSongs.isEmpty()) return;
        playerService.playQueue(new ArrayList<>(favoriteSongs), 0);
    }

    // ===== 曲笺 =====

    public void loadPlaylists() {
        int userId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                List<Playlist> list = playlistService.getUserPlaylists(userId);
                Platform.runLater(() -> playlists.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("加载曲笺失败: " + e.getMessage()));
            }
        });
    }

    public void createPlaylist(String name, String description) {
        int userId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                playlistService.createPlaylist(userId, name, description);
                loadPlaylists();
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("创建曲笺失败: " + e.getMessage()));
            }
        });
    }

    public void addToPlaylist(int playlistId, Song song) {
        asyncExecutor.execute(() -> {
            try {
                playlistService.addSongToPlaylist(playlistId, song.getId());
                loadPlaylists();
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("添加到曲笺失败: " + e.getMessage()));
            }
        });
    }

    public void showAddToPlaylistDialog(Song song) {
        List<Playlist> currentPlaylists = new ArrayList<>(playlists);
        if (currentPlaylists.isEmpty()) {
            errorMessage.set("还没有曲笺，请先创建一个");
            return;
        }
        ChoiceDialog<Playlist> dialog = new ChoiceDialog<>(currentPlaylists.get(0), currentPlaylists);
        dialog.setTitle("加入曲笺");
        dialog.setHeaderText("选择要添加到的曲笺：");
        dialog.setContentText("曲笺：");
        Optional<Playlist> result = dialog.showAndWait();
        result.ifPresent(playlist -> addToPlaylist(playlist.getId(), song));
    }

    public void loadPlaylistSongs(int playlistId) {
        asyncExecutor.execute(() -> {
            try {
                List<Song> songs = playlistService.getPlaylistSongs(playlistId);
                Platform.runLater(() -> playlistSongs.setAll(songs));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("加载曲笺歌曲失败: " + e.getMessage()));
            }
        });
    }

    public void playPlaylist(int playlistId) {
        asyncExecutor.execute(() -> {
            try {
                List<Song> playlistSongs = playlistService.getPlaylistSongs(playlistId);
                if (!playlistSongs.isEmpty()) {
                    Platform.runLater(() -> {
                        playerService.playQueue(playlistSongs, 0);
                        if (onDataChanged != null) onDataChanged.run();
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("播放曲笺失败: " + e.getMessage()));
            }
        });
    }

    public void deletePlaylist(int playlistId) {
        asyncExecutor.execute(() -> {
            try {
                playlistService.deletePlaylist(playlistId);
                loadPlaylists();
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("删除曲笺失败: " + e.getMessage()));
            }
        });
    }

    /** 从曲笺中移除歌曲 */
    public void removeSongFromPlaylist(int playlistId, Song song) {
        asyncExecutor.execute(() -> {
            try {
                playlistService.removeSongFromPlaylist(playlistId, song.getId());
                loadPlaylistSongs(playlistId);
                loadPlaylists(); // 刷新歌曲计数
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("移除歌曲失败: " + e.getMessage()));
            }
        });
    }

    // ===== 余音 =====

    /** 清除所有播放历史 */
    public void clearPlayHistory() {
        int userId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                musicService.clearPlayHistory(userId);
                Platform.runLater(() -> playHistoryList.clear());
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("清除余音失败: " + e.getMessage()));
            }
        });
    }

    public void loadPlayHistory() {
        int userId = appContext.getUserSession().getUserId();
        asyncExecutor.execute(() -> {
            try {
                List<PlayHistory> list = musicService.getPlayHistory(userId, 50);
                Platform.runLater(() -> playHistoryList.setAll(list));
            } catch (Exception e) {
                Platform.runLater(() -> errorMessage.set("加载余音失败: " + e.getMessage()));
            }
        });
    }

    public void playHistorySong(PlayHistory history) {
        // 先在已加载的歌曲中查找
        for (Song s : songs) {
            if (s.getId() == history.getSongId()) {
                playSong(s);
                return;
            }
        }
        // 未找到则从数据库加载
        asyncExecutor.execute(() -> {
            Song song = musicService.findSongById(history.getSongId());
            if (song != null) {
                Platform.runLater(() -> {
                    playerService.playQueue(Collections.singletonList(song), 0);
                    if (onDataChanged != null) onDataChanged.run();
                });
            }
        });
    }

    // ===== 歌词 =====
    private final ObservableList<LyricLine> currentLyrics = FXCollections.observableArrayList();

    public ObservableList<LyricLine> getCurrentLyrics() { return currentLyrics; }

    /** 加载当前歌曲的歌词 */
    public void loadLyrics(Song song) {
        asyncExecutor.execute(() -> {
            try {
                Path audioPath = toLocalPath(song.getUrl());
                Path lrcPath = audioPath != null ? LyricParser.findLyricFile(audioPath) : null;
                if (lrcPath != null) {
                    List<LyricLine> lines = LyricParser.parse(lrcPath);
                    Platform.runLater(() -> currentLyrics.setAll(lines));
                } else {
                    Platform.runLater(() -> currentLyrics.clear());
                }
            } catch (Exception e) {
                Platform.runLater(() -> currentLyrics.clear());
            }
        });
    }

    /** 清空歌词 */
    public void clearLyrics() {
        currentLyrics.clear();
    }

    // ===== Getters =====

    // ===== 封面 =====

    private final ObjectProperty<Image> coverImage = new SimpleObjectProperty<>();

    public ObjectProperty<Image> coverImageProperty() { return coverImage; }

    /**
     * 异步加载歌曲的专辑封面。
     * 从音频文件中提取嵌入的封面图片，更新到 coverImage 属性。
     */
    public void loadCover(Song song) {
        asyncExecutor.execute(() -> {
            try {
                Path audioPath = toLocalPath(song.getUrl());
                Path coverPath = audioPath != null ? com.qiyunge.infrastructure.util.AlbumCoverExtractor.extractCover(audioPath) : null;
                if (coverPath != null && Files.exists(coverPath)) {
                    Image img = new Image(coverPath.toUri().toString());
                    Platform.runLater(() -> coverImage.set(img));
                } else {
                    Platform.runLater(() -> coverImage.set(null));
                }
            } catch (Exception e) {
                Platform.runLater(() -> coverImage.set(null));
            }
        });
    }

    /** 将 file:// URL 转换为本地文件路径 */
    private Path toLocalPath(String url) {
        if (url == null || !url.toLowerCase().startsWith("file:")) return null;
        try {
            URI uri = URI.create(url);
            String decodedPath = URLDecoder.decode(uri.getSchemeSpecificPart(), StandardCharsets.UTF_8);
            while (decodedPath.startsWith("/")) decodedPath = decodedPath.substring(1);
            Path path = Path.of(decodedPath);
            if (Files.exists(path)) return path;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    // ===== 播放器代理属性（供 View 绑定，不直接暴露 MusicPlayerService） =====

    /** 播放队列（只读视图） */
    public ObservableList<Song> getQueue() { return playerService.getQueue(); }

    /** 当前歌曲属性 */
    public ObjectProperty<Song> currentSongProperty() { return playerService.currentSongProperty(); }
    public Song getCurrentSong() { return playerService.getCurrentSong(); }

    /** 播放状态 */
    public BooleanProperty playingProperty() { return playerService.playingProperty(); }

    /** 进度 */
    public DoubleProperty progressProperty() { return playerService.progressProperty(); }

    /** 当前时间文本 */
    public StringProperty currentTimeTextProperty() { return playerService.currentTimeTextProperty(); }

    /** 总时间文本 */
    public StringProperty totalTimeTextProperty() { return playerService.totalTimeTextProperty(); }

    /** 音量 */
    public DoubleProperty volumeProperty() { return playerService.volumeProperty(); }

    /** 播放器错误信息 */
    public StringProperty playerErrorMessageProperty() { return playerService.errorMessageProperty(); }

    /** 播放模式 */
    public ObjectProperty<MusicPlayerService.PlayMode> playModeProperty() { return playerService.playModeProperty(); }
    public MusicPlayerService.PlayMode getPlayMode() { return playerService.getPlayMode(); }

    /** 播放器操作方法 */
    public void togglePause() { playerService.togglePause(); }
    public void playNext() { playerService.playNext(); }
    public void playPrevious() { playerService.playPrevious(); }
    public void cyclePlayMode() { playerService.cyclePlayMode(); }
    public void seek(double ratio) { playerService.seek(ratio); }
    public void setVolume(double vol) { playerService.setVolume(vol); }
    public void playAtIndex(int index) { playerService.playAtIndex(index); }
    public void removeFromQueue(Song song) { playerService.getQueue().remove(song); }

    // ===== Getters =====

    public SortedList<Song> getFilteredSongs() { return sortedSongs; }
    public StringProperty keywordProperty() { return keyword; }
    public BooleanProperty loadingProperty() { return loading; }
    public StringProperty errorMessageProperty() { return errorMessage; }
    public IntegerProperty songCountProperty() { return songCount; }
    public ObservableList<Playlist> getPlaylists() { return playlists; }
    public ObservableList<PlayHistory> getPlayHistoryList() { return playHistoryList; }
    public ObservableList<Song> getFavoriteSongs() { return favoriteSongs; }
    public ObservableList<Song> getPlaylistSongs() { return playlistSongs; }
}
