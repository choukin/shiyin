package com.shiyin.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.shiyin.player.ui.theme.ProvisionalDimens

/**
 * 二期真实波形：振幅来自 WaveformExtractor 解码的 PCM RMS，
 * 选区内的柱条用主题主色高亮，选区外为 outlineVariant。
 * 替代一期 sin() 伪随机的 WaveformPlaceholder（后者仍作为解码失败时的降级）。
 */
@Composable
fun WaveformView(
    amplitudes: List<Float>,
    selectionStartRatio: Float,
    selectionEndRatio: Float,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ProvisionalDimens.WaveformHeight)
    ) {
        val count = amplitudes.size.coerceAtLeast(1)
        val slot = size.width / count
        val barWidth = (slot * BAR_WIDTH_RATIO).coerceAtLeast(MIN_BAR_WIDTH_PX)
        amplitudes.forEachIndexed { index, amplitude ->
            val ratio = index.toFloat() / count
            val color = if (ratio >= selectionStartRatio && ratio <= selectionEndRatio) {
                activeColor
            } else {
                inactiveColor
            }
            val barHeight = (size.height * amplitude).coerceAtLeast(MIN_BAR_HEIGHT_PX)
            drawRect(
                color = color,
                topLeft = Offset(
                    x = index * slot + (slot - barWidth) / 2f,
                    y = (size.height - barHeight) / 2f
                ),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

private const val BAR_WIDTH_RATIO = 0.6f
private const val MIN_BAR_WIDTH_PX = 1f
private const val MIN_BAR_HEIGHT_PX = 2f
