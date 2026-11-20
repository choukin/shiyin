package com.shiyin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.launch

/**
 * 02 屏曲目列表项。
 * - 普通点击：播放；星标 / 长按菜单：收藏；
 * - 左滑：停住并露出「编辑」「删除」两个按钮，点击才执行，避免误删；
 * - 长按菜单：收藏、编辑、删除、永久删除手机文件。
 *
 * 编辑 / 删除的具体交互由曲库页的弹窗承载，这里只负责手势与入口。
 */
@Composable
fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteRequest: (DeleteMode) -> Unit,
    modifier: Modifier = Modifier,
    artwork: ImageBitmap? = null,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val favoriteLabel = stringResource(
        if (isFavorite) R.string.track_unfavorite else R.string.track_favorite
    )
    // MediaStore 对缺失元数据返回 <unknown>，界面上不展示这类占位值。
    val artistLabel = if (LibraryGrouping.normalize(track.artist).isEmpty()) {
        stringResource(R.string.library_unknown_artist)
    } else {
        track.artist
    }
    val subtitle = if (track.description.isBlank()) {
        artistLabel
    } else {
        "$artistLabel · ${track.description}"
    }

    // 允许停在 EndToStart 展开态，也可以滑回；不在滑动时直接删除，删除由露出的按钮触发。
    // 右滑方向由 enableDismissFromStartToEnd=false 关闭，无需废弃的 confirmValueChange veto。
    val dismissState = rememberSwipeToDismissBoxState()
    val collapse: () -> Unit = {
        scope.launch { dismissState.reset() }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val shape = RoundedCornerShape(ShiyinDimens.CardCorner)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = ProvisionalDimens.SwipeActionSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                SwipeAction(
                    label = stringResource(R.string.track_action_edit),
                    glyph = Glyph.Edit,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = {
                        collapse()
                        onEditClick()
                    }
                )
                Spacer(modifier = Modifier.width(ProvisionalDimens.SwipeActionSpacing))
                SwipeAction(
                    label = stringResource(R.string.track_action_delete),
                    glyph = Glyph.Delete,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = {
                        collapse()
                        onDeleteClick()
                    }
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true
    ) {
        // 完全展开时前景已移出屏幕，直接隐藏，避免左边缘残留一小块曲目行。
        Box(
            modifier = Modifier.alpha(
                if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) 0f else 1f
            )
        ) {
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
                        text = subtitle,
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
                    text = { Text(stringResource(R.string.track_action_edit)) },
                    onClick = {
                        menuExpanded = false
                        onEditClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.track_action_delete)) },
                    onClick = {
                        menuExpanded = false
                        onDeleteClick()
                    }
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

/** 左滑后露出的操作按钮：单一表面 + 1dp 描边，不叠渐变、不靠阴影分层。 */
@Composable
private fun SwipeAction(
    label: String,
    glyph: Glyph,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(ShiyinDimens.ButtonCorner)
    Box(
        modifier = Modifier
            .size(
                width = ProvisionalDimens.SwipeActionWidth,
                height = ProvisionalDimens.TrackListItemHeight
            )
            .clip(shape)
            .background(containerColor)
            .border(ProvisionalDimens.DividerThickness, MaterialTheme.colorScheme.outlineVariant, shape)
            .semantics { contentDescription = label }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ShiyinGlyph(
                glyph = glyph,
                tint = contentColor,
                modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
            )
            Spacer(modifier = Modifier.height(ProvisionalDimens.SwipeActionIconGap))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}
