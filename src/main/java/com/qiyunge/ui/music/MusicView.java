package com.qiyunge.ui.music;

import com.qiyunge.app.AppContext;
import com.qiyunge.application.service.MusicProvider;
import com.qiyunge.domain.entity.PlayHistory;
import com.qiyunge.domain.entity.Playlist;
import com.qiyunge.domain.entity.Song;
import com.qiyunge.ui.components.AppButton;
import com.qiyunge.ui.components.LyricPane;
import com.qiyunge.ui.components.MusicBar;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.Node;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 音乐模块主视图。
 * 采用三栏布局：顶部标题栏、左侧边导航、主内容区、底部浮声栏。
 */
public class MusicView extends BorderPane {

    private final AppContext appContext;
    private final MusicViewModel viewModel;
    private final VBox contentArea;
    private final ScrollPane contentScrollPane;

    // 播放队列面板
    private final ListView<Song> queueListView;
    private final Label queueEmpty;

    // 歌词面板
    private final LyricPane lyricPane = new LyricPane();
    private final StackPane lyricPanelWrapper = new StackPane();
    private boolean lyricPanelVisible = false;

    public MusicView(AppContext appContext, MusicBar musicBar) {
        this.appContext = appContext;
        this.viewModel = new MusicViewModel(appContext);
        this.getStyleClass().add("music-view");

        // ===== Top: 顶部标题栏 =====
        MusicHeader header = new MusicHeader(appContext, viewModel);
        setTop(header);

        // ===== Left: 侧边导航栏 =====
        MusicSidebar sidebar = new MusicSidebar(appContext, this::navigateTo);
        setLeft(sidebar);

        // ===== Center: 主内容区 =====
        contentArea = new VBox(8);
        contentArea.setPadding(new Insets(16));
        contentArea.setStyle("-fx-background-color: -bg-primary;");
        VBox.setVgrow(contentArea, Priority.ALWAYS);

        contentScrollPane = new ScrollPane(contentArea);
        contentScrollPane.setFitToWidth(true);
        contentScrollPane.setFitToHeight(true);
        contentScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        contentScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        contentScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        contentScrollPane.getStyleClass().add("page-scroll");

        // 歌词面板
        lyricPane.setPadding(new Insets(0));
        lyricPane.setStyle("-fx-background-color: -bg-primary; -fx-border-color: -border-light; -fx-border-width: 0 0 0 1;");
        lyricPane.setMinWidth(320);
        lyricPane.setPrefWidth(360);
        lyricPane.setMaxWidth(420);
        lyricPanelWrapper.getChildren().add(lyricPane);
        lyricPanelWrapper.setVisible(false);
        lyricPanelWrapper.setManaged(false);

        // 主内容 + 歌词面板 水平布局
        HBox mainContent = new HBox(0);
        HBox.setHgrow(contentScrollPane, Priority.ALWAYS);
        mainContent.getChildren().addAll(contentScrollPane, lyricPanelWrapper);
        setCenter(mainContent);



        // 歌词面板同步
        if (musicBar != null) {
            // 监听切歌：加载歌词 + 更新歌曲信息
            appContext.getMusicPlayerService().currentSongProperty().addListener((obs, oldSong, newSong) -> {
                if (newSong != null) {
                    lyricPane.setSongInfo(newSong);
                    lyricPane.showLoading();
                    lyricPane.clear();
                    appContext.getLyricService().getLyricsAsync(newSong, lines -> {
                        lyricPane.setLyrics(lines);
                    });
                } else {
                    lyricPane.setSongInfo(null);
                    lyricPane.clear();
                }
            });

            // 监听播放进度：高亮歌词 + 更新进度条
            appContext.getMusicPlayerService().currentTimeMsProperty().addListener((obs, old, newMs) -> {
                double ms = newMs.doubleValue();
                lyricPane.updateProgress(ms);
                // 同步进度条
                var player = appContext.getMusicPlayerService();
                double currentSec = ms / 1000.0;
                double totalSec = 0;
                var song = player.getCurrentSong();
                if (song != null) {
                    totalSec = song.getDuration();
                }
                lyricPane.updateProgressbar(currentSec, totalSec);
            });

            // 歌词面板进度拖动回调
            lyricPane.setSeekCallback(progress -> {
                var player = appContext.getMusicPlayerService();
                player.seek(progress);
            });
        }

        // 初始化播放队列面板（在全部歌曲页面内显示）
        queueListView = new ListView<>();
        queueListView.getStyleClass().add("queue-list");
        queueListView.setCellFactory(list -> new QueueListCell());
        queueListView.setItems(viewModel.getQueue());

        queueEmpty = new Label("点击歌曲开始播放");
        queueEmpty.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary; -fx-padding: 20 0;");
        queueListView.setPlaceholder(queueEmpty);

        viewModel.currentSongProperty().addListener((obs, old, song) -> queueListView.refresh());

        // 异步加载数据
        viewModel.loadPlaylists();
        viewModel.loadFavoriteSongs();
        viewModel.loadPlayHistory();

        // 默认显示全部歌曲
        navigateTo("allSongs");
    }

    /**
     * 根据导航键切换主内容区视图。
     */
    private void navigateTo(String page) {
        contentArea.getChildren().clear();
        switch (page) {
            case "allSongs" -> showAllSongs();
            case "online" -> showOnlineSearch();
            case "favorites" -> showFavorites();
            case "history" -> showHistory();
            case "playlists" -> showPlaylists();
            case "import" -> {
                viewModel.importLocalFiles();
                navigateTo("allSongs");
            }
            case "settings" -> showSettings();
            default -> showAllSongs();
        }
        if (!contentArea.getChildren().isEmpty()) {
            Node first = contentArea.getChildren().get(0);
            VBox.setVgrow(first, Priority.ALWAYS);
        }
    }

    /** 切换歌词面板显示/隐藏 */
    public void toggleLyricPanel() {
        lyricPanelVisible = !lyricPanelVisible;
        lyricPanelWrapper.setVisible(lyricPanelVisible);
        lyricPanelWrapper.setManaged(lyricPanelVisible);
    }

    // ==================== 全部歌曲视图 ====================

    private void showAllSongs() {
        VBox page = new VBox(8);

        // 工具栏：搜索 + 导入
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("寻音...");
        searchField.getStyleClass().add("search-box");
        searchField.setPrefWidth(280);
        searchField.setOnAction(e -> viewModel.search(searchField.getText()));
        searchField.textProperty().addListener((obs, old, val) -> viewModel.search(val));

        AppButton importBtn = new AppButton("导入本地音乐", AppButton.Style.SECONDARY);
        importBtn.setOnAction(e -> viewModel.importLocalFiles());

        toolbar.getChildren().addAll(searchField, importBtn);

        // 歌曲表格
        TableView<Song> table = createSongTable(viewModel.getFilteredSongs());
        VBox.setVgrow(table, Priority.ALWAYS);

        // 批量操作栏
        HBox batchBar = createBatchBar(table);

        // 播放队列面板（在全部歌曲页面内显示）
        TitledPane queuePane = new TitledPane("流音台", queueListView);
        queuePane.setCollapsible(true);
        queuePane.setExpanded(false);
        queuePane.setPrefHeight(200);
        queuePane.setMaxHeight(200);

        // 错误提示栏
        HBox errorBar = createErrorBar();

        page.getChildren().addAll(toolbar, batchBar, table, errorBar, queuePane);
        contentArea.getChildren().add(page);
    }

    // ==================== 在线寻音视图 ====================

    private void showOnlineSearch() {
        VBox page = new VBox(8);

        // 工具栏：搜索 + 搜索按钮
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("搜索在线音乐...");
        searchField.getStyleClass().add("search-box");
        searchField.setPrefWidth(280);
        searchField.setOnAction(e -> viewModel.searchOnline(searchField.getText()));

        AppButton searchBtn = new AppButton("搜索", AppButton.Style.PRIMARY);
        searchBtn.setOnAction(e -> viewModel.searchOnline(searchField.getText()));

        toolbar.getChildren().addAll(searchField, searchBtn);

        // 平台筛选 Tab
        HBox providerFilter = new HBox(6);
        providerFilter.setAlignment(Pos.CENTER_LEFT);
        ToggleButton allBtn = new ToggleButton("全部");
        allBtn.setSelected(true);
        allBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        providerFilter.getChildren().add(allBtn);

        List<MusicProvider> providers = viewModel.getOnlineProviders();
        for (MusicProvider p : providers) {
            ToggleButton btn = new ToggleButton(p.getProviderName());
            btn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
            btn.setUserData(p.getProviderId());
            btn.setOnAction(e -> {
                // 取消其他选中
                providerFilter.getChildren().forEach(n -> {
                    if (n instanceof ToggleButton tb && tb != btn) tb.setSelected(false);
                });
                viewModel.setCurrentFilterProvider(btn.isSelected() ? p.getProviderId() : null);
            });
            providerFilter.getChildren().add(btn);
        }

        // "全部"按钮点击
        allBtn.setOnAction(e -> {
            providerFilter.getChildren().forEach(n -> {
                if (n instanceof ToggleButton tb && tb != allBtn) tb.setSelected(false);
            });
            viewModel.setCurrentFilterProvider(null);
        });

        // 加载提示
        Label loadingLabel = new Label("正在搜索...");
        loadingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -primary;");
        loadingLabel.setVisible(false);
        loadingLabel.setManaged(false);

        // 在线歌曲表格（带来源列）
        TableView<Song> table = createOnlineSongTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        // 空状态
        table.setPlaceholder(EmptyState.onlineEmpty());

        // 监听搜索结果变化
        viewModel.getOnlineSongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            if (viewModel.getOnlineSongs().isEmpty()) {
                table.setPlaceholder(EmptyState.noSearchResults());
            }
        });

        // 监听加载状态
        viewModel.loadingProperty().addListener((obs, old, val) -> {
            loadingLabel.setVisible(val);
            loadingLabel.setManaged(val);
        });

        // 批量操作栏
        HBox batchBar = createBatchBar(table);

        // 错误提示栏
        HBox errorBar = createErrorBar();

        page.getChildren().addAll(toolbar, providerFilter, loadingLabel, batchBar, table, errorBar);
        contentArea.getChildren().add(page);
    }

    // ==================== 藏音视图 ====================

    private void showFavorites() {
        VBox page = new VBox(8);

        Label title = new Label("藏音");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        // 收藏歌曲表格
        TableView<Song> table = createSongTable(viewModel.getFavoriteSongs());
        VBox.setVgrow(table, Priority.ALWAYS);

        // 空状态
        if (viewModel.getFavoriteSongs().isEmpty()) {
            table.setPlaceholder(EmptyState.favoritesEmpty());
        }

        // 监听收藏列表变化，更新空状态
        viewModel.getFavoriteSongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            if (viewModel.getFavoriteSongs().isEmpty()) {
                table.setPlaceholder(EmptyState.favoritesEmpty());
            }
        });

        page.getChildren().addAll(title, table);
        contentArea.getChildren().add(page);
    }

    // ==================== 余音视图 ====================

    private void showHistory() {
        VBox page = new VBox(8);

        Label title = new Label("余音");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        // 播放历史列表（按时间分组显示）
        ListView<PlayHistory> historyListView = new ListView<>();
        historyListView.getStyleClass().add("history-list");
        VBox.setVgrow(historyListView, Priority.ALWAYS);
        historyListView.setCellFactory(list -> new ListCell<>() {
            private final HBox cell = new HBox(8);
            private final Label playIcon = new Label("\u25B6");
            private final VBox textArea = new VBox(2);
            private final Label titleLabel = new Label();
            private final Label timeLabel = new Label();

            {
                playIcon.setStyle("-fx-font-size: 10px; -fx-text-fill: -primary;");
                titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");
                timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
                textArea.getChildren().addAll(titleLabel, timeLabel);
                HBox.setHgrow(textArea, Priority.ALWAYS);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(6, 4, 6, 4));
                cell.getChildren().addAll(playIcon, textArea);
            }

            @Override
            protected void updateItem(PlayHistory item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                String artist = item.getSongArtist() != null ? item.getSongArtist() : "未知歌手";
                titleLabel.setText(item.getSongTitle() + " - " + artist);
                timeLabel.setText(item.getPlayedAt() != null
                    ? item.getPlayedAt().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                    : "");
                setGraphic(cell);
            }
        });
        historyListView.setItems(viewModel.getPlayHistoryList());
        historyListView.setPlaceholder(EmptyState.historyEmpty());

        historyListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                PlayHistory selected = historyListView.getSelectionModel().getSelectedItem();
                if (selected != null) viewModel.playHistorySong(selected);
            }
        });

        // 清除历史按钮
        AppButton clearHistoryBtn = new AppButton("清除历史", AppButton.Style.OUTLINE);
        clearHistoryBtn.setOnAction(e -> {
            boolean confirmed = appContext.getDialogService().showConfirm("确认清除", "确定要清除所有播放历史吗？");
            if (confirmed) viewModel.clearPlayHistory();
        });

        page.getChildren().addAll(title, historyListView, clearHistoryBtn);
        contentArea.getChildren().add(page);
    }

    // ==================== 曲笺视图 ====================

    private void showPlaylists() {
        VBox page = new VBox(8);

        Label title = new Label("曲笺");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        // 内容容器：索引视图和详情视图互斥切换
        VBox playlistContainer = new VBox();
        VBox.setVgrow(playlistContainer, Priority.ALWAYS);

        // ===== 索引视图：曲笺列表 =====
        ListView<Object> playlistIndexListView = new ListView<>();
        playlistIndexListView.getStyleClass().add("playlist-list");
        VBox.setVgrow(playlistIndexListView, Priority.ALWAYS);
        playlistIndexListView.setCellFactory(list -> new ListCell<>() {
            private final HBox cell = new HBox(8);
            private final Label iconLabel = new Label();
            private final VBox textArea = new VBox(2);
            private final Label nameLabel = new Label();
            private final Label countLabel = new Label();
            private final Label arrowLabel = new Label(">");
            private final Label deleteLabel = new Label("x");

            {
                iconLabel.setPrefWidth(20);
                iconLabel.setStyle("-fx-font-size: 13px; -fx-alignment: center;");
                nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: -text-primary;");
                countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
                arrowLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
                deleteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-cursor: hand; -fx-padding: 0 4; -fx-alignment: center-right;");
                deleteLabel.setOnMouseEntered(e -> deleteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 0 4; -fx-alignment: center-right;"));
                deleteLabel.setOnMouseExited(e -> deleteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-cursor: hand; -fx-padding: 0 4; -fx-alignment: center-right;"));
                HBox.setHgrow(textArea, Priority.ALWAYS);
                textArea.getChildren().addAll(nameLabel, countLabel);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(8, 4, 8, 4));
                cell.setStyle("-fx-cursor: hand;");
                cell.getChildren().addAll(iconLabel, textArea, arrowLabel, deleteLabel);

                // 删除按钮点击事件（consume 阻止冒泡）
                deleteLabel.setOnMousePressed(e -> {
                    e.consume();
                    Object item = getItem();
                    if (item instanceof Playlist pl) {
                        boolean confirmed = appContext.getDialogService().showConfirm(
                            "确认删除", "确定要删除曲笺「" + pl.getName() + "」吗？");
                        if (confirmed) viewModel.deletePlaylist(pl.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                if (item instanceof String) {
                    iconLabel.setText("\u2665");
                    iconLabel.setStyle("-fx-font-size: 13px; -fx-alignment: center; -fx-text-fill: #ef4444;");
                    nameLabel.setText("藏音");
                    nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #ef4444;");
                    countLabel.setText(viewModel.getFavoriteSongs().size() + " 首");
                    deleteLabel.setVisible(false);
                    deleteLabel.setManaged(false);
                } else if (item instanceof Playlist pl) {
                    iconLabel.setText("\u266B");
                    iconLabel.setStyle("-fx-font-size: 13px; -fx-alignment: center; -fx-text-fill: -text-secondary;");
                    nameLabel.setText(pl.getName());
                    nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: -text-primary;");
                    countLabel.setText(pl.getSongCount() + " 首");
                    deleteLabel.setVisible(true);
                    deleteLabel.setManaged(true);
                }
                setGraphic(cell);
            }
        });

        ObservableList<Object> playlistIndexItems = FXCollections.observableArrayList();
        playlistIndexItems.add("藏音");
        playlistIndexListView.setItems(playlistIndexItems);
        playlistIndexListView.setPlaceholder(EmptyState.playlistsEmpty());

        // ===== 详情视图：曲笺/藏音歌曲列表 =====
        final int[] currentPlaylistId = { -1 };

        HBox detailHeader = new HBox(8);
        detailHeader.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("< 返回");
        backBtn.setStyle("-fx-font-size: 12px; -fx-background-color: transparent; -fx-text-fill: -primary; -fx-cursor: hand; -fx-padding: 2 6;");
        Label detailTitle = new Label();
        detailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        Label detailCount = new Label();
        detailCount.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");
        Button playAllBtn = new Button("\u25B6 播放全部");
        playAllBtn.setStyle("-fx-font-size: 11px; -fx-padding: 2 8; -fx-background-color: rgba(91,141,239,0.1); -fx-text-fill: -primary; -fx-background-radius: 4; -fx-cursor: hand;");
        detailHeader.getChildren().addAll(backBtn, detailTitle, detailCount, playAllBtn);

        ListView<Song> detailSongListView = new ListView<>();
        detailSongListView.getStyleClass().add("queue-list");
        VBox.setVgrow(detailSongListView, Priority.ALWAYS);
        detailSongListView.setCellFactory(list -> new SongDetailCell());

        VBox detailView = new VBox(8);
        VBox.setVgrow(detailView, Priority.ALWAYS);
        detailView.getChildren().addAll(detailHeader, detailSongListView);

        // ===== 切换逻辑：替换 children，彻底避免重叠 =====
        playlistContainer.getChildren().setAll(playlistIndexListView);

        // 点击曲笺索引 → 替换为详情视图
        playlistIndexListView.setOnMousePressed(e -> {
            Object selected = playlistIndexListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            if ("藏音".equals(selected)) {
                currentPlaylistId[0] = -1;
                detailTitle.setText("藏音");
                detailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #ef4444;");
                detailSongListView.setItems(viewModel.getFavoriteSongs());
                detailSongListView.setCellFactory(list -> new SongDetailCell(true));
                detailCount.textProperty().bind(Bindings.size(viewModel.getFavoriteSongs()).asString().concat(" 首"));
                playAllBtn.setOnAction(ev -> viewModel.playFavoriteSongs());
            } else if (selected instanceof Playlist pl) {
                currentPlaylistId[0] = pl.getId();
                detailTitle.setText(pl.getName());
                detailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
                viewModel.loadPlaylistSongs(pl.getId());
                detailSongListView.setItems(viewModel.getPlaylistSongs());
                detailSongListView.setCellFactory(list -> new SongDetailCell(false, currentPlaylistId));
                detailCount.setText(pl.getSongCount() + " 首");
                playAllBtn.setOnAction(ev -> viewModel.playPlaylist(pl.getId()));
            }

            playlistContainer.getChildren().setAll(detailView);
        });

        // 返回按钮 → 替换回索引视图
        backBtn.setOnAction(e -> {
            detailCount.textProperty().unbind();
            playlistContainer.getChildren().setAll(playlistIndexListView);
        });

        // 新建曲笺按钮
        AppButton newPlaylistBtn = new AppButton("新建曲笺", AppButton.Style.PRIMARY);
        newPlaylistBtn.setMaxWidth(Double.MAX_VALUE);
        newPlaylistBtn.setOnAction(e -> {
            PlaylistDialog dialog = new PlaylistDialog("新建曲笺", null);
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String name = dialog.getPlaylistName();
                String desc = dialog.getPlaylistDescription();
                if (!name.isEmpty()) viewModel.createPlaylist(name, desc);
            }
        });

        // 当歌单列表刷新时，同步索引列表
        viewModel.getPlaylists().addListener((javafx.collections.ListChangeListener<Playlist>) c -> {
            playlistIndexItems.clear();
            playlistIndexItems.add("藏音");
            playlistIndexItems.addAll(viewModel.getPlaylists());
        });

        // 当收藏列表变化时，刷新索引列表中"藏音"的计数
        viewModel.getFavoriteSongs().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            playlistIndexListView.refresh();
        });

        page.getChildren().addAll(title, playlistContainer, newPlaylistBtn);
        contentArea.getChildren().add(page);
    }

    // ==================== 音乐设置视图 ====================

    private void showSettings() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(8));

        Label title = new Label("音乐设置");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        Label hint = new Label("音乐设置功能即将上线\n包括：默认播放模式、下载路径、音质偏好等");
        hint.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary; -fx-text-alignment: center;");

        page.getChildren().addAll(title, hint);
        contentArea.getChildren().add(page);
    }

    // ==================== 错误提示栏 ====================

    private HBox createErrorBar() {
        HBox errorBar = new HBox(8);
        errorBar.setAlignment(Pos.CENTER_LEFT);
        errorBar.setStyle("-fx-padding: 6 12; -fx-background-color: rgba(239,68,68,0.08); -fx-background-radius: 8;");
        errorBar.setVisible(false);
        errorBar.setManaged(false);

        Label errorIcon = new Label("!");
        errorIcon.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #ef4444; -fx-pref-width: 20px; -fx-pref-height: 20px; -fx-alignment: center; -fx-background-color: rgba(239,68,68,0.15); -fx-background-radius: 10;");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ef4444;");
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.setWrapText(true);

        Button errorDismiss = new Button("x");
        errorDismiss.setStyle("-fx-font-size: 11px; -fx-background-color: transparent; -fx-text-fill: -text-tertiary; -fx-padding: 2 6; -fx-cursor: hand;");
        errorDismiss.setOnAction(e -> viewModel.errorMessageProperty().set(""));

        errorBar.getChildren().addAll(errorIcon, errorLabel, errorDismiss);
        viewModel.errorMessageProperty().addListener((obs, old, val) -> {
            boolean hasError = val != null && !val.isEmpty();
            errorBar.setVisible(hasError);
            errorBar.setManaged(hasError);
        });

        return errorBar;
    }

    // ==================== 歌曲表格 ====================

    /**
     * 创建配置好的歌曲表格。
     */
    private TableView<Song> createSongTable(ObservableList<Song> items) {
        TableView<Song> table = new TableView<>();
        table.getStyleClass().add("app-table-view");
        table.setItems(items);
        if (items instanceof javafx.collections.transformation.SortedList) {
            ((javafx.collections.transformation.SortedList<Song>) items).comparatorProperty().bind(table.comparatorProperty());
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<Song, String> titleCol = new TableColumn<>("歌名");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayTitle()));
        titleCol.setMinWidth(100);
        titleCol.setComparator(String::compareToIgnoreCase);

        TableColumn<Song, String> artistCol = new TableColumn<>("歌手");
        artistCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayArtist()));
        artistCol.setMinWidth(60);
        artistCol.setComparator(String::compareToIgnoreCase);

        TableColumn<Song, String> formatCol = new TableColumn<>("格式");
        formatCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormatLabel()));
        formatCol.setMinWidth(45);
        formatCol.setMaxWidth(70);
        formatCol.setPrefWidth(55);
        formatCol.setComparator(String::compareTo);

        TableColumn<Song, String> durationCol = new TableColumn<>("时长");
        durationCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDurationText()));
        durationCol.setMinWidth(50);
        durationCol.setMaxWidth(80);
        durationCol.setPrefWidth(65);
        durationCol.setComparator((s1, s2) -> Double.compare(parseDuration(s1), parseDuration(s2)));

        TableColumn<Song, Void> actionCol = new TableColumn<>("操作");
        actionCol.setMinWidth(300);
        actionCol.setMaxWidth(340);
        actionCol.setPrefWidth(320);
        actionCol.setResizable(false);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final HBox actions = new HBox(6);
            private final Button playBtn = new Button("播放");
            private final Button favBtn = new Button("收藏");
            private final Button playlistBtn = new Button("曲笺");
            private final Button delBtn = new Button("移除");

            {
                playBtn.getStyleClass().addAll("app-button", "button-primary");
                favBtn.getStyleClass().addAll("app-button", "button-secondary");
                playlistBtn.getStyleClass().addAll("app-button", "button-secondary");
                delBtn.getStyleClass().addAll("app-button", "button-danger");
                configureTableActionButton(playBtn, 64);
                configureTableActionButton(favBtn, 72);
                configureTableActionButton(playlistBtn, 64);
                configureTableActionButton(delBtn, 64);

                playBtn.setOnAction(e -> viewModel.playSong(getTableView().getItems().get(getIndex())));
                favBtn.setOnAction(e -> {
                    Song song = getTableView().getItems().get(getIndex());
                    viewModel.toggleFavorite(song);
                });
                playlistBtn.setOnAction(e -> {
                    Song song = getTableView().getItems().get(getIndex());
                    viewModel.showAddToPlaylistDialog(song);
                });
                delBtn.setOnAction(e -> viewModel.deleteSong(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Song song = getTableView().getItems().get(getIndex());
                favBtn.setText(song.isFavorited() ? "已收藏" : "收藏");
                favBtn.getStyleClass().removeAll("button-secondary", "button-primary");
                favBtn.getStyleClass().add(song.isFavorited() ? "button-primary" : "button-secondary");
                actions.getChildren().setAll(playBtn, favBtn, playlistBtn, delBtn);
                setGraphic(actions);
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<Song, ?>[] cols = new TableColumn[] { titleCol, artistCol, formatCol, durationCol, actionCol };
        table.getColumns().addAll(cols);
        titleCol.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(titleCol);

        // 空状态
        table.setPlaceholder(EmptyState.localEmpty());

        return table;
    }

    // ==================== 在线歌曲表格（带来源列） ====================

    /**
     * 创建在线歌曲表格，包含来源列和试听/加入队列/收藏/下载操作按钮。
     */
    private TableView<Song> createOnlineSongTable() {
        TableView<Song> table = new TableView<>();
        table.getStyleClass().add("app-table-view");
        table.setItems(viewModel.getOnlineSongs());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<Song, String> titleCol = new TableColumn<>("歌名");
        titleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayTitle()));
        titleCol.setMinWidth(100);
        titleCol.setComparator(String::compareToIgnoreCase);

        TableColumn<Song, String> artistCol = new TableColumn<>("歌手");
        artistCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDisplayArtist()));
        artistCol.setMinWidth(60);
        artistCol.setComparator(String::compareToIgnoreCase);

        TableColumn<Song, String> sourceCol = new TableColumn<>("来源");
        sourceCol.setCellValueFactory(d -> {
            String source = d.getValue().getSource();
            String name = switch (source) {
                case "jamendo" -> "Jamendo";
                case "netease" -> "网易云";
                case "qq" -> "QQ音乐";
                case "kugou" -> "酷狗";
                case "migu" -> "咪咕";
                default -> source != null ? source : "本地";
            };
            return new SimpleStringProperty(name);
        });
        sourceCol.setMinWidth(60);
        sourceCol.setMaxWidth(80);
        sourceCol.setPrefWidth(70);

        TableColumn<Song, String> formatCol = new TableColumn<>("格式");
        formatCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFormatLabel()));
        formatCol.setMinWidth(45);
        formatCol.setMaxWidth(70);
        formatCol.setPrefWidth(55);

        TableColumn<Song, String> durationCol = new TableColumn<>("时长");
        durationCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDurationText()));
        durationCol.setMinWidth(50);
        durationCol.setMaxWidth(80);
        durationCol.setPrefWidth(65);

        TableColumn<Song, Void> actionCol = new TableColumn<>("操作");
        actionCol.setMinWidth(300);
        actionCol.setMaxWidth(340);
        actionCol.setPrefWidth(320);
        actionCol.setResizable(false);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final HBox actions = new HBox(6);
            private final Button playBtn = new Button("试听");
            private final Button queueBtn = new Button("入队");
            private final Button favBtn = new Button("收藏");
            private final Button downloadBtn = new Button("下载");

            {
                playBtn.getStyleClass().addAll("app-button", "button-primary");
                queueBtn.getStyleClass().addAll("app-button", "button-secondary");
                favBtn.getStyleClass().addAll("app-button", "button-secondary");
                downloadBtn.getStyleClass().addAll("app-button", "button-secondary");
                configureTableActionButton(playBtn, 64);
                configureTableActionButton(queueBtn, 64);
                configureTableActionButton(favBtn, 72);
                configureTableActionButton(downloadBtn, 64);
                Tooltip.install(favBtn, new Tooltip("收藏到藏音"));
                Tooltip.install(downloadBtn, new Tooltip("下载到本地曲库"));

                playBtn.setOnAction(e -> viewModel.playOnlineSong(getTableView().getItems().get(getIndex())));
                queueBtn.setOnAction(e -> {
                    Song song = getTableView().getItems().get(getIndex());
                    viewModel.addOnlineToQueue(song);
                });
                favBtn.setOnAction(e -> {
                    Song song = getTableView().getItems().get(getIndex());
                    viewModel.favoriteOnlineSong(song);
                });
                downloadBtn.setOnAction(e -> {
                    Song song = getTableView().getItems().get(getIndex());
                    viewModel.downloadSong(song);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Song song = getTableView().getItems().get(getIndex());
                boolean favorited = song.isFavorited();
                favBtn.setText(favorited ? "已藏" : "收藏");
                favBtn.getStyleClass().removeAll("button-secondary", "button-primary");
                favBtn.getStyleClass().add(favorited ? "button-primary" : "button-secondary");

                // 下载按钮状态：已下载（本地文件存在）显示为「已下载」并禁用
                boolean isLocal = song.getUrl() != null && song.getUrl().startsWith("file:");
                downloadBtn.setText(isLocal ? "已下载" : "下载");
                downloadBtn.setDisable(isLocal);

                actions.getChildren().setAll(playBtn, queueBtn, favBtn, downloadBtn);
                setGraphic(actions);
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<Song, ?>[] cols = new TableColumn[] { titleCol, artistCol, sourceCol, formatCol, durationCol, actionCol };
        table.getColumns().addAll(cols);
        titleCol.setSortType(TableColumn.SortType.DESCENDING);
        table.getSortOrder().add(titleCol);

        return table;
    }

    private void configureTableActionButton(Button button, double width) {
        button.setMinWidth(width);
        button.setPrefWidth(width);
        button.setMaxWidth(width);
        button.setTextOverrun(OverrunStyle.CLIP);
        button.setAlignment(Pos.CENTER);
    }

    // ==================== 批量操作栏 ====================

    /**
     * 创建批量操作栏。
     */
    private HBox createBatchBar(TableView<Song> table) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);

        AppButton batchQueueBtn = new AppButton("加入队列", AppButton.Style.OUTLINE);
        AppButton batchFavBtn = new AppButton("批量藏音", AppButton.Style.OUTLINE);
        AppButton batchDelBtn = new AppButton("批量删除", AppButton.Style.OUTLINE);

        batchQueueBtn.setVisible(false);
        batchQueueBtn.setManaged(false);
        batchFavBtn.setVisible(false);
        batchFavBtn.setManaged(false);
        batchDelBtn.setVisible(false);
        batchDelBtn.setManaged(false);

        Label selectionLabel = new Label();
        selectionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -primary; -fx-font-weight: 500;");
        selectionLabel.setVisible(false);
        selectionLabel.setManaged(false);

        batchQueueBtn.setOnAction(e -> {
            List<Song> selected = table.getSelectionModel().getSelectedItems();
            if (!selected.isEmpty()) viewModel.batchAddToQueue(selected);
        });
        batchFavBtn.setOnAction(e -> {
            List<Song> selected = table.getSelectionModel().getSelectedItems();
            if (!selected.isEmpty()) viewModel.batchFavorite(selected);
        });
        batchDelBtn.setOnAction(e -> {
            List<Song> selected = table.getSelectionModel().getSelectedItems();
            if (!selected.isEmpty()) {
                boolean confirmed = appContext.getDialogService().showConfirm("确认删除", "确定要删除选中的 " + selected.size() + " 首歌曲吗？");
                if (confirmed) viewModel.batchDelete(selected);
            }
        });

        table.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<Song>) c -> {
            int size = table.getSelectionModel().getSelectedItems().size();
            boolean hasSelection = size > 0;
            batchQueueBtn.setVisible(hasSelection);
            batchQueueBtn.setManaged(hasSelection);
            batchFavBtn.setVisible(hasSelection);
            batchFavBtn.setManaged(hasSelection);
            batchDelBtn.setVisible(hasSelection);
            batchDelBtn.setManaged(hasSelection);
            selectionLabel.setVisible(hasSelection);
            selectionLabel.setManaged(hasSelection);
            selectionLabel.setText("已选中 " + size + " 首");
        });

        bar.getChildren().addAll(batchQueueBtn, batchFavBtn, batchDelBtn, selectionLabel);
        return bar;
    }

    // ==================== 播放队列单元格 ====================

    /**
     * 播放队列列表单元格：点击播放当前歌曲，右键可移除。
     */
    private class QueueListCell extends ListCell<Song> {
        private final HBox cell = new HBox(8);
        private final Label indexLabel = new Label();
        private final VBox textArea = new VBox(2);
        private final Label titleLabel = new Label();
        private final Label artistLabel = new Label();
        private final Label formatBadge = new Label();
        private final Label removeLabel = new Label("x");

        QueueListCell() {
            indexLabel.setPrefWidth(22);
            indexLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-alignment: center-right;");
            titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");
            artistLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");
            formatBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-tertiary; -fx-padding: 1 5; -fx-background-color: -bg-tertiary; -fx-background-radius: 4;");
            removeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-padding: 0 4; -fx-cursor: hand; -fx-alignment: center-right;");
            removeLabel.setOnMouseEntered(e -> removeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-padding: 0 4; -fx-cursor: hand; -fx-alignment: center-right;"));
            removeLabel.setOnMouseExited(e -> removeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-padding: 0 4; -fx-cursor: hand; -fx-alignment: center-right;"));

            HBox.setHgrow(textArea, Priority.ALWAYS);
            textArea.getChildren().addAll(titleLabel, artistLabel);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(6, 4, 6, 4));
            cell.getChildren().addAll(indexLabel, textArea, formatBadge, removeLabel);

            // 点击播放：跳到该索引，不重建队列
            setOnMouseClicked(e -> {
                if (e.getClickCount() == 1 && !isEmpty() && e.getTarget() != removeLabel) {
                    Song song = getItem();
                    if (song != null) {
                        int idx = getListView().getItems().indexOf(song);
                        viewModel.playAtIndex(idx);
                    }
                }
            });
        }

        @Override
        protected void updateItem(Song song, boolean empty) {
            super.updateItem(song, empty);
            if (empty || song == null) { setGraphic(null); return; }
            int idx = getListView().getItems().indexOf(song);
            Song current = viewModel.getCurrentSong();
            boolean isCurrent = song == current;
            indexLabel.setText(String.valueOf(idx + 1));
            titleLabel.setText(song.getDisplayTitle());
            artistLabel.setText(song.getDisplayArtist());
            formatBadge.setText(song.getFormatLabel());

            // 移除按钮事件
            removeLabel.setOnMouseClicked(e -> {
                viewModel.removeFromQueue(song);
            });

            if (isCurrent) {
                cell.setStyle("-fx-background-color: rgba(91,141,239,0.08); -fx-background-radius: 6;");
                indexLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -primary; -fx-font-weight: 700; -fx-alignment: center-right;");
                titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -primary; -fx-font-weight: 600;");
            } else {
                cell.setStyle("-fx-background-color: transparent; -fx-background-radius: 6;");
                indexLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-alignment: center-right;");
                titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");
            }
            setGraphic(cell);
        }
    }

    // ==================== 曲笺/藏音歌曲详情单元格 ====================

    /**
     * 曲笺/藏音歌曲详情单元格：显示歌名、歌手、格式，双击播放。
     *
     * @param showFavorite 是否显示收藏/取消收藏按钮（藏音列表需要）
     * @param playlistIdRef 曲笺 ID 引用（非空时显示移除按钮）
     */
    private class SongDetailCell extends ListCell<Song> {
        private final HBox cell = new HBox(12);
        private final Label playIcon = new Label("\u25B6");
        private final VBox textArea = new VBox(2);
        private final Label titleLabel = new Label();
        private final Label artistLabel = new Label();
        private final Label durationLabel = new Label();
        private final Label formatBadge = new Label();
        private final Label favLabel = new Label("\u2665");
        private final Label removeLabel = new Label("x");
        private final boolean showFavorite;
        private final int[] playlistIdRef;

        SongDetailCell() { this(false, null); }

        SongDetailCell(boolean showFavorite) { this(showFavorite, null); }

        SongDetailCell(boolean showFavorite, int[] playlistIdRef) {
            this.showFavorite = showFavorite;
            this.playlistIdRef = playlistIdRef;
            playIcon.setPrefWidth(20);
            playIcon.setStyle("-fx-font-size: 10px; -fx-text-fill: -primary; -fx-alignment: center;");
            titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary; -fx-font-weight: 500;");
            artistLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");
            durationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
            formatBadge.setStyle("-fx-font-size: 10px; -fx-text-fill: -text-tertiary; -fx-padding: 1 6; -fx-background-color: -bg-tertiary; -fx-background-radius: 4;");
            favLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 0 6; -fx-alignment: center-right;");
            favLabel.setOnMouseEntered(e -> favLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: #dc2626; -fx-cursor: hand; -fx-padding: 0 6; -fx-alignment: center-right;"));
            favLabel.setOnMouseExited(e -> favLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 0 6; -fx-alignment: center-right;"));
            favLabel.setOnMousePressed(e -> {
                e.consume();
                Song song = SongDetailCell.this.getItem();
                if (song != null) {
                    viewModel.toggleFavorite(song);
                    viewModel.loadFavoriteSongs();
                }
            });
            removeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-cursor: hand; -fx-padding: 0 6; -fx-alignment: center-right;");
            removeLabel.setOnMouseEntered(e -> removeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 0 6; -fx-alignment: center-right;"));
            removeLabel.setOnMouseExited(e -> removeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-cursor: hand; -fx-padding: 0 6; -fx-alignment: center-right;"));
            removeLabel.setOnMousePressed(e -> {
                e.consume();
                Song song = SongDetailCell.this.getItem();
                if (song != null && playlistIdRef != null && playlistIdRef[0] > 0) {
                    viewModel.removeSongFromPlaylist(playlistIdRef[0], song);
                }
            });
            HBox.setHgrow(textArea, Priority.ALWAYS);
            textArea.getChildren().addAll(titleLabel, artistLabel);
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.setPadding(new Insets(8, 6, 8, 6));
            if (this.showFavorite && this.playlistIdRef == null) {
                cell.getChildren().addAll(playIcon, textArea, durationLabel, formatBadge, favLabel);
            } else if (this.playlistIdRef != null) {
                cell.getChildren().addAll(playIcon, textArea, durationLabel, formatBadge, removeLabel);
            } else {
                cell.getChildren().addAll(playIcon, textArea, durationLabel, formatBadge);
            }
            setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !isEmpty()) {
                    Song song = getItem();
                    if (song != null) viewModel.playSong(song);
                }
            });
        }

        @Override
        protected void updateItem(Song song, boolean empty) {
            super.updateItem(song, empty);
            if (empty || song == null) { setGraphic(null); return; }
            titleLabel.setText(song.getDisplayTitle());
            artistLabel.setText(song.getDisplayArtist());
            durationLabel.setText(song.getDurationText());
            formatBadge.setText(song.getFormatLabel());
            setGraphic(cell);
        }
    }

    // ==================== 工具方法 ====================

    /** 解析时长字符串（如 "3:45"、"--:--"）为秒数 */
    private static double parseDuration(String text) {
        if (text == null || text.equals("--:--")) return -1;
        try {
            String[] parts = text.split(":");
            if (parts.length == 2) {
                int min = Integer.parseInt(parts[0].trim());
                int sec = Integer.parseInt(parts[1].trim());
                return min * 60 + sec;
            }
        } catch (NumberFormatException ignored) {}
        return -1;
    }
}
