package com.shiyin.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.shiyin.player.R
import com.shiyin.player.data.model.Track
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 删除曲目时先问清范围：只从曲库列表移除，还是连同手机里的文件一起删。
 * 后者仍会再触发一次 MediaStore 系统确认框。
 */
@Composable
fun TrackDeleteDialog(
    track: Track,
    canTrash: Boolean,
    onDismiss: () -> Unit,
    onRemoveFromList: () -> Unit,
    onDeleteFile: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.track_delete_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)) {
                Text(
                    text = stringResource(R.string.track_delete_dialog_body, track.title),
                    style = MaterialTheme.typography.bodyMedium
                )
                DeleteChoice(
                    label = stringResource(R.string.track_delete_from_list),
                    detail = stringResource(R.string.track_delete_from_list_detail),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = {
                        onRemoveFromList()
                        onDismiss()
                    }
                )
                DeleteChoice(
                    label = stringResource(R.string.track_delete_phone_file),
                    detail = stringResource(
                        if (canTrash) {
                            R.string.track_delete_phone_file_detail_trash
                        } else {
                            R.string.track_delete_phone_file_detail_permanent
                        }
                    ),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = {
                        onDeleteFile()
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.playlist_cancel))
            }
        }
    )
}

@Composable
private fun DeleteChoice(
    label: String,
    detail: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(ShiyinDimens.ButtonCorner)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(ProvisionalDimens.DividerThickness, MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick)
            .padding(ProvisionalDimens.CardInnerPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            Spacer(modifier = Modifier.size(ProvisionalDimens.SwipeActionIconGap))
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = contentColor)
        }
        Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
        ShiyinGlyph(
            glyph = Glyph.Chevron,
            tint = contentColor,
            modifier = Modifier.size(ProvisionalDimens.IconSizeSmall)
        )
    }
}
