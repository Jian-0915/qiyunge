package com.qiyunge.ui.shell;

import com.qiyunge.app.AppContext;
import com.qiyunge.app.NavigationService;
import com.qiyunge.app.UserSession;
import com.qiyunge.application.service.MusicPlayerService;
import com.qiyunge.domain.entity.Song;
import com.qiyunge.ui.components.LyricPane;
import com.qiyunge.ui.components.MusicBar;
import com.qiyunge.ui.components.SideNavItem;
import com.qiyunge.ui.components.UserAvatar;
import com.qiyunge.ui.components.WindowTitleBar;
import com.qiyunge.ui.dashboard.DashboardView;
import com.qiyunge.ui.music.MusicViewModel;
import com.qiyunge.ui.music.MusicView;
import com.qiyunge.ui.gallery.GalleryView;
import com.qiyunge.ui.entertainment.EntertainmentView;
import com.qiyunge.ui.profile.ProfileView;
import com.qiyunge.ui.admin.AdminView;
import com.qiyunge.ui.settings.SettingsView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

public class MainShell extends BorderPane {

    private final AppContext appContext;
    private final NavigationService navigationService;
    private final StackPane contentArea;
    private final VBox navBox;
    private Label pageTitleLabel;
    private Label userLabel;
    private UserAvatar userAvatar;
    private Runnable onLogout;
    private MusicBar musicBar;
    private StackPane shellRoot;
    private VBox queueOverlay;
    private VBox lyricOverlay;
    private LyricPane lyricPane;
    private boolean queueVisible = false;
    private boolean lyricVisible = false;
    private MusicViewModel musicViewModel;
    private MusicView currentMusicView;

    public MainShell(AppContext appContext) {
        this.appContext = appContext;
        this.navigationService = appContext.getNavigationService();

        // Window title bar (draggable, with min/max/close)
        WindowTitleBar windowBar = new WindowTitleBar(appContext.getPrimaryStage());

        // Top bar
        HBox topBar = createTopBar();

        VBox topContainer = new VBox(windowBar, topBar);
        topContainer.setStyle("-fx-background-color: -bg-topbar; -fx-border-color: -border-light; -fx-border-width: 0 0 0.5px 0;");

        // Side navigation
        navBox = createSideNav();

        // Content area
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);
        navigationService.setContentArea(contentArea);

        // Center: nav + content
        HBox centerArea = new HBox(navBox, contentArea);
        centerArea.getStyleClass().add("center-area");
        HBox.setHgrow(contentArea, Priority.ALWAYS);

        // Bottom music bar（通过 MusicViewModel 代理属性传入，不直接依赖 MusicPlayerService）
        musicViewModel = new MusicViewModel(appContext);
        this.musicBar = new MusicBar(
            musicViewModel.currentSongProperty(),
            musicViewModel.playingProperty(),
            musicViewModel.progressProperty(),
            musicViewModel.currentTimeTextProperty(),
            musicViewModel.totalTimeTextProperty(),
            musicViewModel.volumeProperty(),
            musicViewModel.playerErrorMessageProperty(),
            musicViewModel.playModeProperty(),
            musicViewModel.coverImageProperty(),
            musicViewModel::togglePause,
            musicViewModel::playNext,
            musicViewModel::playPrevious,
            musicViewModel::cyclePlayMode,
            ratio -> { musicViewModel.seek(ratio); return null; },
            this::toggleLyricOverlay,
            this::toggleQueueOverlay
        );

        // Register pages
        registerPages();

        // Assemble
        // MusicBar 在 BorderPane 底部，由 BorderPane 原生管理高度分配
        this.setTop(topContainer);
        this.setCenter(centerArea);
        this.setBottom(musicBar);
        this.getStyleClass().add("main-shell");

        queueOverlay = createQueueOverlay();
        lyricOverlay = createLyricOverlay();
        shellRoot = new StackPane(this, queueOverlay, lyricOverlay);
        StackPane.setAlignment(queueOverlay, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(queueOverlay, new Insets(0, 24, 84, 0));
        queueOverlay.setVisible(false);
        queueOverlay.setManaged(false);
        // 歌词面板不通过 StackPane alignment 定位，而是用 layoutX/Y + translateX/Y 实现拖动
        lyricOverlay.setLayoutX(400);
        lyricOverlay.setLayoutY(200);
        lyricOverlay.setVisible(false);
        lyricOverlay.setManaged(false);

        // Bind top bar user info to session
        bindUserInfo();

        // Navigate to dashboard
        navigationService.navigateTo(NavigationService.Page.DASHBOARD);
        updateNavHighlight(NavigationService.Page.DASHBOARD);

        // 监听导航变化，自动更新侧边栏高亮和页面标题
        navigationService.addNavigationListener(() -> {
            Platform.runLater(() -> {
                NavigationService.Page page = navigationService.getCurrentPage();
                updateNavHighlight(page);
                updatePageTitle(page);
            });
        });
    }

    public void setOnLogout(Runnable callback) {
        this.onLogout = callback;
    }

    public StackPane getRoot() {
        return shellRoot;
    }

    private void bindUserInfo() {
        // UserSession is not a JavaFX bean, so we update manually via listener pattern.
        // ProfileViewModel already calls onProfileUpdated after changes, which triggers
        // NavigationService to refresh. We listen for displayName changes here.
        UserSession session = appContext.getUserSession();
        session.addDisplayNameListener(newName -> {
            Platform.runLater(() -> {
                userLabel.setText(newName);
                userAvatar.update(newName);
                String color = session.getAvatarColor() != null ? session.getAvatarColor() : "#5B8DEF";
                userAvatar.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 50%;");
            });
        });
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.getStyleClass().add("top-bar");
        topBar.setPrefHeight(56);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 24, 0, 24));

        HBox titleGroup = new HBox();
        Label titleLabel = new Label("望云台");
        titleLabel.getStyleClass().add("title");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700;");
        titleGroup.getChildren().add(titleLabel);
        pageTitleLabel = titleLabel;

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("搜索...");
        searchField.getStyleClass().add("search-box");
        searchField.setPrefWidth(240);

        // User avatar + name (bound to session)
        HBox userBox = new HBox(8);
        userBox.setAlignment(Pos.CENTER);
        UserSession session = appContext.getUserSession();
        userAvatar = new UserAvatar(session.getDisplayName(), 32);
        String avatarColor = session.getAvatarColor() != null ? session.getAvatarColor() : "#5B8DEF";
        userAvatar.setStyle("-fx-background-color: " + avatarColor + "20; -fx-background-radius: 50%;");
        userLabel = new Label(session.getDisplayName());
        userLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
        userBox.getChildren().addAll(userAvatar, userLabel);

        topBar.getChildren().addAll(titleGroup, spacer, searchField, userBox);
        return topBar;
    }

    private VBox createSideNav() {
        VBox navBox = new VBox();
        navBox.getStyleClass().add("side-nav");
        navBox.setPrefWidth(200);
        navBox.setSpacing(2);
        navBox.setPadding(new Insets(12, 8, 12, 8));

        // App title in nav
        Label appTitle = new Label("栖云阁");
        appTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -primary; -fx-padding: 8px 16px 16px 16px;");
        navBox.getChildren().add(appTitle);

        // Common pages
        navBox.getChildren().addAll(
            createNavItem("望云台", "dashboard", NavigationService.Page.DASHBOARD),
            createNavItem("听雨轩", "music", NavigationService.Page.MUSIC),
            createNavItem("拾光廊", "gallery", NavigationService.Page.GALLERY),
            createNavItem("百趣园", "entertainment", NavigationService.Page.ENTERTAINMENT),
            createNavItem("吾庐", "profile", NavigationService.Page.PROFILE)
        );

        // Admin page - only visible for admins
        if (appContext.getUserSession().isAdmin()) {
            navBox.getChildren().add(createNavItem("阁务司", "admin", NavigationService.Page.ADMIN));
        }

        navBox.getChildren().add(createNavItem("云枢", "settings", NavigationService.Page.SETTINGS));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        navBox.getChildren().add(spacer);

        // Logout button at bottom
        SideNavItem logoutItem = new SideNavItem("离阁", "logout");
        logoutItem.setOnMouseClicked(e -> {
            if (onLogout != null) onLogout.run();
        });
        navBox.getChildren().add(logoutItem);

        return navBox;
    }

    private SideNavItem createNavItem(String label, String icon, NavigationService.Page page) {
        SideNavItem item = new SideNavItem(label, icon);
        item.setOnMouseClicked(e -> {
            navigationService.navigateTo(page);
            updatePageTitle(page);
            updateNavHighlight(page);
        });
        item.setUserData(page);
        return item;
    }

    private void updatePageTitle(NavigationService.Page page) {
        String[] titles = {"望云台", "听雨轩", "拾光廊", "百趣园", "吾庐", "阁务司", "云枢"};
        pageTitleLabel.setText(titles[page.ordinal()]);
    }

    private void updateNavHighlight(NavigationService.Page page) {
        for (javafx.scene.Node node : navBox.getChildren()) {
            if (node instanceof SideNavItem) {
                Object userData = node.getUserData();
                if (userData instanceof NavigationService.Page) {
                    node.getStyleClass().removeAll("active");
                    if (userData == page) {
                        node.getStyleClass().add("active");
                    }
                }
            }
        }
    }

    private void registerPages() {
        navigationService.registerPage(NavigationService.Page.DASHBOARD, () -> wrapPage(new DashboardView(appContext)));
        navigationService.registerPage(NavigationService.Page.MUSIC, () -> {
            MusicView mv = new MusicView(appContext, musicBar);
            currentMusicView = mv;
            return wrapPage(mv);
        });
        navigationService.registerPage(NavigationService.Page.GALLERY, () -> wrapPage(new GalleryView(appContext)));
        navigationService.registerPage(NavigationService.Page.ENTERTAINMENT, () -> wrapPage(new EntertainmentView(appContext)));
        navigationService.registerPage(NavigationService.Page.PROFILE, () -> {
            ProfileView pv = new ProfileView(appContext);
            pv.setOnAccountDeleted(() -> {
                if (onLogout != null) onLogout.run();
            });
            return wrapPage(pv);
        });
        navigationService.registerPage(NavigationService.Page.ADMIN, () -> wrapPage(new AdminView(appContext)));
        navigationService.registerPage(NavigationService.Page.SETTINGS, () -> wrapPage(new SettingsView(appContext)));
    }

    /** 将页面视图包装在 ScrollPane 中，占满整个 viewport 高度，避免内部 sidebar 被内容挤压。 */
    private Node wrapPage(Region pageContent) {
        ScrollPane sp = new ScrollPane(pageContent);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);  // 让内容占满 viewport 高度，sidebar 高度固定
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setPadding(new Insets(0, 0, 12, 0));
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    private VBox createQueueOverlay() {
        VBox panel = new VBox(8);
        panel.setPrefWidth(360);
        panel.setMaxWidth(360);
        panel.setPrefHeight(360);
        panel.setMaxHeight(360);
        panel.setPadding(new Insets(12));
        panel.setStyle(
            "-fx-background-color: -bg-primary;" +
            "-fx-border-color: -border-light;" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.16), 18, 0, 0, 4);"
        );

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("播放队列");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Label count = new Label();
        count.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");
        count.textProperty().bind(javafx.beans.binding.Bindings.size(musicViewModel.getQueue()).asString("(%d)"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label close = new Label("x");
        close.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary; -fx-cursor: hand;");
        close.setOnMouseClicked(e -> hideQueueOverlay());
        header.getChildren().addAll(title, count, spacer, close);

        ListView<Song> list = new ListView<>(musicViewModel.getQueue());
        list.getStyleClass().add("queue-list");
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setPlaceholder(new Label("暂无播放队列"));
        list.setCellFactory(view -> new ListCell<>() {
            private final HBox row = new HBox(8);
            private final Label indexLabel = new Label();
            private final VBox textBox = new VBox(2);
            private final Label titleLabel = new Label();
            private final Label artistLabel = new Label();
            private final Label removeLabel = new Label("x");

            {
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(6, 4, 6, 4));
                indexLabel.setPrefWidth(24);
                indexLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-alignment: center-right;");
                titleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-primary;");
                artistLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-secondary;");
                removeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-cursor: hand; -fx-padding: 0 4;");
                HBox.setHgrow(textBox, Priority.ALWAYS);
                textBox.getChildren().addAll(titleLabel, artistLabel);
                row.getChildren().addAll(indexLabel, textBox, removeLabel);
                setOnMouseClicked(e -> {
                    if (e.getClickCount() == 1 && !isEmpty() && e.getTarget() != removeLabel) {
                        int idx = getListView().getItems().indexOf(getItem());
                        musicViewModel.playAtIndex(idx);
                    }
                });
                removeLabel.setOnMouseClicked(e -> {
                    e.consume();
                    Song song = getItem();
                    if (song != null) musicViewModel.removeFromQueue(song);
                });
            }

            @Override
            protected void updateItem(Song song, boolean empty) {
                super.updateItem(song, empty);
                if (empty || song == null) {
                    setGraphic(null);
                    return;
                }
                int idx = getListView().getItems().indexOf(song);
                indexLabel.setText(String.valueOf(idx + 1));
                titleLabel.setText(song.getDisplayTitle());
                artistLabel.setText(song.getDisplayArtist());
                boolean current = song == musicViewModel.getCurrentSong();
                row.setStyle(current ? "-fx-background-color: rgba(91,141,239,0.08); -fx-background-radius: 6;" : "-fx-background-color: transparent;");
                setGraphic(row);
            }
        });

        panel.getChildren().addAll(header, list);
        return panel;
    }

    private VBox createLyricOverlay() {
        lyricPane = new LyricPane();
        lyricPane.setPrefWidth(360);
        lyricPane.setMinWidth(320);
        lyricPane.setMaxWidth(420);

        VBox panel = new VBox();
        panel.setPrefWidth(360);
        panel.setMaxWidth(420);
        panel.setMaxHeight(480);
        panel.setStyle(
            "-fx-background-color: -bg-primary;" +
            "-fx-border-color: -border-light;" +
            "-fx-border-width: 1px;" +
            "-fx-background-radius: 8px;" +
            "-fx-border-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.16), 18, 0, 0, 4);"
        );

        // ===== 可拖动标题栏 =====
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: move; -fx-padding: 8 12 0 12;");
        header.setPrefHeight(36);
        Label title = new Label("歌词");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label close = new Label("x");
        close.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-tertiary; -fx-cursor: hand;");
        close.setOnMouseClicked(e -> hideLyricOverlay());
        header.getChildren().addAll(title, spacer, close);

        // 拖动逻辑
        final double[] dragOffset = new double[2];
        header.setOnMousePressed(e -> {
            dragOffset[0] = e.getSceneX() - panel.getTranslateX();
            dragOffset[1] = e.getSceneY() - panel.getTranslateY();
        });
        header.setOnMouseDragged(e -> {
            panel.setTranslateX(e.getSceneX() - dragOffset[0]);
            panel.setTranslateY(e.getSceneY() - dragOffset[1]);
        });

        // LyricPane 填满剩余空间
        VBox.setVgrow(lyricPane, Priority.ALWAYS);

        panel.getChildren().addAll(header, lyricPane);

        // 绑定歌曲切换 → 自动加载歌词
        musicViewModel.currentSongProperty().addListener((obs, old, newSong) -> {
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

        // 绑定播放进度 → 更新歌词高亮
        musicViewModel.currentTimeTextProperty().addListener((obs, old, val) -> {
            if (val == null || val.isEmpty()) return;
            try {
                String[] parts = val.split(":");
                double sec = Double.parseDouble(parts[0]) * 60 + Double.parseDouble(parts[1]);
                lyricPane.updateProgress(sec * 1000);
                String totalVal = musicViewModel.totalTimeTextProperty().get();
                if (totalVal != null && !totalVal.isEmpty()) {
                    String[] tp = totalVal.split(":");
                    double totalSec = Double.parseDouble(tp[0]) * 60 + Double.parseDouble(tp[1]);
                    lyricPane.updateProgressbar(sec, totalSec);
                }
            } catch (Exception ignored) {}
        });

        // seek 回调
        lyricPane.setSeekCallback(progress -> musicViewModel.seek(progress));

        return panel;
    }

    private void toggleQueueOverlay() {
        if (queueVisible) {
            hideQueueOverlay();
        } else {
            queueVisible = true;
            queueOverlay.setVisible(true);
            queueOverlay.setManaged(true);
            queueOverlay.toFront();
        }
    }

    private void hideQueueOverlay() {
        queueVisible = false;
        queueOverlay.setVisible(false);
        queueOverlay.setManaged(false);
    }

    private void toggleLyricOverlay() {
        if (lyricVisible) {
            hideLyricOverlay();
        } else {
            hideQueueOverlay();
            lyricVisible = true;
            lyricOverlay.setVisible(true);
            lyricOverlay.setManaged(true);
            lyricOverlay.toFront();
        }
    }

    private void hideLyricOverlay() {
        lyricVisible = false;
        lyricOverlay.setVisible(false);
        lyricOverlay.setManaged(false);
    }
}
