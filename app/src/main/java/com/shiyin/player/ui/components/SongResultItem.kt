package com.shiyin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import com.shiyin.player.data.model.SongResult
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 04 下载页搜歌结果行。
 *
 * 刻意不加载远程封面：站点的 pic 是 http 且带防盗链，项目当前不引网络图片库；
 * 用与曲库一致的自绘音符图标占位，避免「加载不出来」的空白卡片。
 */
@Composable
fun SongResultItem(
    song: SongResult,
    isPreviewing: Boolean,
    isPlaying: Boolean,
    isDownloading: Boolean,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShiyinDimens.CardCorner),
        color = if (isPreviewing) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ProvisionalDimens.SongResultItemHeight)
                .padding(horizontal = ProvisionalDimens.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(ProvisionalDimens.CoverSizeThumbnail)
                    .clip(RoundedCornerShape(ShiyinDimens.CardCorner))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                ShiyinGlyph(
                    glyph = Glyph.Music,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                )
            }
            Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onPreview) {
                ShiyinGlyph(
                    glyph = if (isPreviewing && isPlaying) Glyph.Pause else Glyph.Play,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                )
            }
            IconButton(onClick = onDownload, enabled = !isDownloading) {
                ShiyinGlyph(
                    glyph = Glyph.Download,
                    tint = if (isDownloading) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                )
            }
        }
    }
}
