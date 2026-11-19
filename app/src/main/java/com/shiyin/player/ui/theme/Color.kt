package com.shiyin.player.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 设计稿已给出的 5 个确定色值（非暂定值，请勿修改）。
 *
 * 屏幕层禁止硬编码色值：所有语义色、渐变色、状态色一律从本文件或
 * [ProvisionalColors] 取用，方便设计标注到位后单点替换。
 */
val ShiyinBackground = Color(0xFFF5F6F3)
val ShiyinSurface = Color(0xFFFFFFFF)
val ShiyinOnSurface = Color(0xFF191C1A)
val ShiyinPrimary = Color(0xFF1E6A54)
val ShiyinPrimaryContainer = Color(0xFFE7F0EB)

/**
 * PROVISIONAL（暂定标尺）：设计稿文字规格未给出渐变起止色、角度、阴影、
 * 分隔线、次级文字与状态色。以下数值依 5 个确定色值推导 + 常见 Material 3
 * 语义色估算，**不是设计标注**。
 *
 * 替换流程：设计标注到位后只改本 object 的常量，屏幕层与 Theme.kt 零改动。
 */
object ProvisionalColors {

    // 封面 / 波形 / 视频预览渐变
    val GradientStart = ShiyinPrimary // PROVISIONAL：渐变起点，暂用主色
    val GradientEnd = Color(0xFF2F8F6E) // PROVISIONAL：渐变终点
    val WaveformGradientEnd = Color(0xFF7FC9A8) // PROVISIONAL：波形柱条末端
    const val GradientAngleDegrees = 135f // PROVISIONAL：linearGradient 角度

    // 语义色
    val Outline = ShiyinOnSurface.copy(alpha = 0.12f) // PROVISIONAL：分隔线/描边
    val OutlineVariant = ShiyinOnSurface.copy(alpha = 0.08f) // PROVISIONAL：弱描边
    val OnSurfaceVariant = ShiyinOnSurface.copy(alpha = 0.60f) // PROVISIONAL：次级文字
    val OnBackground = ShiyinOnSurface // PROVISIONAL：背景上的文字
    val SurfaceVariant = Color(0xFFEDF0EC) // PROVISIONAL：次级表面
    val OnPrimary = Color(0xFFFFFFFF) // PROVISIONAL：主色上的文字
    val OnPrimaryContainer = Color(0xFF0B3B2E) // PROVISIONAL
    val InverseSurface = Color(0xFF2D312F) // PROVISIONAL
    val InverseOnSurface = Color(0xFFF0F2EF) // PROVISIONAL
    val InversePrimary = Color(0xFF8FD0B6) // PROVISIONAL
    val Scrim = Color(0x99000000) // PROVISIONAL

    // 状态色
    val StateLayerPressed = ShiyinPrimary.copy(alpha = 0.12f) // PROVISIONAL
    val DisabledContent = ShiyinOnSurface.copy(alpha = 0.38f) // PROVISIONAL
    val DisabledContainer = ShiyinPrimary.copy(alpha = 0.38f) // PROVISIONAL
    val ShadowAmbient = ShiyinOnSurface.copy(alpha = 0.08f) // PROVISIONAL：环境阴影
    val ShadowSpot = ShiyinOnSurface.copy(alpha = 0.12f) // PROVISIONAL：点阴影
}
