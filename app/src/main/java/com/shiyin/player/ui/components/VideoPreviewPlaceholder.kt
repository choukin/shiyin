package com.shiyin.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import com.shiyin.player.ui.theme.ProvisionalColors
import com.shiyin.player.ui.theme.ShiyinDimens

/** 06 屏视频提取音频的矢量预览占位：16:9 渐变画板 + 播放三角。 */
@Composable
fun VideoPreviewPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(ShiyinDimens.CardCorner))
            .background(
                Brush.linearGradient(
                    listOf(ProvisionalColors.GradientStart, ProvisionalColors.GradientEnd)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawPath(
                path = Path().apply {
                    moveTo(w * 0.43f, h * 0.34f)
                    lineTo(w * 0.64f, h * 0.5f)
                    lineTo(w * 0.43f, h * 0.66f)
                    close()
                },
                color = ProvisionalColors.OnPrimary
            )
        }
    }
}
