package com.shiyin.player.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.shiyin.player.ui.theme.ProvisionalDimens

/** 自绘顶栏，刻意不用实验性的 TopAppBar，避免 Material3 API 差异导致编译失败。 */
@Composable
fun ShiyinTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(ProvisionalDimens.TopBarHeight)
                .padding(horizontal = ProvisionalDimens.ScreenHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    ShiyinGlyph(
                        glyph = Glyph.Back,
                        modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                    )
                }
                Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}
