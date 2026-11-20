package com.shiyin.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * 图标全部 Canvas 自绘：一期不引 material-icons-extended，也不放 SVG 资源。
 * 每个 glyph 用相对坐标绘制，尺寸由调用方通过 ProvisionalDimens 给定。
 */
enum class Glyph {
    Back, Close, Play, Pause, Next, Previous,
    Scan, Link, Trim, Extract, Music, Info, Search, Playlist,
    Favorite, FavoriteFilled, Chevron
}

@Composable
fun ShiyinGlyph(
    glyph: Glyph,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Canvas(modifier = modifier) { drawGlyph(glyph, tint) }
}

private fun DrawScope.drawGlyph(glyph: Glyph, color: Color) {
    val w = size.width
    val h = size.height
    val line = w * 0.075f
    when (glyph) {
        Glyph.Play -> drawPath(
            Path().apply {
                moveTo(w * 0.32f, h * 0.20f)
                lineTo(w * 0.80f, h * 0.50f)
                lineTo(w * 0.32f, h * 0.80f)
                close()
            },
            color
        )

        Glyph.Pause -> {
            drawLine(color, Offset(w * 0.36f, h * 0.20f), Offset(w * 0.36f, h * 0.80f), line * 2f, StrokeCap.Round)
            drawLine(color, Offset(w * 0.66f, h * 0.20f), Offset(w * 0.66f, h * 0.80f), line * 2f, StrokeCap.Round)
        }

        Glyph.Next -> {
            drawPath(
                Path().apply {
                    moveTo(w * 0.24f, h * 0.24f)
                    lineTo(w * 0.66f, h * 0.50f)
                    lineTo(w * 0.24f, h * 0.76f)
                    close()
                },
                color
            )
            drawLine(color, Offset(w * 0.78f, h * 0.22f), Offset(w * 0.78f, h * 0.78f), line, StrokeCap.Round)
        }

        Glyph.Previous -> {
            drawPath(
                Path().apply {
                    moveTo(w * 0.76f, h * 0.24f)
                    lineTo(w * 0.34f, h * 0.50f)
                    lineTo(w * 0.76f, h * 0.76f)
                    close()
                },
                color
            )
            drawLine(color, Offset(w * 0.22f, h * 0.22f), Offset(w * 0.22f, h * 0.78f), line, StrokeCap.Round)
        }

        Glyph.Back -> {
            drawLine(color, Offset(w * 0.72f, h * 0.50f), Offset(w * 0.24f, h * 0.50f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.44f, h * 0.28f), Offset(w * 0.20f, h * 0.50f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.44f, h * 0.72f), Offset(w * 0.20f, h * 0.50f), line, StrokeCap.Round)
        }

        Glyph.Close -> {
            drawLine(color, Offset(w * 0.26f, h * 0.26f), Offset(w * 0.74f, h * 0.74f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.74f, h * 0.26f), Offset(w * 0.26f, h * 0.74f), line, StrokeCap.Round)
        }

        Glyph.Scan -> {
            val inset = w * 0.18f
            val arm = w * 0.20f
            drawLine(color, Offset(inset, inset + arm), Offset(inset, inset), line, StrokeCap.Round)
            drawLine(color, Offset(inset, inset), Offset(inset + arm, inset), line, StrokeCap.Round)
            drawLine(color, Offset(w - inset - arm, inset), Offset(w - inset, inset), line, StrokeCap.Round)
            drawLine(color, Offset(w - inset, inset), Offset(w - inset, inset + arm), line, StrokeCap.Round)
            drawLine(color, Offset(inset, h - inset), Offset(inset, h - inset - arm), line, StrokeCap.Round)
            drawLine(color, Offset(inset, h - inset), Offset(inset + arm, h - inset), line, StrokeCap.Round)
            drawLine(color, Offset(w - inset, h - inset - arm), Offset(w - inset, h - inset), line, StrokeCap.Round)
            drawLine(color, Offset(w - inset, h - inset), Offset(w - inset - arm, h - inset), line, StrokeCap.Round)
            drawLine(color, Offset(inset, h * 0.5f), Offset(w - inset, h * 0.5f), line * 0.7f, StrokeCap.Round)
        }

        Glyph.Link -> {
            drawLine(color, Offset(w * 0.30f, h * 0.62f), Offset(w * 0.62f, h * 0.30f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.26f, h * 0.44f), Offset(w * 0.26f, h * 0.70f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.26f, h * 0.70f), Offset(w * 0.52f, h * 0.70f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.74f, h * 0.30f), Offset(w * 0.74f, h * 0.56f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.48f, h * 0.56f), Offset(w * 0.74f, h * 0.56f), line, StrokeCap.Round)
        }

        Glyph.Trim -> {
            drawLine(color, Offset(w * 0.22f, h * 0.18f), Offset(w * 0.22f, h * 0.82f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.78f, h * 0.18f), Offset(w * 0.78f, h * 0.82f), line, StrokeCap.Round)
            listOf(0.36f, 0.50f, 0.64f).forEachIndexed { index, x ->
                val amplitude = h * (0.18f + index * 0.10f)
                drawLine(
                    color,
                    Offset(w * x, h * 0.5f - amplitude / 2),
                    Offset(w * x, h * 0.5f + amplitude / 2),
                    line,
                    StrokeCap.Round
                )
            }
        }

        Glyph.Extract -> {
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.14f, h * 0.22f),
                size = Size(w * 0.72f, h * 0.56f),
                cornerRadius = CornerRadius(w * 0.08f),
                alpha = 0.35f
            )
            drawPath(
                Path().apply {
                    moveTo(w * 0.42f, h * 0.38f)
                    lineTo(w * 0.68f, h * 0.50f)
                    lineTo(w * 0.42f, h * 0.62f)
                    close()
                },
                color
            )
        }

        Glyph.Music -> {
            drawLine(color, Offset(w * 0.62f, h * 0.20f), Offset(w * 0.62f, h * 0.66f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.62f, h * 0.20f), Offset(w * 0.78f, h * 0.28f), line, StrokeCap.Round)
            drawCircle(color, radius = w * 0.13f, center = Offset(w * 0.44f, h * 0.70f))
        }

        Glyph.Info -> {
            drawCircle(color, radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.5f), alpha = 0.25f)
            drawLine(color, Offset(w * 0.5f, h * 0.44f), Offset(w * 0.5f, h * 0.68f), line, StrokeCap.Round)
            drawCircle(color, radius = line * 0.7f, center = Offset(w * 0.5f, h * 0.34f))
        }

        Glyph.Search -> {
            drawCircle(color, radius = w * 0.24f, center = Offset(w * 0.44f, h * 0.44f), alpha = 0.9f)
            drawLine(color, Offset(w * 0.62f, h * 0.62f), Offset(w * 0.80f, h * 0.80f), line, StrokeCap.Round)
        }

        Glyph.Playlist -> {
            // 三条横线 + 一个小音符，表示"歌曲列表"。
            drawLine(color, Offset(w * 0.20f, h * 0.30f), Offset(w * 0.66f, h * 0.30f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.66f, h * 0.50f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.20f, h * 0.70f), Offset(w * 0.50f, h * 0.70f), line, StrokeCap.Round)
            drawCircle(color, radius = w * 0.09f, center = Offset(w * 0.70f, h * 0.76f))
            drawLine(color, Offset(w * 0.79f, h * 0.76f), Offset(w * 0.79f, h * 0.40f), line, StrokeCap.Round)
        }

        Glyph.Favorite, Glyph.FavoriteFilled -> {
            // 五角星：外半径 0.42、内半径 0.19，中心 (0.5, 0.5)，起始角 -90°。
            val star = Path().apply {
                moveTo(w * 0.5000f, h * 0.0800f)
                lineTo(w * 0.6117f, h * 0.3463f)
                lineTo(w * 0.8995f, h * 0.3702f)
                lineTo(w * 0.6807f, h * 0.5587f)
                lineTo(w * 0.7469f, h * 0.8398f)
                lineTo(w * 0.5000f, h * 0.6900f)
                lineTo(w * 0.2531f, h * 0.8398f)
                lineTo(w * 0.3193f, h * 0.5587f)
                lineTo(w * 0.1005f, h * 0.3702f)
                lineTo(w * 0.3883f, h * 0.3463f)
                close()
            }
            if (glyph == Glyph.FavoriteFilled) {
                drawPath(star, color)
            } else {
                drawPath(star, color, style = Stroke(width = line))
            }
        }

        Glyph.Chevron -> {
            drawLine(color, Offset(w * 0.42f, h * 0.28f), Offset(w * 0.64f, h * 0.50f), line, StrokeCap.Round)
            drawLine(color, Offset(w * 0.64f, h * 0.50f), Offset(w * 0.42f, h * 0.72f), line, StrokeCap.Round)
        }
    }
}
