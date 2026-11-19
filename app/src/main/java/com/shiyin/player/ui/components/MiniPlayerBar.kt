package com.shiyin.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.shiyin.player.data.model.Track
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/** 02 屏底部迷你播放条：点击整条进 07 播放器，右侧按钮切换播放/暂停。 */
@Composable
fun MiniPlayerBar(
    track: Track?,
    isPlaying: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(ProvisionalDimens.MiniPlayerHeight),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = ProvisionalDimens.ElevationRaised
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick)
                .padding(horizontal = ProvisionalDimens.ScreenHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientAlbumArt(
                modifier = Modifier.size(ProvisionalDimens.CoverSizeMini),
                corner = ShiyinDimens.ButtonCorner,
                seed = track?.id ?: 0L
            )
            Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track?.title ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track?.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onToggle) {
                ShiyinGlyph(
                    glyph = if (isPlaying) Glyph.Pause else Glyph.Play,
                    modifier = Modifier.size(ProvisionalDimens.IconSizeMedium),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
