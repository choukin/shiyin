package com.shiyin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

data class QuickTool(val label: String, val route: String, val glyph: Glyph)

/**
 * 02 屏快捷工具行。
 *
 * 入口现已从 4 个增加到 5 个（03 扫描 / 歌单 / 04 下载 / 05 裁剪 / 06 视频提取音频）。
 * 早期实现是「固定 84dp 卡片 + 横向滚动」，390dp 屏可用宽只有 350dp，
 * 5 张卡片需 468dp，导致最后一个「视频提取音频」被完全挤出首屏、看起来像功能消失。
 * 现改为等分铺满：无论屏宽多少，所有入口都在首屏可见，且后续新增入口不会再被藏起来。
 * 标签最多两行，以容纳「视频提取音频」这类较长名称。
 */
@Composable
fun QuickToolsRow(
    tools: List<QuickTool>,
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ProvisionalDimens.QuickToolSpacing)
    ) {
        tools.forEach { tool ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(ProvisionalDimens.QuickToolCardHeight)
                    .clip(RoundedCornerShape(ShiyinDimens.CardCorner))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onToolClick(tool.route) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShiyinGlyph(
                        glyph = tool.glyph,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(ProvisionalDimens.IconSizeLarge)
                    )
                    Spacer(modifier = Modifier.height(ProvisionalDimens.QuickToolIconLabelGap))
                    Text(
                        text = tool.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        minLines = 2
                    )
                }
            }
        }
    }
}
