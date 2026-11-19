package com.shiyin.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 一期只做浅色（设计稿未给深色规范）。
 * 刻意不启用 dynamicColorScheme：Android 12+ 壁纸取色会改变全部色值，
 * 使像素级截图比对失去基线。
 *
 * ColorScheme 字段已在批次 B 阶段 1 统一补齐：设计稿给出的 5 个确定色值 +
 * [ProvisionalColors] 中的 PROVISIONAL 语义色。补齐只能在此处做，
 * 屏幕层不得单独覆盖 colorScheme。
 */
private val ShiyinColorScheme = lightColorScheme(
    primary = ShiyinPrimary,
    onPrimary = ProvisionalColors.OnPrimary,
    primaryContainer = ShiyinPrimaryContainer,
    onPrimaryContainer = ProvisionalColors.OnPrimaryContainer,
    secondary = ShiyinPrimary,
    onSecondary = ProvisionalColors.OnPrimary,
    secondaryContainer = ProvisionalColors.SurfaceVariant,
    onSecondaryContainer = ShiyinOnSurface,
    tertiary = ShiyinPrimary,
    onTertiary = ProvisionalColors.OnPrimary,
    tertiaryContainer = ProvisionalColors.SurfaceVariant,
    onTertiaryContainer = ShiyinOnSurface,
    background = ShiyinBackground,
    onBackground = ProvisionalColors.OnBackground,
    surface = ShiyinSurface,
    onSurface = ShiyinOnSurface,
    surfaceVariant = ProvisionalColors.SurfaceVariant,
    onSurfaceVariant = ProvisionalColors.OnSurfaceVariant,
    outline = ProvisionalColors.Outline,
    outlineVariant = ProvisionalColors.OutlineVariant,
    scrim = ProvisionalColors.Scrim,
    inverseSurface = ProvisionalColors.InverseSurface,
    inverseOnSurface = ProvisionalColors.InverseOnSurface,
    inversePrimary = ProvisionalColors.InversePrimary
)

/** 签名固定：只有 content 一个参数，不提供 darkTheme / dynamicColor。 */
@Composable
fun ShiyinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShiyinColorScheme,
        typography = ShiyinTypography,
        shapes = ShiyinShapes,
        content = content
    )
}
