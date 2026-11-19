# 构建与验收

> 最后更新：2026-11-19。本文记录**本机已实测通过**的真实构建链路；
> 批次 A 时期的「本机无 SDK / 不能编译」描述已作废。
> 版本事实源见 `gradle/libs.versions.toml`，依赖说明见 `docs/DEPENDENCIES.md`。

## 0. 本机环境（已具备，无需再补）

| 项 | 值 |
|---|---|
| 机器 | Apple M2 Mac，macOS（Hypervisor.Framework 可用） |
| JDK | 17 |
| Android SDK | `/Users/zhaoxin/Library/Android/sdk`（含 `platforms/android-37.0`、`android-36`、`build-tools/36.0.0`、`platform-tools/adb`、`emulator/`） |
| Gradle | Wrapper 9.6.0（仓库内 `gradlew` / `gradlew.bat` / `gradle-wrapper.jar` 齐全） |
| Android CLI | Google 新版 `android` v1.1.16919734，位于 `/Users/zhaoxin/.local/bin/android` |
| AVD | `medium_phone`（API 36 / arm64-v8a / google_apis_playstore） |

`ANDROID_HOME` / `ANDROID_SDK_ROOT` 未设置为环境变量也不影响：Gradle 通过
`local.properties` 或 SDK 自动发现定位，`android` CLI 自己识别 SDK 路径。

## 1. 常用命令（均在本机实测通过）

```bash
cd "/Users/zhaoxin/WorkBuddy/云门"

# 编译 + 单测（25 个纯 JVM 单测）
./gradlew :app:assembleDebug :app:testDebugUnitTest --no-daemon

# 产物
ls -lh app/build/outputs/apk/debug/app-debug.apk

# 查询实际解析的依赖树（核实版本/冲突）
./gradlew :app:dependencies --configuration debugRuntimeClasspath --no-daemon
```

AGP 9 内置 Kotlin：不要再应用 `org.jetbrains.kotlin.android` 插件，
`kotlinOptions { jvmTarget }` 也已移除，Java 17 兼容性由 `compileOptions` 对齐。

## 2. 模拟器与装包

```bash
ANDROID=/Users/zhaoxin/.local/bin/android
ADB=/Users/zhaoxin/Library/Android/sdk/platform-tools/adb
APK="/Users/zhaoxin/WorkBuddy/云门/app/build/outputs/apk/debug/app-debug.apk"

$ANDROID emulator list --long
$ANDROID emulator start medium_phone --cold      # 阻塞至 fully started
$ADB devices                                     # emulator-5554
$ADB -s emulator-5554 shell getprop sys.boot_completed   # 1
$ADB -s emulator-5554 install -r -g "$APK"
$ADB -s emulator-5554 shell am start -n com.shiyin.player/.MainActivity
$ADB -s emulator-5554 shell screencap -p /sdcard/s.png
$ADB -s emulator-5554 pull /sdcard/s.png ./preview/s.png
$ANDROID emulator stop medium_phone
```

**环境坑**：`android sdk install` / `emulator start` 这类写操作需要**前台 + 沙箱外授权**。
后台模式下升级不生效，会报 `.sdk/objects/*.part: Operation not permitted (file-delete)`
或 `hardware-qemu.ini` 写不进导致 QEMU 早退。前台授权后一次通过。

## 3. 04 链接下载的本地端到端测试

不依赖公网，用宿主机起静态服务，模拟器通过 `10.0.2.2` 访问：

```bash
mkdir -p /tmp/shiyin-dl && cd /tmp/shiyin-dl
python3 -m http.server 8765 --bind 0.0.0.0
```

在 04 屏输入 `http://10.0.2.2:8765/test.wav` 即可验证真实下载 → 进度 → 入库。
`android:usesCleartextTraffic="true"` 已为 http 直链放通明文；上线前可改为
`res/xml/network_security_config.xml` 域名白名单收紧。

校验入库结果：

```bash
$ADB -s emulator-5554 shell content query \
  --uri content://media/external/audio/media \
  --projection _id:_display_name:duration:mime_type \
  --where "is_music!=0"
```

曲库查询条件是 `IS_MUSIC != 0 AND DURATION > 0`，新文件需被系统媒体扫描器解析出时长后才会出现。

## 4. 权限与组件

7 个权限：`READ_MEDIA_AUDIO`、`READ_EXTERNAL_STORAGE(maxSdk 32)`、`FOREGROUND_SERVICE`、
`FOREGROUND_SERVICE_MEDIA_PLAYBACK`、`POST_NOTIFICATIONS`、`INTERNET`、`ACCESS_NETWORK_STATE`。
组件：`.ShiyinApp`、`MainActivity`、`PlaybackService : MediaSessionService`（`mediaPlayback` FGS）。
详见 `docs/DEPENDENCIES.md` §4 与 `app/src/main/AndroidManifest.xml`。

## 5. 已验收 / 待人工验收

**已实测**：`assembleDebug`、`testDebugUnitTest`（25/25）、OkHttp/Media3 依赖解析、
API 36 模拟器装包启动无崩溃、8 屏截图渲染、04 直链下载闭环。

**仍需人工交互验收**：真实音频播放与后台/锁屏通知、`MediaController` 重连、权限拒绝与「去设置」、
广告关闭持久化、迷你播放条联动、专辑封面 `content://` 解析、05/06 真实媒体文件的波形与导出、
大文件与弱网下的下载取消。
