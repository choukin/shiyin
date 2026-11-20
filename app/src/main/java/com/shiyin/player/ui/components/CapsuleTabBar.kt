package com.shiyin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shiyin.player.ui.theme.ProvisionalColors
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinCapsuleShape

/**
 * 02 屏胶囊 Tab Bar（标签文案规格未给出，由调用方按 PROVISIONAL 传入）。
 *
 * 选中态按 unslop「small surface step + hairline highlight」：低饱和
 * primaryContainer 底 + 1dp 发丝描边 + primary 文字，不再整块铺满高饱和主色。
 * 主色只落在文字上，chrome 保持安静。
 */
@Composable
fun CapsuleTabBar(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ProvisionalDimens.CapsuleTabHeight)
            .clip(ShiyinCapsuleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        tabs.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(ShiyinCapsuleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                    .then(
                        if (selected) {
                            Modifier.border(
                                width = 1.dp,
                                color = ProvisionalColors.Outline,
                                shape = ShiyinCapsuleShape
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}
