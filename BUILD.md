# 构建与验收（批次 A）

本机（Apple M2 Mac）只有 JDK 17 与 `adb`，**没有 Android SDK / Gradle / Kotlin 编译器**，
也没有 `ANDROID_HOME` / `JAVA_HOME`。因此本仓库只交付源码，编译与运行必须在 Android Studio 完成。

---

## 0. 前置：补齐 Gradle Wrapper 三件套（需你操作一次）

按已确认方案，用 Android Studio 新建一个 **Empty Compose Activity** 官方模板工程，
把模板生成的以下三个文件复制到本仓库同路径：

```
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
```

理由：`gradle-wrapper.jar` 是二进制文件，本机无 `gradle` 命令无法生成；
官方模板同时提供一套经过实测的 AGP / Gradle / Kotlin 组合，可把版本错配风险降到最低。

**合并原则**：模板只负责 wrapper 三件套；其余构建脚本、版本目录、Manifest、
源码以本仓库已有文件为准。若模板自带 `gradle/libs.versions.toml` 且版本与
本仓库不同，**以模板默认值为基座**，把本仓库 `[libraries]` 的依赖条目合并进去，
不要强行把 AGP/Kotlin/BOM 升到本仓库标注的版本——先让 Sync 通过，再逐步升级。

---

## 1. 安装 SDK（Android Studio → SDK Manager）

| 需要 | 版本 |
|---|---|
| SDK Platform | **Android 17 (API 37)** —— `compileSdk = 37` 需要 |
| SDK Build Tools | **36.0.0** |
| SDK Platform-Tools | 最新（含 adb） |
| JDK | 17（Android Studio 自带 JBR 17 即可） |

设置环境变量（或由 Android Studio 自动接管）：

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

---

## 2. 打开与 Sync

1. Android Studio → **Open** → 选择本目录 `/Users/zhaoxin/WorkBuddy/云门/`。
2. 等待 **Gradle Sync** 完成（首次会下载 Gradle 9.6.0 发行包与各依赖）。
3. Sync 通过后，先 `Run` 一次：应看到一个显示「拾音」的空白页。
   这说明 Kotlin / Compose / 资源合并 / Manifest 全链路通畅。
4. 把 Sync 结果告诉我，我再开始批次 B（设计系统 → 数据层 → 播放内核 → 8 屏 UI）。

---

## 3. Troubleshooting

| 现象 | 处理 |
|---|---|
| `SDK location not found` | 配 `ANDROID_HOME`，或在根目录建 `local.properties` 写 `sdk.dir=...`（已被 `.gitignore` 忽略） |
| `Failed to find Platform SDK android-37` | 装 Android 17 (API 37) 平台；若暂不想装，把 `compileSdk` 临时改为已装最高版本，并同步把 Compose BOM 降到与之兼容的版本 |
| `kotlinOptions` 已移除 / 报未知 DSL | AGP 9 + Kotlin 2.4 可能改用 `kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }`，直接采用 Android Studio 模板生成的写法 |
| Gradle 9.6.0 下载慢或版本不兼容 | 以模板自带的 `gradle-wrapper.properties` 为准，按其 AGP 兼容表选 Gradle 版本 |
| `Could not resolve androidx.lifecycle:*:2.12.0` | 2.12.0 目前只有 alpha；本仓库已锁定 **2.11.0**（最新稳定）。若被误升级请回退 |
| Coil import 报 `io.coil-kt` 找不到 | Coil 3 的 group 是 `io.coil-kt.coil3`，包名也变了；确认用的是 `coil-compose` 3.6.3 |
| `MediaSessionService` 相关类找不到 | 确认 `media3-session` / `media3-exoplayer` / `media3-common` 三者版本同为 1.11.1 |

---

## 4. Sync 通过后的验收清单（由你确认）

- [ ] Gradle Sync 成功，无未解析依赖
- [ ] `Build → Make Project` 通过
- [ ] 安装到设备/模拟器能启动，显示「拾音」空白页
- [ ] `AndroidManifest.xml` 中 5 个权限 + `PlaybackService` 声明被正确合并（可用 Merged Manifest 视图查看）
- [ ] 确认本机 API 级别，便于批次 B 走查 `READ_MEDIA_AUDIO`(≥33) / `READ_EXTERNAL_STORAGE`(≤32) 两条权限路径

---

## 5. 本机不能做的验证

Gradle Sync、Kotlin/Compose 编译、aapt2 资源合并、Lint/R8、真机权限流转、
后台播放与锁屏/通知/蓝牙控制、MediaStore 实际扫描结果、字体渲染、8 屏视觉走查、
单测执行 —— 全部需要在 Android Studio / 真机上完成。
本机只做静态自检（TOML 引用完整性、XML well-formed、依赖坐标与目录树核对）。
