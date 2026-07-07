# 栖云阁 QiyunGe

一款基于 JavaFX 构建的桌面多媒体管理应用，集音乐播放、图片图库、人脸识别登录于一体，采用古典中式命名风格，打造沉浸式的个人数字空间。

## 功能概览

### 望云台（仪表盘）
- 用户数据统计总览：歌曲数、收藏数、播放历史、图库图片等
- 可视化数据卡片展示

### 听雨轩（音乐模块）
- **本地音乐**：扫描本地目录，自动识别歌曲格式与封面
- **在线音乐**：支持网易云音乐（需 NeteaseCloudMusicApi）、Jamendo 等在线音源
- **播放控制**：播放/暂停、上一曲/下一曲、进度拖拽、音量调节
- **播放模式**：顺序播放、随机播放、单曲循环
- **歌词显示**：支持 LRC 歌词解析与实时滚动高亮
- **播放队列**：可拖拽的播放列表面板
- **歌单管理**：创建、编辑、删除自定义歌单

### 拾光廊（图库模块）
- **本地图库**：导入并管理本地图片
- **在线图源**：支持 Pexels、Unsplash、Pixabay、Wikimedia Commons
- **相册管理**：创建相册，归类收藏图片
- **图片详情**：查看大图、图片信息

### 百趣园（娱乐模块）
- 休闲互动区域

### 吾庐（个人资料）
- 修改显示名称、头像颜色
- 修改密码
- 注销账户

### 阁务司（后台管理，仅管理员可见）
- 用户管理：查看、启用/禁用用户
- 注册审批：审核用户注册申请
- 审计日志：查看系统操作记录
- 默认管理员账号：`admin` / `admin123`（首次登录需修改密码）

### 云枢（设置）
- **主题切换**：晨雾、松林、暮色、月白 四种主题
- **在线图源 API 配置**：Pexels、Unsplash、Pixabay 等
- **网易云音乐 API 配置**

### 人脸识别登录
- 基于 OpenCV (JavaCV) 的人脸检测与识别
- 摄像头实时采集人脸样本
- LBPH 算法训练人脸模型
- 支持人脸识别快速登录

## 技术架构

### 技术栈
| 类别 | 技术 |
|------|------|
| 语言 | Java 21 |
| UI 框架 | JavaFX 21 |
| 数据库 | SQLite (WAL 模式) |
| 数据库迁移 | Flyway |
| 人脸识别 | OpenCV 4.7 (JavaCV) + Webcam Capture |
| 音频解码 | JLayer (MP3) |
| JSON 处理 | Jackson |
| 密码加密 | BCrypt |
| 图标 | Ikonli (Material Design Icons) |
| 日志 | Logback |
| 构建工具 | Maven |
| EXE 封装 | Launch4j |

### 项目结构

```
src/main/java/com/qiyunge/
├── Main.java                          # 应用入口
├── app/                               # 应用层
│   ├── AppContext.java                # 全局上下文与依赖注入
│   ├── AppLauncher.java              # JavaFX Application 启动器
│   ├── NavigationService.java        # 页面导航服务
│   ├── ThemeService.java             # 主题管理
│   ├── UserSession.java              # 用户会话
│   └── DialogService.java            # 对话框服务
├── application/                       # 业务逻辑层
│   ├── auth/AuthService.java         # 认证服务
│   ├── face/FaceRecognitionService.java  # 人脸识别服务
│   └── service/                      # 业务服务
│       ├── AdminService.java         # 管理服务
│       ├── GalleryService.java       # 图库服务
│       ├── MusicService.java         # 音乐服务
│       ├── MusicPlayerService.java   # 播放器服务
│       ├── OnlineImageService.java   # 在线图片服务
│       ├── OnlineMusicService.java   # 在线音乐服务
│       ├── PlaylistService.java      # 歌单服务
│       ├── StatisticsService.java    # 统计服务
│       └── UserService.java          # 用户服务
├── domain/                            # 领域模型层
│   ├── entity/                       # 实体类
│   └── model/                        # 值对象与DTO
├── infrastructure/                    # 基础设施层
│   ├── database/DatabaseManager.java # 数据库管理
│   ├── repository/                   # 数据仓库
│   ├── storage/                      # 存储管理
│   ├── player/JLayerAudioPlayer.java # 音频播放器
│   └── util/                         # 工具类
└── ui/                               # 界面层
    ├── shell/MainShell.java          # 主框架
    ├── splash/SplashView.java        # 启动页
    ├── login/                        # 登录与注册
    ├── dashboard/                    # 仪表盘
    ├── music/                        # 音乐页面
    ├── gallery/                      # 图库页面
    ├── entertainment/                # 娱乐页面
    ├── profile/                      # 个人资料
    ├── admin/                        # 后台管理
    ├── settings/                     # 设置页面
    ├── face/                         # 人脸识别对话框
    └── components/                   # 通用UI组件
```

### 架构设计

项目采用分层架构，职责清晰：

- **UI 层**：JavaFX 视图与 ViewModel，负责界面展示与用户交互
- **Application 层**：业务逻辑编排，协调基础设施与领域模型
- **Domain 层**：核心实体与业务模型
- **Infrastructure 层**：数据库、文件存储、外部 API 等技术实现

## 快速开始

### 环境要求

- **JDK 21+**（需设置 `JAVA_HOME` 环境变量）
- **Maven 3.8+**
- **Windows 10/11**（人脸识别与 EXE 打包仅支持 Windows x86_64）

### 编译运行

```bash
# 克隆项目
git clone https://github.com/QiyunGe/qiyunge.git
cd qiyunge

# 编译并运行（开发模式）
mvn clean javafx:run
```

### 打包发布

```bash
# 完整打包（生成 EXE + JRE + 工具的 ZIP 分发包）
mvn clean package
```

打包产物位于 `target/栖云阁.zip`，包含：
- `栖云阁.exe` — Windows 启动器
- `qiyunge.jar` — 主程序（含所有依赖）
- `jre/` — 捆绑的 Java 运行时
- `tools/NeteaseCloudMusicApi/` — 网易云音乐 API 服务
- `lib/` — 本地依赖（JLayer 等）

### 首次运行

1. 解压 `栖云阁.zip` 到任意目录
2. 双击 `栖云阁.exe` 启动
3. 应用会在 EXE 同级目录创建 `.qiyunge/` 数据目录
4. 首次启动自动初始化数据库，默认管理员：`admin` / `admin123`

## 主题系统

栖云阁提供四种精心设计的主题：

| 主题 | 名称 | 风格 |
|------|------|------|
| 🌅 | 晨雾 | 清新明亮，默认主题 |
| 🌲 | 松林 | 自然沉稳 |
| 🌇 | 暮色 | 温暖柔和 |
| 🌙 | 月白 | 深色护眼 |

## 在线服务配置

部分功能需要配置第三方 API 密钥，在「云枢」设置页面中填写：

| 服务 | 用途 | 获取方式 |
|------|------|----------|
| NeteaseCloudMusicApi | 网易云音乐 | 内置 Node.js 服务，需安装 Node.js |
| Pexels API | 在线图片 | [pexels.com/api](https://www.pexels.com/api/) |
| Unsplash API | 在线图片 | [unsplash.com/developers](https://unsplash.com/developers) |
| Pixabay API | 在线图片 | [pixabay.com/api/docs](https://pixabay.com/api/docs/) |

## 数据库

使用 SQLite 作为本地数据库，通过 Flyway 管理数据库版本迁移：

| 版本 | 说明 |
|------|------|
| V1 | 初始化表结构（用户、歌曲、图库、设置等） |
| V2 | 新增歌曲格式字段 |
| V3 | 新增图库相册功能 |
| V4 | 新增级联删除约束 |
| V5 | 新增人脸识别数据表 |

## 数据存储

所有用户数据存储在应用同级目录的 `.qiyunge/` 文件夹下：

```
.qiyunge/
├── qiyunge.db        # SQLite 数据库
├── config.properties # 应用配置
├── face_data/        # 人脸识别模型数据
├── gallery_cache/    # 图库缓存
├── music_audio/      # 音乐文件缓存
├── music_cover/      # 音乐封面缓存
├── music_lyric/      # 歌词缓存
└── logs/             # 应用日志
```

## 许可证

本项目仅供学习交流使用。
