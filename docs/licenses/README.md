# 字体许可（待补齐）

批次 B 阶段 1 会把以下字体文件离线打包进 `app/src/main/res/font/`：

| 字体 | 用途 | 许可 |
|---|---|---|
| Noto Sans SC | 正文 / 标题 | SIL Open Font License 1.1（Google Fonts / notofonts） |
| Geist Mono | 时长、进度、编号、波形刻度等数字 / 等宽文本 | SIL Open Font License 1.1（Vercel geist-font） |

**待办（实施时执行，此处不代填许可文本）**：

1. 从官方仓库下载字体文件与其 `LICENSE` / `OFL.txt` 原文，
   逐字放入本目录（建议 `OFL-NotoSansSC.txt`、`OFL-GeistMono.txt`），不要改写或概括。
2. 核对许可是否允许随 APK 分发、是否要求保留 NOTICE；OFL 通常要求随附许可文本。
3. Noto Sans SC 的 CJK 全字集体积较大（数 MB～十几 MB），
   只取 Regular / Medium / Bold 中的 2–3 个字重，或做子集化 / 使用 variable font，避免 APK 膨胀。
4. 字体文件名必须全小写 + 下划线（如 `noto_sans_sc_regular.ttf`），与 `Type.kt` 中 `Font(R.font.x)` 一致。
5. 保留 `FontFamily.Default` / `FontFamily.Monospace` 作为字体加载失败时的兜底链。

任何第三方图标 / 矢量素材若后续引入，同样在此登记来源与许可。

## 二期新增依赖登记（2026-11-19）

| 依赖 | 版本 | 用途 | 许可 |
|---|---|---|---|
| `androidx.media3:media3-transformer` | 1.11.1 | 05 音乐裁剪 / 06 视频提取音频 | Apache License 2.0（AndroidX / AOSP） |
| `androidx.media3:media3-muxer`（传递依赖） | 1.11.1 | Transformer 内部封装输出 | Apache License 2.0 |
| `com.squareup.okhttp3:okhttp` | 5.5.1 | 04 链接下载（直链 GET） | Apache License 2.0（Square / OkHttp） |
| `com.squareup.okhttp3:okhttp-android`（KMP variant） | 5.5.1 | Gradle 自动解析的 Android 实现 | Apache License 2.0 |
| `com.squareup.okio:okio` / `okio-jvm`（传递依赖） | 3.18.1 | OkHttp / DataStore 共用 I/O 层 | Apache License 2.0 |

说明：
- Media3 全部模块共用 `version.ref = "media3"`，避免 ABI 错配；okio 由 Gradle 统一对齐到 3.18.1，
  与 `datastore-core-okio` 无冲突（已用 `dependencies --configuration debugRuntimeClasspath` 核实）。
- 未引入 ffmpeg-kit（已退役）、未引入 Retrofit / WorkManager / Coil 网络层。
- Apache-2.0 要求随分发保留 NOTICE，发布前需核对各 AAR 内 `NOTICE` 文件是否需要随 APK 附随
  （当前为内部测试构建，未做发布打包）。
