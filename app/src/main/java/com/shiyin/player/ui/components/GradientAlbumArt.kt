package com.shiyin.player.ui.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.shiyin.player.ui.theme.ProvisionalColors
import com.shiyin.player.ui.theme.ShiyinDimens
import kotlin.math.abs

/**
 * 渐变封面（设计稿用矢量 SVG + 渐变，Android 端用 Canvas + Brush 自绘）。
 *
 * 三级兜底：有 content:// 封面 → Coil3 AsyncImage 叠加；
 * 加载失败 / 无封面 → 渐变 + 抽象音波始终可见。Coil 3 core 内置 content 取图器，
 * 一期不需要 coil-network-okhttp，也不需要 INTERNET 权限。
 */
@Composable
fun GradientAlbumArt(
    modifier: Modifier = Modifier,
    artworkUri: Uri? = null,
    artwork: ImageBitmap? = null,
    corner: Dp = ShiyinDimens.CoverCorner,
    seed: Long = 0L
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(
                Brush.linearGradient(
                    colors = listOf(ProvisionalColors.GradientStart, ProvisionalColors.GradientEnd)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barCount = 7
            val barWidth = size.width / (barCount * 3f)
            repeat(barCount) { index ->
                val pseudo = abs((index * 37 + seed.toInt()) % 50) / 100f
                val barHeight = size.height * (0.18f + pseudo)
                drawRoundRect(
                    color = ProvisionalColors.OnPrimary.copy(alpha = 0.16f),
                    topLeft = Offset(
                        x = size.width * (index + 0.5f) / barCount - barWidth / 2,
                        y = (size.height - barHeight) / 2
                    ),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2)
                )
            }
        }
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (artworkUri != null) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
