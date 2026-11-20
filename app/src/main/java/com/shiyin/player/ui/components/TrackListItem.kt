package com.shiyin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.shiyin.player.R
import com.shiyin.player.data.media.MediaDeleteRepository.DeleteMode
import com.shiyin.player.data.model.Track
import com.shiyin.player.data.util.Formatters
import com.shiyin.player.data.util.LibraryGrouping
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 02 屏曲目列表项。
 * - 普通点击：播放；
 * - 星标 / 长按菜单：收藏或取消收藏；
 * - 左滑：删除（移到回收站，系统确认弹窗）；
 * - 长按：菜单提供收藏、移到回收站与永久删除。
 * 时长用等宽 labelSmall（Geist Mono 分工）。
 */
@Composable
fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    onDeleteRequest: (DeleteMode) -> Unit,
    modifier: Modifier = Modifier,
    artwork: ImageBitmap? = null,
    canTrash: Boolean = true,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val favoriteLabel = stringResource(
        if (isFavorite) R.string.track_unfavorite else R.string.track_favorite
    )
    // MediaStore 对缺失元数据返回 <unknown>，界面上不展示这类占位值。
    val artistLabel = if (LibraryGrouping.normalize(track.artist).isEmpty()) {
        stringResource(R.string.library_unknown_artist)
    } else {
        track.artist
    }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd ||
                value == SwipeToDismissBoxValue.EndToStart
            ) {
                onDeleteRequest(if (canTrash) DeleteMode.Trash else DeleteMode.Permanent)
                // 返回 false：不让 item 停留在 dismissed 态，删除成功后由列表刷新移除。
                false
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val color = if (
                dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart ||
                dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            ) MaterialTheme.colorScheme.errorContainer else Color.Transparent
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(ShiyinDimens.CardCorner))
                    .background(color)
                    .padding(horizontal = ProvisionalDimens.CardInnerPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.track_delete_swipe_hint),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ProvisionalDimens.TrackListItemHeight)
                    .clip(RoundedCornerShape(ShiyinDimens.CardCorner))
                    .background(MaterialTheme.colorScheme.surface)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = { menuExpanded = true }
                    )
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
                        text = artistLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 收藏星标：填充态用主色，未收藏用弱化的次级文字色。
                Box(
                    modifier = Modifier
                        .size(ProvisionalDimens.FavoriteIconSize)
                        .clip(RoundedCornerShape(ShiyinDimens.ButtonCorner))
                        .semantics { contentDescription = favoriteLabel }
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    ShiyinGlyph(
                        glyph = if (isFavorite) Glyph.FavoriteFilled else Glyph.Favorite,
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(ProvisionalDimens.IconSizeSmall)
                    )
                }
                Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
                Text(
                    text = Formatters.formatDuration(track.durationMs),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(favoriteLabel) },
                    onClick = {
                        menuExpanded = false
                        onToggleFavorite()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.track_delete_trash)) },
                    onClick = {
                        menuExpanded = false
                        onDeleteRequest(DeleteMode.Trash)
                    },
                    enabled = canTrash
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.track_delete_permanent)) },
                    onClick = {
                        menuExpanded = false
                        onDeleteRequest(DeleteMode.Permanent)
                    }
                )
            }
        }
    }
}
