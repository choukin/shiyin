package com.shiyin.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 字体分工：Noto Sans SC 用于正文/标题，Geist Mono 用于时长、进度、编号、波形刻度。
 *
 * 字体文件尚未提供（res/font/ 为空），因此 **不能** 写 Font(R.font.xxx)：
 * 那会因缺资源直接编译失败。当前用 FontFamily.Default / Monospace 兜底。
 *
 * 字号 / 行高 / 字距已按 Q1 决策填入 PROVISIONAL 暂定标尺
 * （Material 3 常见值），集中在本文件，标注到位后只改本文件。
 *
 * 字重按 unslop 收敛为 400 / 500 两级：层级由字号与颜色建立，不靠加粗。
 * Material 3 默认的 titleLarge=W700、titleMedium/labelLarge=W600 均已下调，
 * 避免「700–900 headings」与常规性 600 强调。
 *
 * 字体到位后的替换步骤：
 *  1. .ttf/.otf 放入 res/font/（文件名小写下划线）+ OFL 原文放入 docs/licenses/；
 *  2. 用 Font(R.font.xxx, weight = FontWeight.W400/W500) 构造下面两个 FontFamily；
 *  3. 按真实标注逐项替换下列 fontSize / lineHeight / letterSpacing。
 *  字体度量不一致时，文字块的顶边与基线必然错位，无法靠调 padding 收敛。
 */
val ShiyinSans: FontFamily = FontFamily.Default
val ShiyinMono: FontFamily = FontFamily.Monospace

/** PROVISIONAL：等宽文本样式，供时长、进度、屏编号、08 屏参数表使用。 */
object ShiyinMonoText {
    val TimeCode = TextStyle( // PROVISIONAL
        fontFamily = ShiyinMono,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = 0.5.sp
    )
    val Parameter = TextStyle( // PROVISIONAL
        fontFamily = ShiyinMono,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.W400,
        letterSpacing = 0.5.sp
    )
}

private val Base = Typography()

/** PROVISIONAL：所有数值为暂定标尺，非设计标注。字重统一 400 / 500。 */
val ShiyinTypography: Typography = Typography(
    displayLarge = Base.displayLarge.copy(fontFamily = ShiyinSans, fontSize = 57.sp, lineHeight = 64.sp, fontWeight = FontWeight.W400, letterSpacing = (-0.25).sp), // PROVISIONAL
    displayMedium = Base.displayMedium.copy(fontFamily = ShiyinSans, fontSize = 45.sp, lineHeight = 52.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), // PROVISIONAL
    displaySmall = Base.displaySmall.copy(fontFamily = ShiyinSans, fontSize = 36.sp, lineHeight = 44.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), // PROVISIONAL
    headlineLarge = Base.headlineLarge.copy(fontFamily = ShiyinSans, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), // PROVISIONAL
    headlineMedium = Base.headlineMedium.copy(fontFamily = ShiyinSans, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), // PROVISIONAL
    headlineSmall = Base.headlineSmall.copy(fontFamily = ShiyinSans, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.W400, letterSpacing = 0.sp), // PROVISIONAL
    titleLarge = Base.titleLarge.copy(fontFamily = ShiyinSans, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.W500, letterSpacing = 0.sp), // PROVISIONAL：unslop 由 W700 下调
    titleMedium = Base.titleMedium.copy(fontFamily = ShiyinSans, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W500, letterSpacing = 0.1.sp), // PROVISIONAL：unslop 由 W600 下调
    titleSmall = Base.titleSmall.copy(fontFamily = ShiyinSans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W500, letterSpacing = 0.1.sp), // PROVISIONAL
    bodyLarge = Base.bodyLarge.copy(fontFamily = ShiyinSans, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.W400, letterSpacing = 0.5.sp), // PROVISIONAL
    bodyMedium = Base.bodyMedium.copy(fontFamily = ShiyinSans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W400, letterSpacing = 0.25.sp), // PROVISIONAL
    bodySmall = Base.bodySmall.copy(fontFamily = ShiyinSans, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W400, letterSpacing = 0.4.sp), // PROVISIONAL
    labelLarge = Base.labelLarge.copy(fontFamily = ShiyinSans, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.W500, letterSpacing = 0.1.sp), // PROVISIONAL：unslop 由 W600 下调
    // 数字 / 等宽文本：时长 03:24、进度、屏编号、波形刻度
    labelMedium = Base.labelMedium.copy(fontFamily = ShiyinMono, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.W500, letterSpacing = 0.5.sp), // PROVISIONAL
    labelSmall = Base.labelSmall.copy(fontFamily = ShiyinMono, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.W500, letterSpacing = 0.5.sp) // PROVISIONAL
)
