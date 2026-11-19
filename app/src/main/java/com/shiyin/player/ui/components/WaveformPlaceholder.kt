package com.shiyin.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.shiyin.player.ui.theme.ProvisionalColors
import com.shiyin.player.ui.theme.ProvisionalDimens
import kotlin.math.abs
import kotlin.math.sin

/** 05 屏音乐裁剪的静态波形示意（二期才接真实 PCM 波形与选区）。 */
@Composable
fun WaveformPlaceholder(
    modifier: Modifier = Modifier,
    barCount: Int = 48
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ProvisionalDimens.WaveformHeight)
    ) {
        val step = size.width / barCount
        val barWidth = step * 0.55f
        val brush = Brush.verticalGradient(
            listOf(ProvisionalColors.GradientStart, ProvisionalColors.WaveformGradientEnd)
        )
        repeat(barCount) { index ->
            val amplitude = 0.15f + abs(sin(index * 0.45f)) * 0.8f
            val barHeight = size.height * amplitude
            drawRoundRect(
                brush = brush,
                topLeft = Offset(index * step, (size.height - barHeight) / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}
