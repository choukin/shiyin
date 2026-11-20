package com.shiyin.player.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 设计稿基准与已知圆角（确定值，请勿修改）。
 *
 * 设计稿为 390×844 @1x 逻辑像素，Android 端按 dp 一一对应（1 逻辑像素 = 1dp）。
 * 真机截图比对时需乘以 density 换算为 px，见 docs/PIXEL_PERFECT.md。
 *
 * 设计稿未给出的间距 / 尺寸已按 Q1 决策填入 [ProvisionalDimens]，全部为暂定值；
 * 标注到位后只改该 object，屏幕层零改动。
 */
object ShiyinDimens {
    const val DESIGN_WIDTH_DP = 390
    const val DESIGN_HEIGHT_DP = 844

    val CardCorner = 16.dp
    val ButtonCorner = 14.dp
    val CapsuleCorner = 36.dp
    val CoverCorner = 24.dp
}

/**
 * PROVISIONAL（暂定标尺）：依 4dp 栅格与常见移动端尺寸估算，**不是设计标注**。
 * 所有屏幕层布局只能引用这里的常量，禁止在 Composable 中写尺寸魔数。
 * 替换流程：设计标注到位后逐项改值即可，无需改动任何屏幕文件。
 */
object ProvisionalDimens {
    val ScreenHorizontalPadding = 20.dp // PROVISIONAL
    val SectionVerticalSpacing = 24.dp // PROVISIONAL
    val ItemVerticalSpacing = 12.dp // PROVISIONAL
    val ContentVerticalPadding = 16.dp // PROVISIONAL
    val CardInnerPadding = 16.dp // PROVISIONAL

    val QuickToolCardHeight = 88.dp // PROVISIONAL
    val QuickToolCardWidth = 84.dp // PROVISIONAL：快捷工具横向滚动卡片宽度
    val TrackListItemHeight = 72.dp // PROVISIONAL
    val GroupItemHeight = 72.dp // PROVISIONAL：专辑 / 歌手分组卡片，与曲目行等高
    val SongResultItemHeight = 76.dp // PROVISIONAL：搜歌结果行（含试听/下载两个操作）
    val FavoriteIconSize = 32.dp // PROVISIONAL：收藏星标点击区域
    val CapsuleTabHeight = 36.dp // PROVISIONAL
    val MiniPlayerHeight = 64.dp // PROVISIONAL
    val WaveformHeight = 48.dp // PROVISIONAL
    val ButtonHeight = 48.dp // PROVISIONAL
    val ButtonHeightSmall = 40.dp // PROVISIONAL
    val TopBarHeight = 56.dp // PROVISIONAL

    val IconSizeSmall = 20.dp // PROVISIONAL
    val IconSizeMedium = 24.dp // PROVISIONAL
    val IconSizeLarge = 32.dp // PROVISIONAL
    val PlayControlIconSize = 40.dp // PROVISIONAL
    val PlayButtonSize = 64.dp // PROVISIONAL

    val CoverSizeLarge = 320.dp // PROVISIONAL：07 屏大封面
    val CoverSizeThumbnail = 56.dp // PROVISIONAL：列表缩略封面
    val CoverSizeMini = 44.dp // PROVISIONAL：迷你播放条封面

    val ElevationCard = 2.dp // PROVISIONAL
    val ElevationRaised = 4.dp // PROVISIONAL
    val ElevationFab = 8.dp // PROVISIONAL
    val DividerThickness = 1.dp // PROVISIONAL
    val WaveformBarWidth = 3.dp // PROVISIONAL

    /** PROVISIONAL：844 视为内容画布高度，不含系统状态栏。 */
    const val DesignHeightIncludesStatusBar = false

    /** PROVISIONAL：统一在 ShiyinScaffold 用 WindowInsets.safeDrawing + statusBars 处理。 */
    const val EnableEdgeToEdge = true
}
