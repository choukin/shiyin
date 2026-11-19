# 拾音 · 单机音乐播放器 — 依赖清单

> 核实日期：2026-11-13。版本来源：Google Maven / Maven Central 的 `maven-metadata.xml`、`kotlinlang.org`。
> 本机无 Android SDK / Gradle，**所有版本组合未实测**，以 Android Studio 侧 Gradle Sync 通过为最终判据。
> 唯一版本事实源：`gradle/libs.versions.toml`。

## 1. 版本基线

| 项 | 版本 | 核实结论 |
|---|---|---|
| Kotlin | **2.4.21** | kotlinlang.org 官网 Latest（2026-10-08） |
| Compose BOM | **2026.09.00** | Google Maven `<latest>/<release>` 一致 |
| Media3 | **1.11.1** | Google Maven 最新稳定；1.12.0-beta01 为预发布，不采用 |
| activity-compose | **1.13.0** | Google Maven 稳定版；1.14.0-alpha03 为预发布 |
| AGP | **9.5.1** | Google Maven：9.5.0 之后的稳定补丁；9.6.0-alpha03 为预发布 |
| Gradle | 9.6.0 | AGP 9.5 系列配套，**需 Sync 复核** |
| lifecycle | **2.11.0** | ⚠️ Google Maven `<release>` 是 `2.12.0-alpha05`（预发布）；最新无后缀稳定版是 2.11.0，规划稿曾误写 2.12.0 |
| navigation-compose | 2.10.2 | `<latest>/<release>` 一致 |
| core-ktx | 1.19.1 | `<latest>/<release>` 一致 |
| datastore-preferences | **1.3.0** | `<latest>/<release>` 一致（规划稿曾写 1.2.1） |
| Coil 3 coil-compose | 3.6.3 | Maven Central `<latest>/<release>` 一致 |
| kotlinx-coroutines | 1.11.0 | Maven Central `<latest>/<release>` 一致 |
| JUnit | 4.13.2 | Maven Central `<latest>/<release>` 一致 |
| material3 | **1.4.0** | 最新稳定；但由 BOM 管理，`[libraries]` 行**不写版本** |
| JDK / Java | 17 / jvmTarget 17 | 本机 17.0.19 满足 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 | Compose 1.12 要求 compileSdk 37 + AGP 9；targetSdk 36 满足 Play ≥36 要求 |

**Google Maven 的坑**：`<latest>/<release>` 字段会把 alpha/beta 当作 release（AGP、lifecycle、material3 均如此）。选版本必须看 `<versions>` 列表里**无后缀**的最大值，不能只读 `<release>`。

## 2. 一期必需依赖

| 类别 | 坐标 | 用途 |
|---|---|---|
| 插件 | `com.android.application` / `org.jetbrains.kotlin.android` / `org.jetbrains.kotlin.plugin.compose` | 构建；Compose Compiler 版本**复用 Kotlin 的 `version.ref`**，不单独锁 |
| BOM | `androidx.compose:compose-bom` (`platform`) | 统一管理所有 `androidx.compose.*` |
| UI | `androidx.compose.ui:ui` / `ui-graphics` / `ui-tooling-preview` / `ui-tooling`(debug) / `androidx.compose.foundation:foundation` / `androidx.compose.material3:material3` | 8 屏基础、Canvas + `Brush` 渐变自绘、`@Preview`、Material 3 组件 |
| 宿主 / 导航 / 状态 | `androidx.activity:activity-compose` / `androidx.navigation:navigation-compose` / `androidx.lifecycle:lifecycle-viewmodel-compose` / `lifecycle-runtime-compose` / `androidx.core:core-ktx` | `setContent`、8 条路由、ViewModel、`collectAsStateWithLifecycle`、`ContextCompat` 权限判断 |
| 播放 | `androidx.media3:media3-exoplayer` / `media3-session` / `media3-common` / `media3-datasource` / `media3-extractor` | ExoPlayer 解码；`MediaSessionService` 后台播放 + 锁屏/通知/蓝牙控制；`ContentDataSource` 读 MediaStore `content://` URI |
| 封面 | `io.coil-kt.coil3:coil-compose` | `AsyncImage` 加载 MediaStore 专辑封面。**Coil 3 已换 group**（2.x 是 `io.coil-kt`） |
| 持久化 | `androidx.datastore:datastore-preferences` | 收藏、上次播放、排序偏好、广告条「已关闭」、占位页「已了解」 |
| 异步 | `org.jetbrains.kotlinx:kotlinx-coroutines-core` / `-android` | MediaStore 查询切 `Dispatchers.IO`；Flow / StateFlow |
| 测试 | `junit:junit` | 纯逻辑单测（时长格式化、Track 映射、UiState 状态机） |

### 关键取舍

| 决策点 | 结论 | 理由 |
|---|---|---|
| 曲库数据 | 纯 `ContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)`，**不落库、不引 Room** | 系统媒体库是权威源，用户在别处增删文件会自动反映；「最近添加」用 `DATE_ADDED DESC` |
| DI | 手动 `AppContainer`，**不引 Hilt/Koin/KSP** | `MediaSessionService` 由系统实例化无法构造注入；单模块小项目零注解处理器＝零构建失败面 |
| 权限请求 | `activity-compose` 自带 `rememberLauncherForActivityResult` + `ActivityResultContracts`，**不引 Accompanist** | Accompanist 大部分模块已废弃迁移到官方 Compose；权限逻辑本就应在 Screen 层 |
| 播放 UI | **不引 `media3-ui` / `media3-ui-compose`** | 07 屏按设计稿用 Compose 自绘，View 体系 `PlayerView` 反而是负担 |
| 封面网络层 | **不引 `coil-network-okhttp` / `coil-svg`** | Coil 3 已把网络层拆为独立 artifact；一期只有 `content://` 本地封面，无网络图片、无 SVG 文件 |
| 字体 | `.ttf/.otf` **离线打包进 `res/font`**，不引 `ui-text-google-fonts` | 可下载字体依赖 GMS + 联网，违背单机离线定位 |
| 矢量素材 | Compose `Canvas` + `Brush` 自绘 + 少量 vector drawable，**不引 SVG 渲染库/波形库** | 设计稿的 SVG 是设计元素，Android 不吃 SVG；渐变用 `Brush` 更贴近且零依赖 |
| 广告 | **零 SDK**；曲库页为本地可关闭占位条 | 按已确认决策，一期不接真实广告、不请求网络 |

## 3. 明确不引入 / 二期预留

**一期零引入**：`media3-ui`、`media3-ui-compose`、各流媒体/软解码扩展、`material-icons-extended`（少量图标自绘）、`ui-text-google-fonts`、`coil-network-okhttp`、`coil-svg`、Room、Hilt、Koin、KSP、Accompanist、`material3-adaptive`、Robolectric、Espresso、任何广告 SDK。
**一期 Manifest 不声明 `INTERNET`**（纯单机，最小化权限面）。

**二期才引入**：

| 功能 | 依赖 / API | 说明 |
|---|---|---|
| 音乐裁剪（05 屏） | `androidx.media3:media3-transformer`（+ `media3-muxer` / `media3-inspector`） | `MediaItem.ClippingConfiguration(startPositionMs, endPositionMs)` |
| 视频提取音频（06 屏） | 同上 | `EditedMediaItem.Builder(...).setRemoveVideo(true)`，配合 clipping 取片段 |
| 转码前台服务 | `FOREGROUND_SERVICE_MEDIA_PROCESSING` | `mediaProcessing` 类型，每天累计 6h 上限，完成需主动 `stopForeground()+stopSelf()` |
| 链接下载（04 屏） | OkHttp（+ 可选 WorkManager / Retrofit）、`INTERNET`、`ACCESS_NETWORK_STATE` | **启动前必须先解决版权合规**，否则改为「导入本地文件」或砍掉 |
| 导出/分享 | `FileProvider` + SAF `ACTION_CREATE_DOCUMENT` | 与「导出成功提示」态配套 |
| Compose 组件测试 | `ui-test-junit4` / `ui-test-manifest` | 需 instrumented 环境 |

**禁用**：
- `com.arthenica:ffmpeg-kit-*` —— 已正式退役，Maven Central 二进制按计划下架（<6.0 于 2025-02-01、6.0 于 2025-04-01）；续作 FFmpegKitNext 为 source-only，需自行编译 NDK。**新项目不得作为依赖**，裁剪/提取统一走 Media3 Transformer。
- `MANAGE_EXTERNAL_STORAGE` —— Play 政策严格限制，MediaStore URI 已足够。

## 4. 权限与 Manifest 组件

| 声明 | API | 性质 | 说明 |
|---|---|---|---|
| `READ_MEDIA_AUDIO` | ≥33 | 运行时 | 只读音频；不需要 IMAGES / VIDEO |
| `READ_EXTERNAL_STORAGE` + `maxSdkVersion="32"` | ≤32 | 运行时 | 与上者二选一生效；`RequestMultiplePermissions` 可同时传 |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | ≥34 成对强制 | 安装时 | 对应 `foregroundServiceType="mediaPlayback"` |
| `POST_NOTIFICATIONS` | ≥33 | 运行时 | **媒体会话通知对此权限豁免**，拒绝不阻断播放；首次点播放时非阻塞请求 |
| `WAKE_LOCK` | — | — | 未声明，Media3/ExoPlayer 内部已处理播放期持锁 |

组件：`.ShiyinApp`、`MainActivity`（`exported=true`、`singleTop`、MAIN/LAUNCHER）、`PlaybackService : MediaSessionService`（`exported=true` 与官方模板一致，便于系统/外部控制器绑定；`foregroundServiceType="mediaPlayback"`；MediaSessionService action filter）。
**不声明**：`BOOT_COMPLETED`（API 35+ 禁止由此启动 mediaPlayback FGS）、`MediaLibraryService`（一期无车机浏览树）。

## 5. 风险

| 风险 | 缓解 |
|---|---|
| 版本组合未实测（本机无 SDK） | 批次 A 先只交骨架，以用户侧 Sync 通过为准；以 AS 官方模板生成的骨架为基座，不强行覆盖模板默认版本 |
| `compileSdk 37` 需 Android 17 平台 | BUILD.md 写明 SDK Manager 安装 API 37 + Build Tools 36.0.0；若暂未安装，先与模板默认值对齐再升级 |
| 链接下载版权 | 一期只占位、不写下载代码、不声明 `INTERNET`；二期启动前确认来源合法 |
| 字体体积/授权 | Noto Sans SC 全字集数 MB～十几 MB，只取 2–3 字重或子集化；OFL LICENSE 放 `docs/licenses/` |
| MediaStore 封面机型差异 | `AlbumArtResolver` 统一兜底渐变封面；Coil 配 placeholder/error；不阻塞列表 |
| 二期 KSP 版本耦合 | 一期手动 DI + DataStore 完全绕开；二期上 Hilt/Room 时单独做版本矩阵验收 |
