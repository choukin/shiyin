# 设计说明（对应设计稿第 08 屏）

> 本文件是 Ardot 设计稿《拾音 · 单机音乐播放器 App UI》（fileId `746937122612662`）
> 第 08 屏「设计说明」的文字化归档。
> 设计稿本体需登录访问，本地只有文字规格；**App 内 08 屏为只读静态页，标注「设计说明，非产品功能」**。
> 视觉还原按文字规格执行，**非像素级比对**。

## 1. 画布与排布

- 8 屏手机端 UI，单屏 **390 × 844**
- 设计稿排布：4 列 × 2 行 + 标题栏 + 屏标签
- 用户附加场景标签曾为「网站设计」，但需求是手机 App，按移动端执行

## 2. 视觉规范（唯一 token 落点：`ui/theme/`）

| 用途 | 值 |
|---|---|
| 背景 | `#F5F6F3` |
| 卡片 / 表面 | `#FFFFFF` |
| 主文字 | `#191C1A` |
| 强调色（墨绿） | `#1E6A54` |
| 浅强调 | `#E7F0EB` |

| 元素 | 圆角 |
|---|---|
| 卡片 | 16 |
| 按钮 | 14 |
| 胶囊导航 | 36 |
| 封面 | 24 |

字体：**Noto Sans SC**（正文/标题）+ **Geist Mono**（时长、进度、编号、波形刻度等数字/等宽文本）。
Android 端离线打包 `.ttf/.otf` 到 `res/font`，注入 `FontFamily` → `Typography` → `MaterialTheme`；
不采用 Google Fonts 可下载字体（依赖 GMS + 联网，违背离线字体基线）。

所有色值、圆角、间距集中在 `Color.kt` / `Shape.kt` / `Dimens.kt` / `Type.kt`，
**屏幕层禁止硬编码色值与圆角**。

## 3. 8 屏清单

| 编号 | 名称 | 实现程度（一期 / 二期） |
|---|---|---|
| 01 | 开屏 | 完整：纯品牌 1.2s |
| 02 | 曲库首页 | 完整：四 Tab（全部/收藏/专辑/歌手）真实视图、分组二级详情页、快捷工具、最近添加、迷你播放条 |
| 03 | 扫描本机 | 完整：权限请求、扫描进度/结果、回到曲库 |
| 04 | 下载 | 完整：搜歌（第三方公开接口搜索 → 列表 → 独立 ExoPlayer 试听 → 下载入库）、分页加载、直链下载、SAF 本地导入三入口 |
| 05 | 音乐裁剪 | 二期完整：真实 PCM 波形 + 可拖拽选区 + Transformer 裁剪导出 |
| 06 | 视频提取音频 | 二期完整：整段 / 所选片段可点击 + `setRemoveVideo(true)` 提取导出 |
| 07 | 播放器 | 完整：渐变封面、Mono 时长、进度、播放控制、后台播放 |
| 08 | 设计说明 | 完整：App 内只读静态页 + 本文档 |

设计稿中所有封面 / 波形 / 视频预览均为**矢量 SVG + 渐变**构建（设计稿内元素，本地无 SVG 文件）。
Android 端落地方式：Compose `Canvas` + `Brush.linearGradient/radialGradient` 自绘 + 少量 vector drawable，
不引入 SVG 渲染库与第三方波形库。

## 5. 待补充状态（4 个，二期已全部补齐）

| 状态 | 处理 |
|---|---|
| 空曲库引导 | 02 屏真实实现：引导卡 +「扫描本机」按钮 → 03 屏 |
| 无存储权限 | 02/03 屏真实实现：权限说明 +「授予权限」+「去设置」；永久拒绝不重复弹窗 |
| 链接解析失败 | 04 直链真实实现：无效地址 / 无网络 / HTTP 错误 / 非媒体内容 / 空文件 / 超 300MB / 写库失败，逐类给出提示并附技术 detail |
| 搜歌状态 | 04 搜歌真实实现：未搜索引导、搜索中、空结果、加载更多、搜索/试听失败，均给出对应状态 |
| 导出成功提示 | 05/06 二期真实实现：`ExportState.Success` 显示实际导出文件名，文件写入音乐库后可被曲库扫到 |

## 6. 产品风险提示

- **04 下载（2026-11-20 扩展）**：主入口「搜歌」调用 myfreemp3 公开接口
  `POST https://www.myfreemp3.com.cn/`（表单 `input/filter=name/page/type=netease`）取列表，
  用独立 ExoPlayer 试听，下载沿用 `LinkDownloadRepository` 入库。次要入口「直链」仍只对用户
  显式粘贴的 http(s) 媒体直链发 GET，响应必须是音频/视频类型，HTML 错误页一律拒绝入库。
  为此新增 `INTERNET` / `ACCESS_NETWORK_STATE` 两个权限与 OkHttp 依赖，其余页面仍为纯本地实现。
  **搜索结果来自第三方站点，来源合法性由使用者自行负责；上线前需单独做内容合规与隐私政策评审**，
  并确认该接口的服务条款允许客户端调用。
- **接口实测约束（2026-11-20）**：返回 `{code, error, data:{list, more, total}}`，每页 10 条；
  空关键词 / 无匹配会返回整页 HTML 而非 JSON，解析前必须判首字符。结果里的播放地址是
  `http://music.163.com/song/media/outer/url?id=xxx.mp3`，直连会 302 到 126.net CDN 后拿 403；
  **必须把协议规范化为 https**，才返回 200 audio/mpeg 且支持 Range。`type=kuake` 空列表、
  `type=ws` 返回 code 403，故 UI 只暴露 netease。
- **明文流量**：Android 9+ 默认禁用明文 HTTP，04 为支持直链与 CDN 的 http 重定向设置了
  `android:usesCleartextTraffic="true"`（https 外链 302 后落到 `http://m801.music.126.net`）。
  若上线要收紧，改用 `res/xml/network_security_config.xml` 域名白名单即可，无需改代码。
- **转码前台服务**：本期导出与下载在页面生命周期内的协程中完成，未引入
  `FOREGROUND_SERVICE_MEDIA_PROCESSING`；P2 若接到后台转码，需处理 6h/天配额与主动 stop。
- **平台差异**：iOS 沙盒下「扫描本机文件」实际走文件 App / 文档选择器；
  Android 需 `READ_MEDIA_AUDIO`（API 33+）或 `READ_EXTERNAL_STORAGE`（≤32）。
- **专辑封面可用性**：MediaStore `albumart` URI 在部分 OEM / 无内嵌封面的文件上返回空，
  需统一兜底渐变封面。
