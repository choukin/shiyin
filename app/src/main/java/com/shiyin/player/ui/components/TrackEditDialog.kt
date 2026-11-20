package com.shiyin.player.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import com.shiyin.player.R
import com.shiyin.player.data.model.Track
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 曲目信息编辑：名称、简介、自定义海报。
 * 这三项都是拾音内的展示覆盖，不改写手机里的音频文件；弹窗内明确写出这一点。
 */
@Composable
fun TrackEditDialog(
    track: Track,
    onDismiss: () -> Unit,
    onSave: (String, String, Uri?, Boolean) -> Unit,
    resolveArtwork: suspend (Track) -> ImageBitmap? = { null },
    loadArtwork: suspend (Uri) -> ImageBitmap? = { null }
) {
    var title by remember(track.id) { mutableStateOf(track.title) }
    var description by remember(track.id) { mutableStateOf(track.description) }
    var pickedArtwork by remember(track.id) { mutableStateOf<Uri?>(null) }
    var removeArtwork by remember(track.id) { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pickedArtwork = uri
            removeArtwork = false
        }
    }

    val baseArtwork by produceState<ImageBitmap?>(initialValue = null, key1 = track.id) {
        value = resolveArtwork(track)
    }
    val preview by produceState<ImageBitmap?>(
        initialValue = baseArtwork,
        key1 = pickedArtwork,
        key2 = removeArtwork,
        key3 = baseArtwork
    ) {
        value = when {
            removeArtwork -> null
            pickedArtwork != null -> loadArtwork(pickedArtwork!!)
            else -> baseArtwork
        }
    }

    val coverShape = RoundedCornerShape(ShiyinDimens.CoverCorner)
    val hasCustomArtwork = pickedArtwork != null || (track.artworkUri != null && !removeArtwork)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.track_edit_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GradientAlbumArt(
                    modifier = Modifier
                        .size(ProvisionalDimens.EditCoverSize)
                        .clip(coverShape)
                        .border(
                            ProvisionalDimens.DividerThickness,
                            MaterialTheme.colorScheme.outlineVariant,
                            coverShape
                        )
                        .clickable { picker.launch("image/*") },
                    corner = ShiyinDimens.CoverCorner,
                    seed = track.id,
                    artwork = preview
                )
                Spacer(modifier = Modifier.height(ProvisionalDimens.SwipeActionIconGap))
                TextButton(onClick = { picker.launch("image/*") }) {
                    Text(stringResource(R.string.track_edit_pick_cover))
                }
                if (hasCustomArtwork) {
                    TextButton(onClick = {
                        pickedArtwork = null
                        removeArtwork = true
                    }) {
                        Text(stringResource(R.string.track_edit_clear_cover))
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.track_edit_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.track_edit_description_label)) },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                Text(
                    text = stringResource(R.string.track_edit_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(title, description, pickedArtwork, removeArtwork)
                onDismiss()
            }) {
                Text(stringResource(R.string.track_edit_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.playlist_cancel))
            }
        }
    )
}
