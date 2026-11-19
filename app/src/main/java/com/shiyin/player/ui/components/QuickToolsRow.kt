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

/** 02 屏快捷工具卡片行：4 个入口分别对应 03/04/05/06 路由。 */
@Composable
fun QuickToolsRow(
    tools: List<QuickTool>,
    onToolClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)
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
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    Text(
                        text = tool.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
