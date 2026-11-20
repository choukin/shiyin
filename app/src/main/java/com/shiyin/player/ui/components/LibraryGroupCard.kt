package com.shiyin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 专辑 / 歌手分组卡片：与曲目行等高、同一圆角与内边距，
 * 右侧弱化的 chevron 提示可进入二级页。封面沿用 GradientAlbumArt 的三级兜底。
 */
@Composable
fun LibraryGroupCard(
    name: String,
    countLabel: String,
    artwork: ImageBitmap?,
    seed: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ProvisionalDimens.GroupItemHeight)
            .clip(RoundedCornerShape(ShiyinDimens.CardCorner))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = ProvisionalDimens.CardInnerPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GradientAlbumArt(
            modifier = Modifier.size(ProvisionalDimens.CoverSizeThumbnail),
            corner = ShiyinDimens.ButtonCorner,
            seed = seed,
            artwork = artwork
        )
        Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ShiyinGlyph(
            glyph = Glyph.Chevron,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ProvisionalDimens.IconSizeSmall)
        )
    }
}
