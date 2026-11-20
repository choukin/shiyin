package com.shiyin.player.ui.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.shiyin.player.ui.theme.ProvisionalColors
import com.shiyin.player.ui.theme.ProvisionalDimens

/**
 * 黑胶唱片样式的播放封面：圆形封面缓慢旋转，播放时转、暂停时停在当前角度。
 *
 * - 真实封面（[artwork] / [artworkUri]）铺满唱片盘面，无封面时用渐变兜底；
 * - 中心轴孔 + 高光环模拟黑胶质感；
 * - 用 [Animatable] 而非 rememberInfiniteTransition，保证暂停后从当前角度续转，不回弹到 0°。
 */
@Composable
fun RotatingVinyl(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    artwork: ImageBitmap? = null,
    artworkUri: Uri? = null,
    size: Dp = ProvisionalDimens.CoverSizeLarge
) {
    val rotation = remember { Animatable(0f) }

    // 播放时无限匀速旋转；暂停时停住当前角度（不重置）。
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f * ROTATIONS_PER_LOOP,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = ROTATION_DURATION_MS,
                        easing = LinearEasing
                    )
                )
            )
        } else {
            rotation.stop()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .graphicsLayer { rotationZ = rotation.value },
        contentAlignment = Alignment.Center
    ) {
        // 唱片盘面：真实封面 or 渐变 + 抽象纹路兜底。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ProvisionalColors.GradientStart,
                            ProvisionalColors.GradientEnd
                        )
                    )
                )
        ) {
            when {
                artwork != null -> Image(
                    bitmap = artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                artworkUri != null -> AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // 黑胶同心纹路（半透明圆环），让"唱片"更可辨识。
            VinylGrooves()
        }

        // 中心轴孔：外圈亮环 + 中心深色孔。
        Box(
            modifier = Modifier
                .size(size * 0.16f)
                .clip(CircleShape)
                .background(Color(0x55FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.06f)
                    .clip(CircleShape)
                    .background(MaterialThemeSurfaceDark)
            )
        }
    }
}

/** 唱片的同心圆纹路，随盘面一起旋转。 */
@Composable
private fun VinylGrooves() {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(cx, cy)
        val ringColor = Color.White.copy(alpha = 0.10f)
        for (i in 1..8) {
            drawCircle(
                color = ringColor,
                radius = radius * (0.18f + i * 0.095f),
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f)
            )
        }
    }
}

private val MaterialThemeSurfaceDark = Color(0xFF1A1A1A)
private const val ROTATIONS_PER_LOOP = 1
private const val ROTATION_DURATION_MS = 12_000
