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
import com.shiyin.player.data.model.Track
import com.shiyin.player.data.util.Formatters
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/** 02 屏「最近添加」列表项。时长用等宽 labelSmall（Geist Mono 分工）。 */
@Composable
fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artwork: ImageBitmap? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ProvisionalDimens.TrackListItemHeight)
            .clip(RoundedCornerShape(ShiyinDimens.CardCorner))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = ProvisionalDimens.CardInnerPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GradientAlbumArt(
            modifier = Modifier.size(ProvisionalDimens.CoverSizeThumbnail),
            corner = ShiyinDimens.ButtonCorner,
            seed = track.id,
            artwork = artwork
        )
        Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = Formatters.formatDuration(track.durationMs),
            style = MaterialTheme.typography.labelSmall
        )
    }
}
