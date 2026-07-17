# 栖云阁 QiyunGe 更新日志

本项目遵循 **语义化版本（Semantic Versioning）** 规范，版本号格式为 `主版本.次版本.修订版本`。

---

## [v1.1.0] - 2026-07-17

### ✨ 新增功能

#### 背景图片系统
- 每套主题均配备专属背景图片，采用「背景图 + 半透明覆盖层」双层结构
- 新增 3 张高清背景图资源：
  - [bg-morning-mist.jpg](file:///D:/QiyunGe/src/main/resources/images/backgrounds/bg-morning-mist.jpg) — 晨雾主题
  - [bg-bamboo.jpg](file:///D:/QiyunGe/src/main/resources/images/backgrounds/bg-bamboo.jpg) — 竹林主题
  - [bg-dusk.jpg](file:///D:/QiyunGe/src/main/resources/images/backgrounds/bg-dusk.jpg) — 暮色主题
- [MainShell](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/shell/MainShell.java) 重构为 StackPane 分层结构：背景图层 → 覆盖层 → 主界面 → 浮层

#### 闲云馆（原百趣园）娱乐模块全面升级
- **模块更名**：百趣园 → 闲云馆
- **猜数字游戏（猜天机）**：支持简单/中等/困难三种难度，记录最佳成绩
- **翻牌记忆游戏（忆往昔）**：多难度等级，支持最佳分数与用时记录
- **游乐场首页**：卡片式游戏入口，展示游戏简介与最佳成绩
- **游戏记录系统**：统一存储各类游戏的得分、用时、难度等数据
- **三栏式布局**：顶部标题栏 + 左侧边导航 + 主内容区，界面结构更清晰

#### 诗意日历组件
- 新增 [PoeticCalendar](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/components/PoeticCalendar.java) 组件，集成到主界面顶栏
- 支持公历 / 农历一键切换显示
- 自动实时更新日期显示

#### 农历工具类
- 新增 [LunarCalendar](file:///D:/QiyunGe/src/main/java/com/qiyunge/infrastructure/util/LunarCalendar.java)
- 支持公历到农历的日期转换

#### 播放队列浮层
- 主界面右下角新增播放队列弹出面板
- 显示当前播放队列，支持点击切换、移除歌曲

#### 歌词浮层
- 主界面新增可拖动的歌词面板
- 支持歌词实时滚动高亮、进度条点击跳转
- 面板可自由拖动位置

### 🎨 界面优化

#### 主题系统重构
- 主题调整为三套：晨雾（morning）、竹林（bamboo）、暮色（dusk）
- [ThemeService](file:///D:/QiyunGe/src/main/java/com/qiyunge/app/ThemeService.java) 主题枚举更新
- [theme.css](file:///D:/QiyunGe/src/main/resources/styles/theme.css) 全面重构：
  - 细化主题变量（背景色、主色、强调色、文字色、边框色等）
  - 引入 `bg-image` 和 `bg-overlay` 变量
  - 三套主题各自独立的配色方案与背景图

#### 图片详情对话框重构
- [ImageDetailDialog](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/gallery/ImageDetailDialog.java) 全面升级
- 更合理的图片缩放与布局
- 优化大图查看体验

#### 样式系统整合
- [components.css](file:///D:/QiyunGe/src/main/resources/styles/components.css) 大幅扩展：新增背景层、娱乐模块、游戏卡片、侧边导航、诗意日历、播放队列、歌词面板等组件样式
- 移除独立的 [gallery.css](file:///D:/QiyunGe/src/main/resources/css/gallery.css)，样式统一整合至 components.css

#### 主界面结构优化
- [MainShell](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/shell/MainShell.java) 重构为分层 StackPane
- 顶栏新增诗意日历显示
- 页面标题根据导航自动更新

#### 音乐视图优化
- [MusicView](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/music/MusicView.java) 交互改进
- 播放控制与歌单管理体验提升

#### 仪表盘优化
- [DashboardView](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/dashboard/DashboardView.java) 数据卡片布局优化
- 新增游戏相关统计数据展示

### 🔧 底层改进

#### 音乐栏架构重构
- [MusicBar](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/components/MusicBar.java) 改为通过 [MusicViewModel](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/music/MusicViewModel.java) 代理属性绑定，不再直接依赖 MusicPlayerService

#### 人脸识别优化
- [FaceRecognitionService](file:///D:/QiyunGe/src/main/java/com/qiyunge/application/face/FaceRecognitionService.java) 改进
- 模型训练与识别流程优化

#### 音乐播放服务优化
- [MusicPlayerService](file:///D:/QiyunGe/src/main/java/com/qiyunge/application/service/MusicPlayerService.java) 播放逻辑改进
- 播放状态管理更稳定

#### 用户认证服务增强
- [AuthService](file:///D:/QiyunGe/src/main/java/com/qiyunge/application/auth/AuthService.java) 新增认证相关功能

#### 应用上下文优化
- [AppContext](file:///D:/QiyunGe/src/main/java/com/qiyunge/app/AppContext.java) 服务注册扩展
- 纳入娱乐服务、游戏记录仓储等新组件

#### 管理后台优化
- [AdminView](file:///D:/QiyunGe/src/main/java/com/qiyunge/ui/admin/AdminView.java) 界面改进

### 🗄️ 数据库迁移

| 版本 | 说明 |
|------|------|
| **V6** | 新增闲云馆模块表：`game_records`（游戏记录）、`daily_fortune`（每日运势）、`daily_quiz_records`（每日答题）、`pomodoro_sessions`（番茄钟）、`achievements`（成就） |
| **V7** | 修复 V3/V5 中时间格式不一致问题，统一使用 `datetime('now', 'localtime')` 本地时间格式 |
| **V8** | 移除 `registration_requests` 表的 `username` UNIQUE 约束，允许同一用户在申请被驳回/账号注销后重新提交注册申请 |

### 🐛 问题修复

- 修复部分数据库表时间格式混用 UTC 与本地时间的问题
- 修复注册请求表无法重复提交的限制
- 修复若干组件样式与布局问题

---

## [v1.0.0] - 2026-07-07

### 🎉 初始版本发布

栖云阁 QiyunGe 第一个正式版本。

#### 核心功能模块

- **望云台（仪表盘）**：用户数据统计总览，可视化数据卡片
- **听雨轩（音乐模块）**：本地/在线音乐播放、歌词显示、歌单管理、播放队列
- **拾光廊（图库模块）**：本地图库与在线图源（Pexels/Unsplash/Pixabay/Wikimedia）
- **百趣园（娱乐模块）**：休闲互动区域
- **吾庐（个人资料）**：修改信息、密码、注销账户
- **阁务司（后台管理）**：用户管理、注册审批、审计日志（仅管理员可见）
- **云枢（设置）**：四套主题切换、在线图源 API 配置、网易云音乐 API 配置
- **人脸识别登录**：基于 OpenCV LBPH 算法的人脸检测与识别

#### 技术栈

- Java 21 + JavaFX 21
- SQLite (WAL 模式) + Flyway 数据库迁移
- JavaCV/OpenCV 4.7 人脸识别
- JLayer MP3 音频播放
- Jackson JSON 处理
- BCrypt 密码加密
- Ikonli 图标库
