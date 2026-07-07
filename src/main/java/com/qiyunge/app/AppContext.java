package com.qiyunge.app;

import com.qiyunge.application.auth.AuthService;
import com.qiyunge.application.face.FaceRecognitionService;
import com.qiyunge.application.service.AdminService;
import com.qiyunge.application.service.AsyncExecutor;
import com.qiyunge.application.service.AuditLogService;
import com.qiyunge.application.service.GalleryService;
import com.qiyunge.application.service.ImageProvider;
import com.qiyunge.application.service.JamendoProvider;
import com.qiyunge.application.service.LocalMusicProvider;
import com.qiyunge.application.service.LyricService;
import com.qiyunge.application.service.MusicPlayerService;
import com.qiyunge.application.service.MusicProviderRegistry;
import com.qiyunge.application.service.MusicService;
import com.qiyunge.application.service.UserService;
import com.qiyunge.application.service.NeteaseApiClient;
import com.qiyunge.application.service.NeteaseApiProcessManager;
import com.qiyunge.application.service.NeteaseMusicProvider;
import com.qiyunge.application.service.OnlineMusicService;
import com.qiyunge.application.service.OnlineImageService;
import com.qiyunge.application.service.PexelsProvider;
import com.qiyunge.application.service.PixabayProvider;
import com.qiyunge.application.service.UnsplashProvider;
import com.qiyunge.application.service.StatisticsService;
import com.qiyunge.application.service.WikimediaProvider;
import com.qiyunge.domain.entity.Song;
import com.qiyunge.infrastructure.database.DatabaseManager;
import com.qiyunge.infrastructure.repository.AuditLogRepository;
import com.qiyunge.infrastructure.repository.FavoriteSongRepository;
import com.qiyunge.infrastructure.repository.GalleryImageRepository;
import com.qiyunge.infrastructure.repository.ImageAlbumItemRepository;
import com.qiyunge.infrastructure.repository.ImageAlbumRepository;
import com.qiyunge.infrastructure.repository.UserImagePreferenceRepository;
import com.qiyunge.application.service.PlaylistService;
import com.qiyunge.infrastructure.repository.PlaylistRepository;
import com.qiyunge.infrastructure.repository.PlayHistoryRepository;
import com.qiyunge.infrastructure.repository.RegistrationRequestRepository;
import com.qiyunge.infrastructure.repository.SongRepository;
import com.qiyunge.infrastructure.repository.UserFaceDataRepository;
import com.qiyunge.infrastructure.repository.UserRepository;
import com.qiyunge.infrastructure.storage.AppStorage;
import com.qiyunge.infrastructure.storage.ConfigStorage;
import javafx.stage.Stage;

public class AppContext {

    private Stage primaryStage;
    private DatabaseManager databaseManager;
    private AppStorage appStorage;
    private ConfigStorage configStorage;
    private NavigationService navigationService;
    private ThemeService themeService;
    private DialogService dialogService;

    // Repositories
    private UserRepository userRepository;
    private AuditLogRepository auditLogRepository;
    private RegistrationRequestRepository registrationRequestRepository;
    private UserFaceDataRepository userFaceDataRepository;
    private SongRepository songRepository;
    private FavoriteSongRepository favoriteSongRepository;
    private PlayHistoryRepository playHistoryRepository;
    private PlaylistRepository playlistRepository;
    private GalleryImageRepository galleryImageRepository;
    private UserImagePreferenceRepository userImagePreferenceRepository;
    private ImageAlbumRepository imageAlbumRepository;
    private ImageAlbumItemRepository imageAlbumItemRepository;

    // Services
    private UserService userService;
    private AuditLogService auditLogService;
    private AdminService adminService;
    private AuthService authService;
    private FaceRecognitionService faceRecognitionService;
    private MusicService musicService;
    private MusicPlayerService musicPlayerService;
    private AsyncExecutor asyncExecutor;
    private PlaylistService playlistService;
    private StatisticsService statisticsService;
    private MusicProviderRegistry musicProviderRegistry;
    private OnlineMusicService onlineMusicService;
    private NeteaseApiProcessManager neteaseApiProcessManager;
    private LyricService lyricService;
    private GalleryService galleryService;
    private OnlineImageService onlineImageService;

    private UserSession userSession;

    /**
     * 分步初始化回调接口
     */
    public interface InitCallback {
        void onProgress(double progress, String status, String detail);
    }

    public void initialize() {
        initialize(null);
    }

    public void initialize(InitCallback callback) {
        long t0 = System.currentTimeMillis();

        // 1. 存储与数据库
        notify(callback, 0.05, "初始化存储目录...", "创建应用数据目录");
        appStorage = new AppStorage();
        appStorage.ensureDirectories();
        configStorage = new ConfigStorage(appStorage);
        System.out.println("[INIT] 1. 存储目录初始化: " + (System.currentTimeMillis() - t0) + "ms");

        long t1 = System.currentTimeMillis();
        notify(callback, 0.12, "初始化数据库...", "创建数据库连接与表结构");
        databaseManager = new DatabaseManager(appStorage);
        databaseManager.initialize();
        System.out.println("[INIT] 2. 数据库初始化: " + (System.currentTimeMillis() - t1) + "ms");

        // 2. 数据仓库
        long t2 = System.currentTimeMillis();
        notify(callback, 0.20, "初始化数据仓库...", "加载用户、音乐、图库等数据访问层");
        userRepository = new UserRepository(databaseManager);
        auditLogRepository = new AuditLogRepository(databaseManager);
        registrationRequestRepository = new RegistrationRequestRepository(databaseManager);
        userFaceDataRepository = new UserFaceDataRepository(databaseManager);
        songRepository = new SongRepository(databaseManager);
        favoriteSongRepository = new FavoriteSongRepository(databaseManager);
        playHistoryRepository = new PlayHistoryRepository(databaseManager);
        playlistRepository = new PlaylistRepository(databaseManager);
        galleryImageRepository = new GalleryImageRepository(databaseManager);
        userImagePreferenceRepository = new UserImagePreferenceRepository(databaseManager);
        imageAlbumRepository = new ImageAlbumRepository(databaseManager);
        imageAlbumItemRepository = new ImageAlbumItemRepository(databaseManager);
        System.out.println("[INIT] 3. 数据仓库初始化: " + (System.currentTimeMillis() - t2) + "ms");

        // 3. 核心服务
        long t3 = System.currentTimeMillis();
        notify(callback, 0.35, "初始化核心服务...", "用户管理、认证、审计");
        userService = new UserService(userRepository);
        auditLogService = new AuditLogService(auditLogRepository);
        asyncExecutor = new AsyncExecutor();
        adminService = new AdminService(userRepository, registrationRequestRepository, auditLogService, databaseManager);
        authService = new AuthService(databaseManager, userRepository, auditLogService, registrationRequestRepository);
        System.out.println("[INIT] 4. 核心服务初始化: " + (System.currentTimeMillis() - t3) + "ms");

        // 4. 人脸识别（延迟初始化，不阻塞启动）
        long t4 = System.currentTimeMillis();
        notify(callback, 0.45, "初始化人脸识别模块...", "后台加载中");
        faceRecognitionService = new FaceRecognitionService(appStorage, userFaceDataRepository);
        authService.setFaceRecognitionService(faceRecognitionService);
        System.out.println("[INIT] 5. 人脸识别初始化: " + (System.currentTimeMillis() - t4) + "ms");

        // 5. 音乐服务
        long t5 = System.currentTimeMillis();
        notify(callback, 0.55, "初始化音乐服务...", "本地音乐、在线音乐提供者");
        MusicProviderRegistry providerRegistry = new MusicProviderRegistry();

        musicService = new MusicService(songRepository, favoriteSongRepository, playHistoryRepository,
            providerRegistry, appStorage.getMusicAudioPath(), appStorage.getMusicCoverPath(), appStorage.getMusicLyricPath());

        providerRegistry.register(new LocalMusicProvider(musicService));
        providerRegistry.register(new JamendoProvider(), 50);

        musicPlayerService = new MusicPlayerService(this);
        playlistService = new PlaylistService(playlistRepository);
        statisticsService = new StatisticsService(userRepository, songRepository,
            favoriteSongRepository, playHistoryRepository, playlistRepository,
            registrationRequestRepository, galleryImageRepository, userImagePreferenceRepository);
        System.out.println("[INIT] 6. 音乐服务初始化: " + (System.currentTimeMillis() - t5) + "ms");

        // 6. 图库服务
        long t6 = System.currentTimeMillis();
        notify(callback, 0.65, "初始化图库服务...", "本地图库与在线图片源");
        galleryService = new GalleryService(galleryImageRepository, userImagePreferenceRepository,
            imageAlbumRepository, imageAlbumItemRepository, databaseManager, appStorage.getGalleryCachePath());

        onlineImageService = new OnlineImageService();
        registerImageProvider(new PexelsProvider(configStorage.get("pexelsApiKey", null)));
        registerImageProvider(new WikimediaProvider());
        registerImageProvider(new UnsplashProvider(configStorage.get("unsplashClientId", null)));
        registerImageProvider(new PixabayProvider(configStorage.get("pixabayApiKey", null)));
        System.out.println("[INIT] 7. 图库服务初始化: " + (System.currentTimeMillis() - t6) + "ms");

        // 7. 网易云 API（异步启动，不阻塞）
        long t7 = System.currentTimeMillis();
        notify(callback, 0.75, "启动网易云 API 服务...", "后台启动中，不阻塞主界面");
        neteaseApiProcessManager = new NeteaseApiProcessManager(appStorage);
        neteaseApiProcessManager.startAsync(urlOpt -> {
            urlOpt.ifPresent(url -> {
                NeteaseApiClient neteaseClient = new NeteaseApiClient(url);
                providerRegistry.register(new NeteaseMusicProvider(neteaseClient), 10);
                System.out.println("[INIT] 网易云音乐提供者已注册: " + url);
            });
        });
        System.out.println("[INIT] 8. 网易云 API 异步启动已提交: " + (System.currentTimeMillis() - t7) + "ms");

        OnlineMusicService onlineMusicService = new OnlineMusicService(providerRegistry);
        this.onlineMusicService = onlineMusicService;
        this.musicProviderRegistry = providerRegistry;

        lyricService = new LyricService(providerRegistry, asyncExecutor, appStorage.getAppDataPath().resolve("lyrics"));

        musicPlayerService.addOnPlaybackReady(song -> {
            asyncExecutor.execute(() -> {
                try {
                    musicService.updateSongFormatAndDuration(
                        song.getId(), song.getFormat(), song.getCodec(), song.getDuration());
                } catch (Exception e) {
                    System.err.println("Failed to update song metadata: " + e.getMessage());
                }
            });
        });

        // 8. UI 服务
        long t8 = System.currentTimeMillis();
        notify(callback, 0.88, "初始化界面服务...", "主题、导航、会话");
        themeService = new ThemeService(configStorage);
        navigationService = new NavigationService();
        dialogService = new DialogService();
        userSession = new UserSession();
        navigationService.setUserSession(userSession);
        System.out.println("[INIT] 9. UI 服务初始化: " + (System.currentTimeMillis() - t8) + "ms");

        // 9. 恢复配置
        long t9 = System.currentTimeMillis();
        notify(callback, 0.95, "恢复用户配置...", "音量、播放模式");
        String savedVolumeStr = configStorage.get("musicVolume", null);
        if (savedVolumeStr != null) {
            try {
                musicPlayerService.setVolume(Double.parseDouble(savedVolumeStr));
            } catch (NumberFormatException ignored) {}
        }
        String savedMode = configStorage.get("playMode", null);
        if (savedMode != null) {
            try {
                musicPlayerService.setPlayMode(MusicPlayerService.PlayMode.valueOf(savedMode));
            } catch (IllegalArgumentException ignored) {}
        }
        System.out.println("[INIT] 10. 配置恢复: " + (System.currentTimeMillis() - t9) + "ms");

        long total = System.currentTimeMillis() - t0;
        notify(callback, 1.0, "初始化完成", "欢迎回来");
        System.out.println("[INIT] ===== 初始化总耗时: " + total + "ms =====");
    }

    private void notify(InitCallback callback, double progress, String status, String detail) {
        if (callback != null) {
            callback.onProgress(progress, status, detail);
        }
    }

    public void shutdown() {
        if (userSession.isLoggedIn()) {
            var userOpt = userService.findById(userSession.getUserId());
            userOpt.ifPresent(auditLogService::logLogout);
        }
        if (musicPlayerService != null) {
            Song lastSong = musicPlayerService.getCurrentSong();
            if (lastSong != null) {
                configStorage.set("lastSongId", String.valueOf(lastSong.getId()));
                configStorage.set("lastSongUrl", lastSong.getUrl());
            }
            configStorage.set("musicVolume", String.valueOf(musicPlayerService.volumeProperty().get()));
            configStorage.set("playMode", musicPlayerService.getPlayMode().name());
            musicPlayerService.stopCurrent();
        }
        if (neteaseApiProcessManager != null) {
            neteaseApiProcessManager.stop();
        }
        if (musicProviderRegistry != null) {
            musicProviderRegistry.shutdown();
        }
        if (faceRecognitionService != null) {
            faceRecognitionService.shutdown();
        }
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
        }
        if (databaseManager != null) {
            configStorage.flush();
            databaseManager.close();
        }
    }

    private void registerImageProvider(ImageProvider provider) {
        if (provider.isConfigured()) {
            onlineImageService.registerProvider(provider);
            System.out.println("[AppContext] 在线图片源已注册: " + provider.getProviderName());
        } else {
            System.out.println("[AppContext] 在线图片源未配置，已跳过: " + provider.getProviderName());
        }
    }

    public Stage getPrimaryStage() { return primaryStage; }
    public void setPrimaryStage(Stage primaryStage) { this.primaryStage = primaryStage; }
    DatabaseManager getDatabaseManager() { return databaseManager; }
    public AppStorage getAppStorage() { return appStorage; }
    public StatisticsService getStatisticsService() { return statisticsService; }
    public ConfigStorage getConfigStorage() { return configStorage; }
    public NavigationService getNavigationService() { return navigationService; }
    public ThemeService getThemeService() { return themeService; }
    public DialogService getDialogService() { return dialogService; }

    public UserService getUserService() { return userService; }
    public AuditLogService getAuditLogService() { return auditLogService; }
    public AdminService getAdminService() { return adminService; }
    public AuthService getAuthService() { return authService; }
    public MusicService getMusicService() { return musicService; }
    public MusicPlayerService getMusicPlayerService() { return musicPlayerService; }
    public AsyncExecutor getAsyncExecutor() { return asyncExecutor; }
    public PlaylistService getPlaylistService() { return playlistService; }
    public MusicProviderRegistry getMusicProviderRegistry() { return musicProviderRegistry; }
    public OnlineMusicService getOnlineMusicService() { return onlineMusicService; }
    public LyricService getLyricService() { return lyricService; }
    public NeteaseApiProcessManager getNeteaseApiProcessManager() { return neteaseApiProcessManager; }
    public GalleryService getGalleryService() { return galleryService; }
    public OnlineImageService getOnlineImageService() { return onlineImageService; }

    public FaceRecognitionService getFaceRecognitionService() { return faceRecognitionService; }

    public UserSession getUserSession() { return userSession; }
}
