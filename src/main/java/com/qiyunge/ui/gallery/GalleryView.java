package com.qiyunge.ui.gallery;

import com.qiyunge.app.AppContext;
import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.domain.entity.ImageAlbum;
import com.qiyunge.application.service.ImageProvider;
import com.qiyunge.ui.components.AppButton;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 拾光廊图库主视图。
 * 采用三栏布局：顶部标题栏、左侧边导航、主内容区。
 */
public class GalleryView extends BorderPane {

    private final AppContext appContext;
    private final GalleryViewModel viewModel;
    private final VBox contentArea;
    private final ScrollPane contentScrollPane;
    // 页面缓存：避免切换导航时重建页面丢失内容
    private final java.util.Map<String, javafx.scene.Node> pageCache = new java.util.HashMap<>();

    private final String[] categories = {"全部", "风景", "自然", "美食", "人物", "建筑", "其他"};
    private String onlineCategory = "全部";
    private String onlineProvider = null; // null = all providers
    private String currentPage = null;
    private TextField onlineSearchField;

    public GalleryView(AppContext appContext) {
        this.appContext = appContext;
        this.viewModel = new GalleryViewModel(appContext);
        this.getStyleClass().add("gallery-view");

        viewModel.addDataChangeListener(() -> {
            invalidatePage("categories");
            invalidatePage("favorites");
            invalidatePage("albums");
            // 立即刷新当前页面数据
            if (currentPage != null) {
                refreshPageData(currentPage);
            }
        });

        // ===== Top: 顶部标题栏 =====
        GalleryHeader header = new GalleryHeader(appContext);
        setTop(header);

        // ===== Left: 侧边导航栏 =====
        GallerySidebar sidebar = new GallerySidebar(appContext, this::navigateTo);
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
        setCenter(contentScrollPane);

        // 默认显示光影墙
        navigateTo("allImages");

        // 拖拽上传支持
        contentArea.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            e.consume();
        });
        contentArea.setOnDragDropped(e -> {
            javafx.scene.input.Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                for (File file : db.getFiles()) {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                        || name.endsWith(".webp") || name.endsWith(".gif")) {
                        viewModel.uploadImage(file, viewModel.selectedCategoryProperty().get(), null);
                    }
                }
                e.setDropCompleted(true);
            }
            e.consume();
        });
    }

    /**
     * 根据导航键切换主内容区视图。
     */
    private void navigateTo(String page) {
        contentArea.getChildren().clear();
        currentPage = page;

        // 优先从缓存加载页面（保留搜索结果等状态）
        javafx.scene.Node cached = pageCache.get(page);
        if (cached != null) {
            contentArea.getChildren().add(cached);
            // 从缓存加载时强制刷新数据，确保数据是最新的
            refreshPageData(page);
            return;
        }

        switch (page) {
            case "allImages" -> showAllImages();
            case "categories" -> showCategories();
            case "favorites" -> showFavorites();
            case "albums" -> showAlbums();
            case "upload" -> {
                handleUpload();
                navigateTo("allImages");
            }
            case "onlineSearch" -> showOnlineSearch();
            case "settings" -> showSettings();
            default -> showAllImages();
        }
    }

    /**
     * 从缓存加载页面后刷新数据。
     * 确保页面显示的是最新数据，避免反复进出页面时数据不更新。
     */
    private void refreshPageData(String page) {
        switch (page) {
            case "allImages" -> viewModel.loadImages();
            case "favorites" -> viewModel.loadFavoriteImages();
            case "categories" -> {
                invalidatePage("categories");
                // 重建当前显示的风物卷页面
                if ("categories".equals(currentPage)) {
                    contentArea.getChildren().clear();
                    showCategories();
                }
            }
            case "albums" -> {
                invalidatePage("albums");
                if ("albums".equals(currentPage)) {
                    contentArea.getChildren().clear();
                    showAlbums();
                }
            }
            case "onlineSearch" -> {
            }
        }
    }

    /** 将页面添加到内容区并缓存 */
    private void addPage(String key, javafx.scene.Node page) {
        pageCache.put(key, page);
        VBox.setVgrow(page, Priority.ALWAYS);
        contentArea.getChildren().add(page);
    }

    /** 清除指定页面的缓存（数据变更后需要刷新时调用） */
    private void invalidatePage(String key) {
        pageCache.remove(key);
    }

    // ==================== 光影墙视图 ====================

    private void showAllImages() {
        VBox page = new VBox(8);

        // 工具栏：搜索 + 排序 + 视图切换 + 上传
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("寻影...");
        searchField.getStyleClass().add("search-box");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, old, val) -> viewModel.search(val));

        // 排序选择
        ChoiceBox<String> sortChoice = new ChoiceBox<>();
        sortChoice.getItems().addAll("时间↓", "时间↑", "名称", "大小↓", "大小↑");
        sortChoice.setValue("时间↓");
        sortChoice.setStyle("-fx-font-size: 12px;");
        sortChoice.setPrefWidth(90);
        sortChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) viewModel.sortImages(val);
        });

        // 视图切换按钮
        Button gridBtn = new Button("▦");
        gridBtn.setStyle("-fx-font-size: 14px; -fx-background-color: -primary-light; -fx-text-fill: -primary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
        Button listBtn = new Button("☰");
        listBtn.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: -text-secondary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");

        AppButton uploadBtn = new AppButton("采风上传", AppButton.Style.SECONDARY);
        uploadBtn.setOnAction(e -> handleUpload());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(searchField, sortChoice, spacer, gridBtn, listBtn, uploadBtn);

        // 分类标签栏
        HBox categoryTabs = createCategoryTabs();

        // 图片网格（默认视图）
        FlowPane imageGrid = new FlowPane(16, 16);
        imageGrid.getStyleClass().add("gallery-image-grid");

        ScrollPane imageScrollPane = new ScrollPane(imageGrid);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        imageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        imageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(imageScrollPane, Priority.ALWAYS);

        // 图片列表视图（TableView）
        TableView<GalleryImage> imageTable = createImageTable();
        VBox.setVgrow(imageTable, Priority.ALWAYS);
        imageTable.setVisible(false);
        imageTable.setManaged(false);

        // 空状态
        GalleryEmptyState emptyState = GalleryEmptyState.allImagesEmpty();
        emptyState.setOnUpload(this::handleUpload);
        emptyState.setOnExplore(() -> navigateTo("onlineSearch"));
        VBox emptyStateWrapper = createEmptyState(emptyState);

        // 批量操作栏
        HBox batchBar = createBatchBar(imageGrid);

        // 错误提示栏
        HBox errorBar = createErrorBar();

        page.getChildren().addAll(toolbar, categoryTabs, batchBar, imageScrollPane, imageTable, emptyStateWrapper, errorBar);
        addPage("allImages", page);

        // 视图切换逻辑
        gridBtn.setOnAction(e -> {
            imageScrollPane.setVisible(true);
            imageScrollPane.setManaged(true);
            imageTable.setVisible(false);
            imageTable.setManaged(false);
            gridBtn.setStyle("-fx-font-size: 14px; -fx-background-color: -primary-light; -fx-text-fill: -primary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
            listBtn.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: -text-secondary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
        });
        listBtn.setOnAction(e -> {
            imageScrollPane.setVisible(false);
            imageScrollPane.setManaged(false);
            imageTable.setVisible(true);
            imageTable.setManaged(true);
            listBtn.setStyle("-fx-font-size: 14px; -fx-background-color: -primary-light; -fx-text-fill: -primary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
            gridBtn.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: -text-secondary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand;");
            imageTable.setItems(viewModel.getImages());
        });

        // 加载数据
        viewModel.loadImages();

        // 监听图片列表变化刷新网格
        viewModel.getImages().addListener((javafx.collections.ListChangeListener<GalleryImage>) c -> {
            Platform.runLater(() -> refreshImageGrid(imageGrid, emptyStateWrapper));
        });

        // 初始刷新
        refreshImageGrid(imageGrid, emptyStateWrapper);
    }

    /** 创建图片列表视图（TableView） */
    private TableView<GalleryImage> createImageTable() {
        TableView<GalleryImage> table = new TableView<>();
        table.getStyleClass().add("app-table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<GalleryImage, String> titleCol = new TableColumn<>("标题");
        titleCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getTitle()));
        titleCol.setMinWidth(150);

        TableColumn<GalleryImage, String> categoryCol = new TableColumn<>("分类");
        categoryCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCategory()));
        categoryCol.setMinWidth(80);
        categoryCol.setMaxWidth(120);

        TableColumn<GalleryImage, String> sizeCol = new TableColumn<>("大小");
        sizeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDisplayFileSize()));
        sizeCol.setMinWidth(80);
        sizeCol.setMaxWidth(100);

        TableColumn<GalleryImage, String> resCol = new TableColumn<>("分辨率");
        resCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getDisplayResolution()));
        resCol.setMinWidth(100);
        resCol.setMaxWidth(140);

        TableColumn<GalleryImage, String> dateCol = new TableColumn<>("上传时间");
        dateCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getCreatedAt()));
        dateCol.setMinWidth(120);

        @SuppressWarnings("unchecked")
        TableColumn<GalleryImage, ?>[] cols = new TableColumn[] { titleCol, categoryCol, sizeCol, resCol, dateCol };
        table.getColumns().addAll(cols);

        // 双击查看详情
        table.setRowFactory(tv -> {
            var row = new javafx.scene.control.TableRow<GalleryImage>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    openDetailDialog(row.getItem());
                }
            });
            return row;
        });

        return table;
    }

    // ==================== 风物卷视图 ====================

    private void showCategories() {
        VBox page = new VBox(12);

        // 内容容器：索引视图和详情视图互斥切换
        VBox catContainer = new VBox();
        VBox.setVgrow(catContainer, Priority.ALWAYS);

        // ===== 索引视图：分类卡片网格 =====
        Label title = new Label("风物卷");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        Label subtitle = new Label("按分类浏览你的图片收藏");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");

        FlowPane categoryGrid = new FlowPane(16, 16);
        categoryGrid.getStyleClass().add("gallery-image-grid");

        ScrollPane categoryScrollPane = new ScrollPane(categoryGrid);
        categoryScrollPane.setFitToWidth(true);
        categoryScrollPane.setFitToHeight(true);
        categoryScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        categoryScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(categoryScrollPane, Priority.ALWAYS);

        int userId = appContext.getUserSession().getUserId();
        for (String cat : categories) {
            if ("全部".equals(cat)) continue;
            VBox categoryCard = createCategoryCard(cat, userId);
            categoryGrid.getChildren().add(categoryCard);
        }

        VBox indexView = new VBox(12, title, subtitle, categoryScrollPane);
        VBox.setVgrow(indexView, Priority.ALWAYS);

        // ===== 详情视图：分类下的收藏图片 =====
        HBox detailHeader = new HBox(8);
        detailHeader.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("< 返回");
        backBtn.setStyle("-fx-font-size: 12px; -fx-background-color: transparent; -fx-text-fill: -primary; -fx-cursor: hand; -fx-padding: 2 6;");
        Label detailTitle = new Label();
        detailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        Label detailCount = new Label();
        detailCount.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");
        detailHeader.getChildren().addAll(backBtn, detailTitle, detailCount);

        FlowPane detailImageGrid = new FlowPane(16, 16);
        detailImageGrid.getStyleClass().add("gallery-image-grid");

        ScrollPane detailScrollPane = new ScrollPane(detailImageGrid);
        detailScrollPane.setFitToWidth(true);
        detailScrollPane.setFitToHeight(true);
        detailScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        detailScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(detailScrollPane, Priority.ALWAYS);

        VBox detailEmptyState = createEmptyState(GalleryEmptyState.categoriesEmpty());

        VBox detailView = new VBox(8);
        VBox.setVgrow(detailView, Priority.ALWAYS);
        detailView.getChildren().addAll(detailHeader, detailScrollPane, detailEmptyState);

        // 初始显示索引视图
        catContainer.getChildren().add(indexView);

        // 返回按钮 → 切换回索引视图
        backBtn.setOnAction(e -> {
            catContainer.getChildren().setAll(indexView);
            // 刷新分类卡片计数
            refreshCategoryCardCounts(categoryGrid, userId);
        });

        // 点击分类卡片 → 切换到详情视图
        for (javafx.scene.Node node : categoryGrid.getChildren()) {
            if (node instanceof VBox card) {
                String cat = (String) card.getUserData();
                if (cat == null) continue;
                final String category = cat;
                card.setOnMouseClicked(e -> {
                    detailTitle.setText(category);
                    detailCount.setText("");
                    viewModel.loadFavoriteImagesByCategory(category);
                    catContainer.getChildren().setAll(detailView);
                });
            }
        }

        // 页面级别监听图片列表变化，刷新详情视图网格
        viewModel.getImages().addListener((javafx.collections.ListChangeListener<GalleryImage>) c -> {
            Platform.runLater(() -> {
                refreshImageGrid(detailImageGrid, detailEmptyState);
                detailCount.setText(viewModel.getImages().size() + " 张");
            });
        });

        page.getChildren().add(catContainer);
        addPage("categories", page);
    }

    /** 创建分类卡片（收藏图片计数） */
    private VBox createCategoryCard(String category, int userId) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPrefSize(160, 120);
        card.setStyle(
            "-fx-background-color: -bg-card; " +
            "-fx-background-radius: 12px; " +
            "-fx-border-color: -border-light; " +
            "-fx-border-width: 0.5px; " +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1); " +
            "-fx-cursor: hand;"
        );

        String icon = switch (category) {
            case "风景" -> "\u2600";   // ☀
            case "自然" -> "\u2618";   // ☘
            case "美食" -> "\u2615";   // ☕
            case "人物" -> "\u263A";   // ☺
            case "建筑" -> "\u2302";   // ⌂
            default -> "\u25C6";       // ◆
        };

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 32px; -fx-text-fill: -primary; -fx-opacity: 0.7;");

        Label nameLabel = new Label(category);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        // 分类统计：仅计数收藏图片
        int count = 0;
        try {
            count = appContext.getGalleryService().getFavoriteImagesByCategory(userId, category).size();
        } catch (Exception ignored) {}

        Label countLabel = new Label(count + " 张");
        countLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");

        card.getChildren().addAll(iconLabel, nameLabel, countLabel);
        card.setUserData(category);

        // 悬停效果
        card.setOnMouseEntered(e -> {
            card.setStyle(
                "-fx-background-color: -bg-card; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: -primary; " +
                "-fx-border-width: 1px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 4); " +
                "-fx-cursor: hand; -fx-translate-y: -2;"
            );
        });
        card.setOnMouseExited(e -> {
            card.setStyle(
                "-fx-background-color: -bg-card; " +
                "-fx-background-radius: 12px; " +
                "-fx-border-color: -border-light; " +
                "-fx-border-width: 0.5px; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 2, 0, 0, 1); " +
                "-fx-cursor: hand;"
            );
        });

        return card;
    }

    /** 刷新分类卡片的收藏图片计数 */
    private void refreshCategoryCardCounts(FlowPane categoryGrid, int userId) {
        for (javafx.scene.Node node : categoryGrid.getChildren()) {
            if (!(node instanceof VBox card)) continue;
            String catName = (String) card.getUserData();
            if (catName == null) continue;
            Label countLabel = null;
            for (javafx.scene.Node child : card.getChildren()) {
                if (child instanceof Label lbl) {
                    String style = lbl.getStyle();
                    if (style != null && style.contains("-fx-font-size: 11px") && style.contains("-text-tertiary")) {
                        countLabel = lbl;
                    }
                }
            }
            if (countLabel != null) {
                int count = 0;
                try {
                    count = appContext.getGalleryService().getFavoriteImagesByCategory(userId, catName).size();
                } catch (Exception ignored) {}
                countLabel.setText(count + " 张");
            }
        }
    }

    // ==================== 择景视图 ====================

    private void showFavorites() {
        VBox page = new VBox(8);

        Label title = new Label("择景");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        // 收藏图片网格
        FlowPane imageGrid = new FlowPane(16, 16);
        imageGrid.getStyleClass().add("gallery-image-grid");

        ScrollPane favScrollPane = new ScrollPane(imageGrid);
        favScrollPane.setFitToWidth(true);
        favScrollPane.setFitToHeight(true);
        favScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        favScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        favScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(favScrollPane, Priority.ALWAYS);

        // 空状态
        VBox emptyState = createEmptyState(GalleryEmptyState.favoritesEmpty());

        page.getChildren().addAll(title, favScrollPane, emptyState);
        addPage("favorites", page);

        // 加载收藏数据
        viewModel.loadFavoriteImages();

        viewModel.getImages().addListener((javafx.collections.ListChangeListener<GalleryImage>) c -> {
            Platform.runLater(() -> refreshImageGrid(imageGrid, emptyState));
        });

        refreshImageGrid(imageGrid, emptyState);
    }

    // ==================== 图集视图 ====================

    private void showAlbums() {
        VBox page = new VBox(8);

        Label title = new Label("图集");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        // 内容容器：索引视图和详情视图互斥切换
        VBox albumContainer = new VBox();
        VBox.setVgrow(albumContainer, Priority.ALWAYS);

        // ===== 索引视图：图集列表 =====
        ListView<javafx.scene.Node> albumIndexListView = new ListView<>();
        albumIndexListView.getStyleClass().add("playlist-list");
        VBox.setVgrow(albumIndexListView, Priority.ALWAYS);

        // ===== 详情视图：图集图片网格 =====
        final int[] currentAlbumId = { -1 };

        HBox detailHeader = new HBox(8);
        detailHeader.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("< 返回");
        backBtn.setStyle("-fx-font-size: 12px; -fx-background-color: transparent; -fx-text-fill: -primary; -fx-cursor: hand; -fx-padding: 2 6;");
        Label detailTitle = new Label();
        detailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        Label detailCount = new Label();
        detailCount.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary;");

        detailHeader.getChildren().addAll(backBtn, detailTitle, detailCount);

        FlowPane detailImageGrid = new FlowPane(16, 16);
        detailImageGrid.getStyleClass().add("gallery-image-grid");

        ScrollPane albumDetailScrollPane = new ScrollPane(detailImageGrid);
        albumDetailScrollPane.setFitToWidth(true);
        albumDetailScrollPane.setFitToHeight(true);
        albumDetailScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        albumDetailScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        albumDetailScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(albumDetailScrollPane, Priority.ALWAYS);

        VBox detailEmptyState = createEmptyState(GalleryEmptyState.categoriesEmpty());

        VBox detailView = new VBox(8);
        VBox.setVgrow(detailView, Priority.ALWAYS);
        detailView.getChildren().addAll(detailHeader, albumDetailScrollPane, detailEmptyState);

        // ===== 切换逻辑 =====
        albumContainer.getChildren().add(albumIndexListView);

        // 点击图集索引 → 替换为详情视图
        albumIndexListView.setOnMousePressed(e -> {
            int selIdx = albumIndexListView.getSelectionModel().getSelectedIndex();
            if (selIdx < 0) return;

            // 择景（第一项）
            if (selIdx == 0) {
                currentAlbumId[0] = -1;
                detailTitle.setText("择景");
                detailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #ef4444;");
                viewModel.loadFavoriteImages();
                detailCount.setText(viewModel.getImages().size() + " 张");
            } else {
                int albumIdx = selIdx - 1;
                List<ImageAlbum> albums = viewModel.getAlbums();
                if (albumIdx < albums.size()) {
                    ImageAlbum album = albums.get(albumIdx);
                    currentAlbumId[0] = album.getId();
                    detailTitle.setText(album.getName());
                    detailTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
                    detailCount.setText(album.getImageCount() + " 张");
                    viewModel.loadAlbumImages(album.getId());
                }
            }

            albumContainer.getChildren().setAll(detailView);
        });

        // 返回按钮 → 替换回索引视图
        backBtn.setOnAction(e -> {
            albumContainer.getChildren().setAll(albumIndexListView);
        });

        // 新建图集按钮
        AppButton newAlbumBtn = new AppButton("新建图集", AppButton.Style.PRIMARY);
        newAlbumBtn.setMaxWidth(Double.MAX_VALUE);
        newAlbumBtn.setOnAction(e -> {
            AlbumDialog dialog = new AlbumDialog("新建图集", null);
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                String name = dialog.getAlbumName();
                String desc = dialog.getAlbumDescription();
                if (!name.isEmpty()) viewModel.createAlbum(name, desc);
            }
        });

        // 监听图集列表变化，刷新索引列表
        viewModel.getAlbums().addListener((javafx.collections.ListChangeListener<ImageAlbum>) c -> {
            Platform.runLater(() -> refreshAlbumIndexList(albumIndexListView));
        });

        // 初始加载图集
        viewModel.loadAlbums();

        // 监听图集图片变化，刷新详情视图网格
        viewModel.getAlbumImages().addListener((javafx.collections.ListChangeListener<GalleryImage>) c -> {
            Platform.runLater(() -> refreshAlbumImageGrid(detailImageGrid, detailEmptyState));
        });

        // 页面级别监听 images 变化，择景详情视图需要刷新
        viewModel.getImages().addListener((javafx.collections.ListChangeListener<GalleryImage>) c -> {
            Platform.runLater(() -> {
                if (currentAlbumId[0] == -1) {
                    refreshImageGrid(detailImageGrid, detailEmptyState);
                    detailCount.setText(viewModel.getImages().size() + " 张");
                }
            });
        });

        page.getChildren().addAll(title, albumContainer, newAlbumBtn);
        addPage("albums", page);
    }

    /** 刷新图集索引列表 */
    private void refreshAlbumIndexList(ListView<javafx.scene.Node> listView) {
        listView.getItems().clear();

        // 择景项（置顶）
        HBox favItem = new HBox(10);
        favItem.setAlignment(Pos.CENTER_LEFT);
        favItem.setPadding(new Insets(8, 12, 8, 12));
        favItem.setStyle("-fx-cursor: hand;");
        Label favIcon = new Label("\u2665");
        favIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: #ef4444; -fx-pref-width: 20; -fx-alignment: center;");
        VBox favText = new VBox(2);
        Label favName = new Label("择景");
        favName.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #ef4444;");
        Label favCount = new Label(viewModel.getImages().size() + " 张");
        favCount.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
        favText.getChildren().addAll(favName, favCount);
        favItem.getChildren().addAll(favIcon, favText);
        listView.getItems().add(favItem);

        // 图集项
        for (ImageAlbum album : viewModel.getAlbums()) {
            HBox albumItem = new HBox(10);
            albumItem.setAlignment(Pos.CENTER_LEFT);
            albumItem.setPadding(new Insets(8, 12, 8, 12));
            albumItem.setStyle("-fx-cursor: hand;");

            Label albumIcon = new Label("\u266A");
            albumIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: -text-secondary; -fx-pref-width: 20; -fx-alignment: center;");

            VBox albumText = new VBox(2);
            HBox.setHgrow(albumText, Priority.ALWAYS);
            Label albumName = new Label(album.getName());
            albumName.setStyle("-fx-font-size: 13px; -fx-font-weight: 500; -fx-text-fill: -text-primary;");
            Label albumCount = new Label(album.getImageCount() + " 张");
            albumCount.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary;");
            albumText.getChildren().addAll(albumName, albumCount);

            Label deleteLabel = new Label("x");
            deleteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-cursor: hand; -fx-padding: 0 4; -fx-alignment: center-right;");
            deleteLabel.setOnMouseEntered(ev -> deleteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 0 4; -fx-alignment: center-right;"));
            deleteLabel.setOnMouseExited(ev -> deleteLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -text-tertiary; -fx-cursor: hand; -fx-padding: 0 4; -fx-alignment: center-right;"));
            deleteLabel.setOnMousePressed(ev -> {
                ev.consume();
                boolean confirmed = appContext.getDialogService().showConfirm("确认删除", "确定要删除图集「" + album.getName() + "」吗？");
                if (confirmed) viewModel.deleteAlbum(album.getId());
            });

            albumItem.getChildren().addAll(albumIcon, albumText, deleteLabel);
            listView.getItems().add(albumItem);
        }
    }

    /** 刷新图集详情中的图片网格 */
    private void refreshAlbumImageGrid(FlowPane imageGrid, VBox emptyState) {
        imageGrid.getChildren().clear();
        List<GalleryImage> images = viewModel.getAlbumImages();

        if (images.isEmpty()) {
            imageGrid.setVisible(false);
            imageGrid.setManaged(false);
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            imageGrid.setVisible(true);
            imageGrid.setManaged(true);
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            for (GalleryImage image : images) {
                ImageCard card = new ImageCard(image,
                    () -> openDetailDialog(image),
                    () -> viewModel.toggleFavorite(image)
                );
                imageGrid.getChildren().add(card);
            }
        }
    }

    // ==================== 在线寻图视图 ====================

    private void showOnlineSearch() {
        VBox page = new VBox(8);

        // ===== 第一行：搜索栏 =====
        HBox toolbar = new HBox(12);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        onlineSearchField = new TextField();
        onlineSearchField.setPromptText("搜索在线图片...");
        onlineSearchField.getStyleClass().add("search-box");
        onlineSearchField.setPrefWidth(280);
        onlineSearchField.setOnAction(e -> doOnlineSearch(onlineSearchField.getText()));

        AppButton searchBtn = new AppButton("搜索", AppButton.Style.PRIMARY);
        searchBtn.setOnAction(e -> doOnlineSearch(onlineSearchField.getText()));

        // 刷新按钮：获取新的图片内容（使用随机页码）
        Button refreshBtn = new Button("⟳");
        refreshBtn.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: -text-secondary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-border-color: -border-light; -fx-border-width: 1px;");
        refreshBtn.setOnMouseEntered(e -> refreshBtn.setStyle("-fx-font-size: 14px; -fx-background-color: -bg-hover; -fx-text-fill: -text-primary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-border-color: -border-light; -fx-border-width: 1px;"));
        refreshBtn.setOnMouseExited(e -> refreshBtn.setStyle("-fx-font-size: 14px; -fx-background-color: transparent; -fx-text-fill: -text-secondary; -fx-background-radius: 4; -fx-padding: 4 8; -fx-cursor: hand; -fx-border-color: -border-light; -fx-border-width: 1px;"));
        refreshBtn.setOnAction(e -> doOnlineRefresh());

        HBox errorBar = createErrorBar();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(onlineSearchField, searchBtn, refreshBtn, spacer, errorBar);

        // ===== 第二行：分类标签栏 =====
        HBox categoryBar = new HBox(6);
        categoryBar.setAlignment(Pos.CENTER_LEFT);
        Label categoryLabel = new Label("分类：");
        categoryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-padding: 0 4 0 0;");
        categoryBar.getChildren().add(categoryLabel);
        for (String cat : categories) {
            HBox tab = createOnlineCategoryTab(cat, cat.equals("全部"));
            categoryBar.getChildren().add(tab);
        }

        // ===== 第三行：平台筛选 =====
        HBox providerBar = new HBox(6);
        providerBar.setAlignment(Pos.CENTER_LEFT);
        Label providerLabel = new Label("来源：");
        providerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-tertiary; -fx-padding: 0 4 0 0;");
        providerBar.getChildren().add(providerLabel);

        ToggleButton allProviderBtn = new ToggleButton("全部");
        allProviderBtn.setSelected(true);
        allProviderBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
        allProviderBtn.setOnAction(e -> {
            onlineProvider = null;
            providerBar.getChildren().forEach(n -> {
                if (n instanceof ToggleButton tb && tb != allProviderBtn) tb.setSelected(false);
            });
            allProviderBtn.setSelected(true);
        });
        providerBar.getChildren().add(allProviderBtn);

        List<ImageProvider> providers = appContext.getOnlineImageService().getProviders();
        for (ImageProvider p : providers) {
            ToggleButton btn = new ToggleButton(p.getProviderName());
            btn.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");
            btn.setUserData(p.getProviderId());
            btn.setOnAction(e -> {
                providerBar.getChildren().forEach(n -> {
                    if (n instanceof ToggleButton tb && tb != btn) tb.setSelected(false);
                });
                if (btn.isSelected()) {
                    onlineProvider = p.getProviderId();
                } else {
                    onlineProvider = null;
                    allProviderBtn.setSelected(true);
                }
            });
            providerBar.getChildren().add(btn);
        }

        // ===== 加载提示 =====
        Label loadingLabel = new Label("正在搜索...");
        loadingLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -primary;");
        loadingLabel.setVisible(false);
        loadingLabel.setManaged(false);

        // ===== 在线图片网格（仅此区域可滚动） =====
        FlowPane imageGrid = new FlowPane(16, 16);
        imageGrid.getStyleClass().add("gallery-image-grid");

        ScrollPane imageScrollPane = new ScrollPane(imageGrid);
        imageScrollPane.setFitToWidth(true);
        imageScrollPane.setFitToHeight(true);
        imageScrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        imageScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        imageScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(imageScrollPane, Priority.ALWAYS);

        VBox emptyState = createEmptyState(GalleryEmptyState.noSearchResults());

        page.getChildren().addAll(toolbar, categoryBar, providerBar, loadingLabel, imageScrollPane, emptyState);
        addPage("onlineSearch", page);

        // 监听搜索结果变化
        viewModel.getOnlineImages().addListener((javafx.collections.ListChangeListener<GalleryImage>) c -> {
            loadingLabel.setVisible(false);
            loadingLabel.setManaged(false);
            Platform.runLater(() -> {
                imageGrid.getChildren().clear();
                List<GalleryImage> images = viewModel.getOnlineImages();
                if (images.isEmpty()) {
                    imageGrid.setVisible(false);
                    imageGrid.setManaged(false);
                    emptyState.setVisible(true);
                    emptyState.setManaged(true);
                } else {
                    imageGrid.setVisible(true);
                    imageGrid.setManaged(true);
                    emptyState.setVisible(false);
                    emptyState.setManaged(false);
                    for (GalleryImage image : images) {
                        if (image.getCategory() == null || image.getCategory().isEmpty()) {
                            image.setCategory(onlineCategory.equals("全部") ? guessCategory(image) : onlineCategory);
                        }
                        ImageCard card = new ImageCard(image,
                            () -> openDetailDialog(image),
                            () -> viewModel.favoriteOnlineImage(image, onlineCategory)
                        );
                        String sourceName = image.getSource() != null ? image.getSource() : "online";
                        Label sourceBadge = new Label(sourceName);
                        sourceBadge.setStyle("-fx-font-size: 9px; -fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 4; -fx-padding: 1 5;");
                        StackPane.setAlignment(sourceBadge, Pos.BOTTOM_RIGHT);
                        StackPane.setMargin(sourceBadge, new Insets(0, 6, 28, 0));
                        Label downloadIcon = new Label("\u2193");
                        downloadIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-opacity: 0; -fx-cursor: hand; -fx-padding: 4; -fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 4;");
                        StackPane.setAlignment(downloadIcon, Pos.TOP_LEFT);
                        StackPane.setMargin(downloadIcon, new Insets(6));
                        card.setOnMouseEntered(ev -> downloadIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-opacity: 0.9; -fx-cursor: hand; -fx-padding: 4; -fx-background-color: rgba(0,0,0,0.5); -fx-background-radius: 4;"));
                        card.setOnMouseExited(ev -> downloadIcon.setStyle("-fx-font-size: 14px; -fx-text-fill: white; -fx-opacity: 0; -fx-cursor: hand; -fx-padding: 4; -fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 4;"));
                        downloadIcon.setOnMouseClicked(ev -> {
                            ev.consume();
                            viewModel.downloadOnlineImage(image, onlineCategory);
                        });
                        card.getChildren().addAll(sourceBadge, downloadIcon);
                        imageGrid.getChildren().add(card);
                    }
                }
            });
        });

        viewModel.loadingProperty().addListener((obs, old, val) -> {
            loadingLabel.setVisible(val);
            loadingLabel.setManaged(val);
        });
    }

    /** 创建在线搜索分类标签 */
    private HBox createOnlineCategoryTab(String text, boolean active) {
        HBox tab = new HBox();
        tab.setAlignment(Pos.CENTER);
        tab.setStyle(active
            ? "-fx-text-fill: -primary; -fx-font-weight: 600; -fx-background-color: -primary-light; -fx-background-radius: 6px; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;"
            : "-fx-text-fill: -text-secondary; -fx-background-color: transparent; -fx-background-radius: 6px; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;"
        );
        Label label = new Label(text);
        label.setStyle(active
            ? "-fx-font-size: 12px; -fx-text-fill: -primary; -fx-font-weight: 600;"
            : "-fx-font-size: 12px; -fx-text-fill: -text-secondary;"
        );
        tab.getChildren().add(label);
        tab.setOnMouseEntered(e -> {
            if (!tab.getStyleClass().contains("tab-active")) {
                tab.setStyle("-fx-text-fill: -text-primary; -fx-background-color: -bg-hover; -fx-background-radius: 6px; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");
            }
        });
        tab.setOnMouseExited(e -> {
            if (!tab.getStyleClass().contains("tab-active")) {
                tab.setStyle("-fx-text-fill: -text-secondary; -fx-background-color: transparent; -fx-background-radius: 6px; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");
            }
        });
        tab.setOnMouseClicked(e -> {
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) tab.getParent();
            if (parent != null) {
                for (javafx.scene.Node sibling : parent.getChildren()) {
                    if (sibling instanceof HBox t) {
                        t.getStyleClass().remove("tab-active");
                        for (javafx.scene.Node node : t.getChildren()) {
                            if (node instanceof Label lbl) {
                                lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");
                            }
                        }
                        t.setStyle("-fx-text-fill: -text-secondary; -fx-background-color: transparent; -fx-background-radius: 6px; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");
                    }
                }
            }
            tab.getStyleClass().add("tab-active");
            for (javafx.scene.Node node : tab.getChildren()) {
                if (node instanceof Label lbl) {
                    lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: -primary; -fx-font-weight: 600;");
                }
            }
            tab.setStyle("-fx-text-fill: -primary; -fx-font-weight: 600; -fx-background-color: -primary-light; -fx-background-radius: 6px; -fx-padding: 4 12; -fx-font-size: 12px; -fx-cursor: hand;");
            onlineCategory = text;
        });
        if (active) {
            tab.getStyleClass().add("tab-active");
        }
        return tab;
    }

    /** 执行在线搜索（组合分类+关键词，从第1页开始） */
    private void doOnlineSearch(String userInput) {
        String keyword = userInput != null ? userInput.trim() : "";
        String searchKeyword;
        if ("全部".equals(onlineCategory)) {
            searchKeyword = keyword;
        } else {
            searchKeyword = keyword.isEmpty() ? onlineCategory : onlineCategory + " " + keyword;
        }
        if (searchKeyword.isEmpty()) {
            appContext.getDialogService().showInfo("提示", "请选择分类或输入搜索关键词");
            return;
        }
        appContext.getOnlineImageService().clearCache();
        if (onlineProvider != null) {
            viewModel.searchOnlineByProvider(searchKeyword, onlineProvider);
        } else {
            viewModel.searchOnline(searchKeyword);
        }
    }

    /** 执行在线刷新（使用随机页码获取新内容） */
    private void doOnlineRefresh() {
        String keyword = onlineSearchField != null ? onlineSearchField.getText().trim() : "";
        String searchKeyword;
        if ("全部".equals(onlineCategory)) {
            searchKeyword = keyword;
        } else {
            searchKeyword = keyword.isEmpty() ? onlineCategory : onlineCategory + " " + keyword;
        }
        if (searchKeyword.isEmpty()) {
            appContext.getDialogService().showInfo("提示", "请先选择分类或进行搜索");
            return;
        }
        appContext.getOnlineImageService().clearCache();
        if (onlineProvider != null) {
            viewModel.refreshOnlineByProvider(searchKeyword, onlineProvider);
        } else {
            viewModel.refreshOnline(searchKeyword);
        }
    }

    /** 根据图片标题/关键词智能推断分类 */
    private String guessCategory(GalleryImage image) {
        String text = (image.getTitle() != null ? image.getTitle() : "") + " " + (image.getSubCategory() != null ? image.getSubCategory() : "");
        text = text.toLowerCase();
        if (text.matches(".*[\u5c71\u6e56\u6d77\u57ce\u5e02\u98ce\u5149].*") || text.matches(".*sunset|sunrise|mountain|lake|ocean|city|landscape.*")) return "风景";
        if (text.matches(".*[\u82b1\u6811\u68ee\u6797\u690d\u7269].*") || text.matches(".*flower|tree|forest|plant|nature.*")) return "自然";
        if (text.matches(".*[\u5496\u5561\u86cb\u7cd5\u9910\u5385\u6599\u7406].*") || text.matches(".*coffee|cake|food|restaurant|cuisine.*")) return "美食";
        if (text.matches(".*[\u4eba\u50cf\u5973\u5b69\u7537\u4eba].*") || text.matches(".*portrait|person|woman|man|face.*")) return "人物";
        if (text.matches(".*[\u697c\u6865\u5854\u5bfa\u5e99].*") || text.matches(".*building|house|temple|tower|bridge|architect.*")) return "建筑";
        return "其他";
    }
    // ==================== 图库设置视图 ====================

    private void showSettings() {
        VBox page = new VBox(16);
        page.setPadding(new Insets(8));

        Label title = new Label("图库设置");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: -text-primary;");

        // 默认分类设置
        VBox defaultCategorySection = new VBox(8);
        defaultCategorySection.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 12px; -fx-padding: 16; -fx-border-color: -border-light; -fx-border-width: 0.5px;");

        Label catTitle = new Label("默认分类");
        catTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        Label catDesc = new Label("上传图片时使用的默认分类");
        catDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        ChoiceBox<String> defaultCatChoice = new ChoiceBox<>();
        defaultCatChoice.getItems().addAll("风景", "自然", "美食", "人物", "建筑", "其他");
        defaultCatChoice.setValue(appContext.getConfigStorage().get("galleryDefaultCategory", "风景"));
        defaultCatChoice.setStyle("-fx-font-size: 12px;");
        defaultCatChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) {
                appContext.getConfigStorage().set("galleryDefaultCategory", val);
            }
        });

        defaultCategorySection.getChildren().addAll(catTitle, catDesc, defaultCatChoice);

        // 缓存管理
        VBox cacheSection = new VBox(8);
        cacheSection.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 12px; -fx-padding: 16; -fx-border-color: -border-light; -fx-border-width: 0.5px;");

        Label cacheTitle = new Label("缓存管理");
        cacheTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");

        Label cacheSizeLabel = new Label("缩略图缓存：计算中...");
        cacheSizeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        AppButton clearCacheBtn = new AppButton("清理缓存", AppButton.Style.OUTLINE);
        clearCacheBtn.setOnAction(e -> {
            try {
                java.nio.file.Path thumbDir = appContext.getAppStorage().getGalleryCachePath().resolve("thumbnails");
                if (java.nio.file.Files.exists(thumbDir)) {
                    try (var stream = java.nio.file.Files.walk(thumbDir)) {
                        stream.sorted(java.util.Comparator.reverseOrder())
                            .map(java.nio.file.Path::toFile)
                            .forEach(File::delete);
                    }
                }
                cacheSizeLabel.setText("缩略图缓存：0 B");
            } catch (Exception ex) {
                cacheSizeLabel.setText("清理失败: " + ex.getMessage());
            }
        });

        cacheSection.getChildren().addAll(cacheTitle, cacheSizeLabel, clearCacheBtn);

        // 异步计算缓存大小
        appContext.getAsyncExecutor().execute(() -> {
            try {
                java.nio.file.Path thumbDir = appContext.getAppStorage().getGalleryCachePath().resolve("thumbnails");
                long size = 0;
                if (java.nio.file.Files.exists(thumbDir)) {
                    try (var stream = java.nio.file.Files.walk(thumbDir)) {
                        size = stream
                            .filter(java.nio.file.Files::isRegularFile)
                            .mapToLong(p -> {
                                try { return java.nio.file.Files.size(p); }
                                catch (Exception ex) { return 0; }
                            }).sum();
                    }
                }
                long finalSize = size;
                Platform.runLater(() -> {
                    String display;
                    if (finalSize < 1024) display = finalSize + " B";
                    else if (finalSize < 1024 * 1024) display = String.format("%.1f KB", finalSize / 1024.0);
                    else display = String.format("%.1f MB", finalSize / (1024.0 * 1024.0));
                    cacheSizeLabel.setText("缩略图缓存：" + display);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> cacheSizeLabel.setText("缩略图缓存：未知"));
            }
        });

        // 图片质量偏好
        VBox qualitySection = new VBox(8);
        qualitySection.setStyle("-fx-background-color: -bg-card; -fx-background-radius: 12px; -fx-padding: 16; -fx-border-color: -border-light; -fx-border-width: 0.5px;");

        Label qualityTitle = new Label("图片质量偏好");
        qualityTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -text-primary;");
        Label qualityDesc = new Label("缩略图生成质量");
        qualityDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: -text-secondary;");

        ChoiceBox<String> qualityChoice = new ChoiceBox<>();
        qualityChoice.getItems().addAll("高质量", "标准", "低质量");
        qualityChoice.setValue(appContext.getConfigStorage().get("galleryThumbnailQuality", "标准"));
        qualityChoice.setStyle("-fx-font-size: 12px;");
        qualityChoice.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null) {
                appContext.getConfigStorage().set("galleryThumbnailQuality", val);
            }
        });

        qualitySection.getChildren().addAll(qualityTitle, qualityDesc, qualityChoice);

        page.getChildren().addAll(title, defaultCategorySection, cacheSection, qualitySection);
        addPage("settings", page);
    }

    // ==================== 分类标签栏 ====================

    private HBox createCategoryTabs() {
        HBox tabs = new HBox(6);
        tabs.setAlignment(Pos.CENTER_LEFT);
        tabs.setPadding(new Insets(0, 0, 8, 0));
        tabs.setStyle("-fx-border-color: -border-light; -fx-border-width: 0 0 1 0;");

        for (String cat : categories) {
            HBox tab = createTabItem(cat, cat.equals("全部"));
            tabs.getChildren().add(tab);
        }

        return tabs;
    }

    private HBox createTabItem(String text, boolean active) {
        HBox tab = new HBox();
        tab.setAlignment(Pos.CENTER);
        tab.setStyle(active
            ? "-fx-text-fill: -primary; -fx-font-weight: 600; -fx-background-color: -primary-light; -fx-background-radius: 6px; -fx-padding: 6 14; -fx-font-size: 13px; -fx-cursor: hand;"
            : "-fx-text-fill: -text-secondary; -fx-background-color: transparent; -fx-background-radius: 6px; -fx-padding: 6 14; -fx-font-size: 13px; -fx-cursor: hand;"
        );

        Label label = new Label(text);
        label.setStyle(active
            ? "-fx-font-size: 13px; -fx-text-fill: -primary; -fx-font-weight: 600;"
            : "-fx-font-size: 13px; -fx-text-fill: -text-secondary;"
        );
        tab.getChildren().add(label);

        // 悬停效果
        tab.setOnMouseEntered(e -> {
            if (!tab.getStyleClass().contains("tab-active")) {
                tab.setStyle("-fx-text-fill: -text-primary; -fx-background-color: -bg-hover; -fx-background-radius: 6px; -fx-padding: 6 14; -fx-font-size: 13px; -fx-cursor: hand;");
            }
        });
        tab.setOnMouseExited(e -> {
            if (!tab.getStyleClass().contains("tab-active")) {
                tab.setStyle("-fx-text-fill: -text-secondary; -fx-background-color: transparent; -fx-background-radius: 6px; -fx-padding: 6 14; -fx-font-size: 13px; -fx-cursor: hand;");
            }
        });

        tab.setOnMouseClicked(e -> {
            // 通过父容器查找兄弟标签，不依赖 tabItems 字段（缓存加载后 tabItems 可能为空）
            javafx.scene.layout.Pane parent = (javafx.scene.layout.Pane) tab.getParent();
            if (parent != null) {
                for (javafx.scene.Node sibling : parent.getChildren()) {
                    if (sibling instanceof HBox t) {
                        t.getStyleClass().remove("tab-active");
                        for (javafx.scene.Node node : t.getChildren()) {
                            if (node instanceof Label lbl) {
                                lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -text-secondary;");
                            }
                        }
                        t.setStyle("-fx-text-fill: -text-secondary; -fx-background-color: transparent; -fx-background-radius: 6px; -fx-padding: 6 14; -fx-font-size: 13px; -fx-cursor: hand;");
                    }
                }
            }
            // 激活当前标签
            tab.getStyleClass().add("tab-active");
            for (javafx.scene.Node node : tab.getChildren()) {
                if (node instanceof Label lbl) {
                    lbl.setStyle("-fx-font-size: 13px; -fx-text-fill: -primary; -fx-font-weight: 600;");
                }
            }
            tab.setStyle("-fx-text-fill: -primary; -fx-font-weight: 600; -fx-background-color: -primary-light; -fx-background-radius: 6px; -fx-padding: 6 14; -fx-font-size: 13px; -fx-cursor: hand;");
            viewModel.filterByCategory(text);
        });

        if (active) {
            tab.getStyleClass().add("tab-active");
        }

        return tab;
    }

    // ==================== 批量操作栏 ====================

    private HBox createBatchBar(FlowPane imageGrid) {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);

        AppButton batchFavBtn = new AppButton("批量收藏", AppButton.Style.OUTLINE);
        AppButton batchDelBtn = new AppButton("批量删除", AppButton.Style.OUTLINE);
        AppButton batchAlbumBtn = new AppButton("加入图集", AppButton.Style.OUTLINE);

        batchFavBtn.setVisible(false);
        batchFavBtn.setManaged(false);
        batchDelBtn.setVisible(false);
        batchDelBtn.setManaged(false);
        batchAlbumBtn.setVisible(false);
        batchAlbumBtn.setManaged(false);

        Label selectionLabel = new Label();
        selectionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -primary; -fx-font-weight: 500;");
        selectionLabel.setVisible(false);
        selectionLabel.setManaged(false);

        // 收集选中的图片
        batchFavBtn.setOnAction(e -> {
            List<GalleryImage> selected = getSelectedImages(imageGrid);
            if (!selected.isEmpty()) viewModel.batchFavorite(selected);
        });

        batchDelBtn.setOnAction(e -> {
            List<GalleryImage> selected = getSelectedImages(imageGrid);
            if (!selected.isEmpty()) {
                boolean confirmed = appContext.getDialogService().showConfirm("确认删除", "确定要删除选中的 " + selected.size() + " 张图片吗？");
                if (confirmed) viewModel.batchDelete(selected);
            }
        });

        batchAlbumBtn.setOnAction(e -> {
            List<GalleryImage> selected = getSelectedImages(imageGrid);
            if (selected.isEmpty()) return;
            // 弹出图集选择对话框
            showAddToAlbumDialog(selected);
        });

        // 监听图片网格中卡片的选中状态
        imageGrid.getChildren().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) c -> {
            updateBatchBarState(imageGrid, batchFavBtn, batchDelBtn, batchAlbumBtn, selectionLabel);
        });

        bar.getChildren().addAll(batchFavBtn, batchDelBtn, batchAlbumBtn, selectionLabel);
        return bar;
    }

    /** 获取当前选中的图片列表 */
    private List<GalleryImage> getSelectedImages(FlowPane imageGrid) {
        List<GalleryImage> selected = new ArrayList<>();
        for (javafx.scene.Node node : imageGrid.getChildren()) {
            if (node instanceof ImageCard card && card.isSelected()) {
                selected.add(card.getImage());
            }
        }
        return selected;
    }

    private void updateBatchBarState(FlowPane imageGrid, AppButton favBtn, AppButton delBtn, AppButton albumBtn, Label selectionLabel) {
        int count = getSelectedImages(imageGrid).size();
        boolean hasSelection = count > 0;
        favBtn.setVisible(hasSelection);
        favBtn.setManaged(hasSelection);
        delBtn.setVisible(hasSelection);
        delBtn.setManaged(hasSelection);
        albumBtn.setVisible(hasSelection);
        albumBtn.setManaged(hasSelection);
        if (selectionLabel != null) {
            selectionLabel.setVisible(hasSelection);
            selectionLabel.setManaged(hasSelection);
            selectionLabel.setText("已选中 " + count + " 张");
        }
    }

    /** 弹出图集选择对话框 */
    private void showAddToAlbumDialog(List<GalleryImage> selectedImages) {
        List<ImageAlbum> albums = viewModel.getAlbums();
        if (albums.isEmpty()) {
            appContext.getDialogService().showInfo("提示", "还没有图集，请先创建一个图集");
            return;
        }

        javafx.scene.control.ChoiceDialog<ImageAlbum> dialog = new javafx.scene.control.ChoiceDialog<>(albums.get(0), albums);
        dialog.setTitle("加入图集");
        dialog.setHeaderText("选择要加入的图集");
        dialog.setContentText("图集：");

        Optional<ImageAlbum> result = dialog.showAndWait();
        result.ifPresent(album -> viewModel.batchAddToAlbum(album.getId(), selectedImages));
    }

    // ==================== 图片网格刷新 ====================

    private void refreshImageGrid(FlowPane imageGrid, VBox emptyState) {
        imageGrid.getChildren().clear();
        List<GalleryImage> images = viewModel.getImages();

        if (images.isEmpty()) {
            imageGrid.setVisible(false);
            imageGrid.setManaged(false);
            emptyState.setVisible(true);
            emptyState.setManaged(true);
        } else {
            imageGrid.setVisible(true);
            imageGrid.setManaged(true);
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            for (GalleryImage image : images) {
                ImageCard card = new ImageCard(image,
                    () -> openDetailDialog(image),
                    () -> viewModel.toggleFavorite(image)
                );
                imageGrid.getChildren().add(card);
            }
        }
    }

    // ==================== 空状态包装 ====================

    private VBox createEmptyState(GalleryEmptyState inner) {
        VBox wrapper = new VBox(inner);
        wrapper.setPadding(new Insets(60, 20, 60, 20));
        wrapper.setVisible(false);
        return wrapper;
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

    // ==================== 详情弹窗 ====================

    private void openDetailDialog(GalleryImage image) {
        List<GalleryImage> images = new ArrayList<>(viewModel.getImages());
        if (images.isEmpty() || !images.contains(image)) {
            List<GalleryImage> onlineImages = new ArrayList<>(viewModel.getOnlineImages());
            if (!onlineImages.isEmpty()) {
                images = onlineImages;
            } else {
                List<GalleryImage> albumImgs = new ArrayList<>(viewModel.getAlbumImages());
                if (!albumImgs.isEmpty()) {
                    images = albumImgs;
                }
            }
        }
        int index = images.indexOf(image);
        if (index < 0) index = 0;

        // 确保列表非空
        if (images.isEmpty()) {
            images = List.of(image);
            index = 0;
        }

        ImageDetailDialog dialog = new ImageDetailDialog(appContext, images, index,
            () -> viewModel.toggleFavorite(image),
            () -> viewModel.deleteImage(image.getId()),
            new ImageDetailDialog.OnAddToAlbumCallback() {
                @Override
                public List<ImageAlbum> getAlbums() {
                    return viewModel.getAlbums();
                }
                @Override
                public void addToAlbum(int albumId, GalleryImage img) {
                    viewModel.addImageToAlbum(albumId, img.getId());
                }
                @Override
                public void removeFromAlbum(int albumId, GalleryImage img) {
                    viewModel.removeImageFromAlbum(albumId, img.getId());
                }
                @Override
                public List<ImageAlbum> getAlbumsForImage(int imageId) {
                    return viewModel.getAlbumsForImage(imageId);
                }
            }
        );
        dialog.show();
    }

    // ==================== 上传图片 ====================

    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择图片");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("图片文件", "*.jpg", "*.jpeg", "*.png", "*.webp", "*.gif")
        );

        List<File> files = fileChooser.showOpenMultipleDialog(this.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                viewModel.uploadImage(file, viewModel.selectedCategoryProperty().get(), null);
            }
        }
    }
}
