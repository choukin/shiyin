package com.shiyin.player.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shiyin.player.R
import com.shiyin.player.data.model.Playlist
import com.shiyin.player.data.model.Track
import com.shiyin.player.ui.components.Glyph
import com.shiyin.player.ui.components.ShiyinGlyph
import com.shiyin.player.ui.components.ShiyinScaffold
import com.shiyin.player.ui.theme.ProvisionalDimens

/**
 * 09 歌单页（基础闭环）：列表 / 创建 / 加歌 / 播放整单 / 删除 / 移除单曲。
 * 两级导航：未选中显示歌单列表，选中后显示该歌单曲目。
 */
@Composable
fun PlaylistScreen(
    state: PlaylistUiState,
    onCreate: (String) -> Unit,
    onShowCreateDialog: (Boolean) -> Unit,
    onSelect: (String?) -> Unit,
    onDelete: (String) -> Unit,
    onAddTracks: (String, List<Long>) -> Unit,
    onRemoveTrack: (String, Long) -> Unit,
    onPlay: (Playlist) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = state.selected
    val title = if (selected != null) selected.name else stringResource(R.string.playlist_title)

    ShiyinScaffold(
        title = title,
        onBack = { if (selected != null) onSelect(null) else onBack() },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ProvisionalDimens.ScreenHorizontalPadding)
        ) {
            if (selected == null) {
                PlaylistList(
                    playlists = state.playlists,
                    onCreateClick = { onShowCreateDialog(true) },
                    onSelect = onSelect,
                    onDelete = onDelete
                )
            } else {
                PlaylistDetail(
                    playlist = selected,
                    allTracks = state.allTracks,
                    onAddTracks = { ids -> onAddTracks(selected.id, ids) },
                    onRemoveTrack = { id -> onRemoveTrack(selected.id, id) },
                    onPlay = { onPlay(selected) }
                )
            }
        }

        if (state.showCreateDialog) {
            CreatePlaylistDialog(
                onCreate = onCreate,
                onDismiss = { onShowCreateDialog(false) }
            )
        }
    }
}

@Composable
private fun PlaylistList(
    playlists: List<Playlist>,
    onCreateClick: () -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = onCreateClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.playlist_create))
        }
        Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
        if (playlists.isEmpty()) {
            Text(
                text = stringResource(R.string.playlist_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)
            ) {
                items(playlists, key = { it.id }) { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(playlist.id) }
                            .padding(vertical = ProvisionalDimens.ItemVerticalSpacing / 2),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShiyinGlyph(
                            glyph = Glyph.Music,
                            modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(playlist.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                stringResource(R.string.playlist_track_count, playlist.trackIds.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDelete(playlist.id) }) {
                            ShiyinGlyph(
                                glyph = Glyph.Close,
                                modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    playlist: Playlist,
    allTracks: List<Track>,
    onAddTracks: (List<Long>) -> Unit,
    onRemoveTrack: (Long) -> Unit,
    onPlay: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val trackIdsInPlaylist = remember(playlist.trackIds) { playlist.trackIds.toSet() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)) {
            Button(onClick = onPlay, enabled = playlist.trackIds.isNotEmpty()) {
                Text(stringResource(R.string.playlist_play_all))
            }
            Button(onClick = { showAddDialog = true }) {
                Text(stringResource(R.string.playlist_add_songs))
            }
        }
        Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
        if (playlist.trackIds.isEmpty()) {
            Text(
                text = stringResource(R.string.playlist_no_songs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val trackById = remember(allTracks) { allTracks.associateBy { it.id } }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)
            ) {
                items(playlist.trackIds, key = { it }) { id ->
                    val track = trackById[id]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = track?.title ?: stringResource(R.string.playlist_missing_track),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRemoveTrack(id) }) {
                            ShiyinGlyph(
                                glyph = Glyph.Close,
                                modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSongsDialog(
            allTracks = allTracks,
            alreadyIn = trackIdsInPlaylist,
            onConfirm = { ids ->
                onAddTracks(ids)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun CreatePlaylistDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_create)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name_label)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }) {
                Text(stringResource(R.string.playlist_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.playlist_cancel)) }
        }
    )
}

@Composable
private fun AddSongsDialog(
    allTracks: List<Track>,
    alreadyIn: Set<Long>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val selected = remember { mutableStateOf(emptySet<Long>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.playlist_add_songs)) },
        text = {
            LazyColumn(modifier = Modifier.height(ProvisionalDimens.QuickToolCardHeight * 3)) {
                items(allTracks, key = { it.id }) { track ->
                    val checked = track.id in selected.value || track.id in alreadyIn
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (track.id !in alreadyIn) {
                                    selected.value = if (track.id in selected.value) {
                                        selected.value - track.id
                                    } else {
                                        selected.value + track.id
                                    }
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            enabled = track.id !in alreadyIn,
                            onCheckedChange = {
                                selected.value = if (track.id in selected.value) {
                                    selected.value - track.id
                                } else {
                                    selected.value + track.id
                                }
                            }
                        )
                        Text(track.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected.value.toList()) }) {
                Text(stringResource(R.string.playlist_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.playlist_cancel)) }
        }
    )
}
