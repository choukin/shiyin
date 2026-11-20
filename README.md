# 拾音 · Shiyin

[![CI](https://github.com/choukin/shiyin/actions/workflows/ci.yml/badge.svg)](https://github.com/choukin/shiyin/actions/workflows/ci.yml)
[![Release](https://github.com/choukin/shiyin/actions/workflows/release.yml/badge.svg)](https://github.com/choukin/shiyin/actions/workflows/release.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

一款 **纯本机、零广告、无账号体系** 的 Android 音乐播放器。曲库直接读 MediaStore，不落数据库、不需要登录、不上传任何音频；所有编辑信息只存在应用私有目录里。UI 用 Jetpack Compose + Material 3 手写，图标为 Canvas 自绘，无 SVG / 图标库 / 动画库。

## 功能

| 模块 | 已实现 |
|---|---|
| 曲库首页 | 全部 / 收藏 / 专辑 / 歌手四个真实视图（中文拼音分组、二级详情页）、最近添加、空状态、迷你播放条 |
| 曲目操作 | 左滑露出「编辑 / 删除」两个按钮（滑动本身不再直接删）；长按菜单同入口，另含「永久删除手机文件」 |
| 曲目编辑 | 改名称、简介、海报（系统照片选择器）。存 DataStore + `filesDir/covers/`，**不改写手机里的音频文件、不申请写权限**；名称留空恢复原名，海报可恢复默认 |
| 删除范围 | 「仅从列表移除」＝应用内隐藏、文件保留、可一键恢复；「删除手机文件」＝走 MediaStore 系统确认框（API 30+ 回收站，低版本直接删） |
| 播放 | 唱片旋转封面、进度与控制、`MediaSessionService` 后台播放 + 锁屏 / 通知 / 蓝牙控制 |
| 歌单 | 创建、详情、加歌、移除、整单播放，DataStore 持久化（按 MediaStore `_ID` 引用） |
| 音频编辑 | 首页统一入口，内含「音频裁剪 / 提取音频」两个 Tab，共用一个 `EditorViewModel`，切换 Tab 自动重置默认模式与参数 |
| 音乐裁剪（Tab） | 真实 PCM 波形、可拖拽选区、裁剪试听、源文件大小、自定义导出文件名、M4A / WAV / MP3 三种格式 |
| 视频提取音频（Tab） | 视频预览、整段 / 所选片段、`setRemoveVideo(true)` 导出，同样支持试听与三种格式 |
| 下载 | 「搜歌」：第三方公开接口搜索 → 列表 → 独立 ExoPlayer 试听 → 分页 → 下载入库；「直链」：粘贴音 / 视频 URL 的 GET 下载（进度、取消、类型与大小校验）；SAF 本地导入 |
| 其他 | 扫描本机音频（权限引导 / 扫描结果）、App 内设计说明页、开屏 |

## 技术栈

| 类别 | 选型 |
|---|---|
| 语言 / UI | Kotlin 2.4.21、Jetpack Compose（BOM 2026.09.00）、Material 3，单 Activity + 静态路由 |
| 播放 / 媒体 | Media3 1.11.1（ExoPlayer / Session / Transformer），MediaCodec 解码 PCM，LAME（`recorder-mp3` 1.0.4）编码 MP3 |
| 网络 | OkHttp 5.5.1 —— 只用于 04 下载页；结果地址统一 `https` 规范化并允许跨协议重定向 |
| 持久化 | DataStore Preferences（收藏 / 歌单 / 曲目覆盖信息 / 隐藏曲目），不引入 Room |
| 封面 | Coil 3 `coil-compose`，只加载 `content://` 本地 URI / 应用私有文件，无网络图片层 |
| 构建 | Gradle 9.6.0、AGP 9.5.1、JDK 17；`compileSdk 37 / targetSdk 36 / minSdk 26` |
| 测试 | JUnit 4 纯 JVM 单测（57 例，覆盖编码 / 分组 / 链接规范化 / 状态派生等） |

AGP 9 已内置 Kotlin，**不要再应用 `org.jetbrains.kotlin.android` 插件**；依赖版本的唯一事实源是 `gradle/libs.versions.toml`。

## 快速开始

需要 JDK 17 与 Android SDK（Gradle 会在首次构建时按需补齐 `compileSdk 37` 对应的 platform 与 build-tools），仓库自带 Gradle Wrapper，无需全局 Gradle。

```bash
git clone git@github.com:choukin/shiyin.git
cd shiyin

# 单测 + debug 包
./gradlew :app:testDebugUnitTest :app:assembleDebug

# 产物
ls -lh app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties`（本机 SDK 路径）已被忽略，CI 用 `ANDROID_HOME`，无需提交。debug 包已显式开启 **v1 + v2 + v3 混合签名**，规避部分国产 ROM 安装器解析「纯 v2 单签名」包时报「解析软件包时出现问题 (33)」的问题。

更完整的本机构建 / 模拟器 / 权限说明见 [BUILD.md](BUILD.md)。

## 发版

打 `v*` tag 即触发 [Release 工作流](.github/workflows/release.yml)：单测 → 构建 release APK + AAB → 校验签名方案 → 生成 SHA256 → 创建 GitHub Release。

```bash
git tag -a v0.2.0 -m "release 0.2.0"
git push origin v0.2.0
```

- `versionName` 从 tag 去掉 `v` 得到；`versionCode` 取 `git rev-list --count HEAD`，单调递增，无需手改。
- tag 含 `-`（如 `v0.2.0-rc1`）会标记为预发布。
- 也可在 Actions 页面手动运行：默认只出构建产物，勾上 `create_release` 才创建 Release。
- `main` 与 PR 另有一个轻量 [CI](.github/workflows/ci.yml)：单测 + debug 包 artifact，不产出 Release。

**正式签名需要你在仓库里配置 4 个 Secrets**，未配置时工作流会自动降级为一次性 CI 密钥（包能装，但会在 Release 正文顶部警告「不可覆盖安装 / 不可上架」）。完整步骤、密钥生成命令与排错见 [docs/RELEASE.md](docs/RELEASE.md)。

| GitHub Secret | 含义 |
|---|---|
| `SIGNING_KEYSTORE_BASE64` | release keystore 文件的 Base64（单行） |
| `SIGNING_STORE_PASSWORD` | keystore 口令 |
| `SIGNING_KEY_ALIAS` | key alias |
| `SIGNING_KEY_PASSWORD` | key 口令 |

密钥不入库：`.gitignore` 已排除 `*.jks` / `*.keystore` / `keystore.properties` / `signing/`，构建脚本只在四项属性齐全时才创建 release signingConfig。

## 已知限制与合规提示

- **MP3 导出仅 arm64-v8a**：LAME 依赖只提供 arm64 的 `libLame.so`，x86 模拟器上该格式自动禁用，M4A / WAV 正常。
- **04 搜歌接入的是第三方站点 myfreemp3 的公开接口**，非官方 API，存在服务条款、可用性与版权合规风险；正式分发前建议替换为自有或已授权的音源。直链下载只请求用户显式粘贴的地址。
- `usesCleartextTraffic="true"` 是为 http 直链与本机 `10.0.2.2` 测试放通；上线前建议改为 `network_security_config.xml` 域名白名单。
- 名称 / 简介 / 海报覆盖按 MediaStore `_ID` 关联，音频文件被移动或重新扫描换 ID 后可能失配；应用内隐藏集合同理。
- 无广告 SDK、无账号与云同步、无后台数据上传；`INTERNET` 权限仅服务 04 下载页。
- 最低支持 Android 8.0（API 26）。

## 文档索引

| 文档 | 内容 |
|---|---|
| [BUILD.md](BUILD.md) | 本机环境、常用命令、模拟器装包、直链本地端到端测试 |
| [docs/RELEASE.md](docs/RELEASE.md) | 发版工作流、签名 Secrets、版本号规则、故障排查 |
| [docs/DEPENDENCIES.md](docs/DEPENDENCIES.md) | 依赖版本基线与取舍、权限清单、风险 |
| [docs/DESIGN_NOTES.md](docs/DESIGN_NOTES.md) | 视觉规范、8 屏清单、交互状态说明 |
| [docs/PIXEL_PERFECT.md](docs/PIXEL_PERFECT.md) | 设计稿还原的验收流程与差距清单 |
| [docs/licenses/](docs/licenses/) | 第三方依赖许可证归档 |

## 目录结构

```
app/src/main/java/com/shiyin/player/
├── data/            MediaStore 数据源、Repository、偏好、编解码、媒体编辑与删除
├── di/              AppContainer 手工依赖装配（无 Hilt / Koin）
├── playback/        PlaybackService（MediaSessionService）
└── ui/              screens / components / navigation / theme
.github/workflows/   CI 与 Release 自动化
docs/                设计、依赖、发版文档
gradle/              版本目录 libs.versions.toml 与 Wrapper
```

## License

[MIT](LICENSE) © 2026 choukin
