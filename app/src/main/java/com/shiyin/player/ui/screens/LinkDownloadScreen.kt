package com.shiyin.player.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.shiyin.player.R
import com.shiyin.player.data.model.DownloadError
import com.shiyin.player.data.model.DownloadUiState
import com.shiyin.player.data.model.ExportState
import com.shiyin.player.data.model.SongResult
import com.shiyin.player.data.util.Formatters
import com.shiyin.player.ui.components.CapsuleTabBar
import com.shiyin.player.ui.components.EmptyState
import com.shiyin.player.ui.components.ShiyinScaffold
import com.shiyin.player.ui.components.SongResultItem
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 04 下载页：主入口「搜歌」（搜索 → 试听 → 下载入库），次要入口「直链」与 SAF 本地导入。
 *
 * 两个模式共用一条状态流与一个下载 Job，避免并发写媒体库。
 * 尺寸全部取自 ProvisionalDimens / ShiyinDimens，屏幕层零魔数、零硬编码色值。
 */
@Composable
fun LinkDownloadScreen(
    state: DownloadUiState,
    onModeChange: (Int) -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onPreviewClick: (SongResult) -> Unit,
    onStopPreview: () -> Unit,
    onDownloadSong: (SongResult) -> Unit,
    onDismissSaved: () -> Unit,
    onConsumeSearchError: () -> Unit,
    onUrlChange: (String) -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onImport: (Uri?) -> Unit,
    onReset: () -> Unit,
    onConsumeError: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> onImport(uri) }
    val tabs = listOf(
        stringResource(R.string.download_tab_search),
        stringResource(R.string.download_tab_link)
    )

    ShiyinScaffold(
        title = stringResource(R.string.link_download),
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = ProvisionalDimens.ScreenHorizontalPadding,
                vertical = ProvisionalDimens.ContentVerticalPadding
            ),
            verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)
        ) {
            item {
                CapsuleTabBar(tabs = tabs, selectedIndex = state.mode, onSelected = onModeChange)
            }
            if (state.mode == MODE_SEARCH) {
                searchSection(
                    state = state,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    onLoadMore = onLoadMore,
                    onPreviewClick = onPreviewClick,
                    onStopPreview = onStopPreview,
                    onDownloadSong = onDownloadSong,
                    onCancel = onCancel,
                    onDismissSaved = onDismissSaved,
                    onConsumeSearchError = onConsumeSearchError
                )
            } else {
                linkSection(
                    state = state,
                    onUrlChange = onUrlChange,
                    onDownload = onDownload,
                    onCancel = onCancel,
                    onImport = { picker.launch(arrayOf(MIME_AUDIO)) },
                    onReset = onReset,
                    onConsumeError = onConsumeError
                )
            }
        }
    }
}

private fun LazyListScope.searchSection(
    state: DownloadUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLoadMore: () -> Unit,
    onPreviewClick: (SongResult) -> Unit,
    onStopPreview: () -> Unit,
    onDownloadSong: (SongResult) -> Unit,
    onCancel: () -> Unit,
    onDismissSaved: () -> Unit,
    onConsumeSearchError: () -> Unit
) {
    item {
        InfoCard(text = stringResource(R.string.download_search_body))
    }
    item {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ShiyinDimens.CardCorner),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = stringResource(R.string.download_notice),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
            )
        }
    }
    item {
        Column {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(text = stringResource(R.string.download_search_label)) },
                placeholder = { Text(text = stringResource(R.string.download_search_hint)) },
                singleLine = true,
                enabled = !state.isSearching,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
            Button(
                onClick = onSearch,
                enabled = !state.isSearching && state.query.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.download_search_action))
            }
        }
    }
    item {
        Text(
            text = stringResource(R.string.download_source_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    if (state.isSearching) {
        item {
            Column {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                Text(
                    text = stringResource(R.string.download_searching),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 试听条：独立于曲库播放队列，显示实时进度与停止入口。
    if (state.previewId != null) {
        item {
            val progress = if (state.previewDurationMs > 0L) {
                (state.previewPositionMs.toFloat() / state.previewDurationMs.toFloat())
                    .coerceIn(0f, 1f)
            } else {
                0f
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
                    Text(
                        text = stringResource(R.string.download_preview_now, state.previewTitle),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.download_preview_progress,
                                // 播放结束后 ExoPlayer 的 currentPosition 可能略超 duration，钳制避免 00:30 / 00:29。
                                Formatters.formatDuration(
                                    state.previewPositionMs.coerceAtMost(
                                        state.previewDurationMs.coerceAtLeast(0L)
                                    )
                                ),
                                Formatters.formatDuration(state.previewDurationMs)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onStopPreview) {
                            Text(text = stringResource(R.string.download_preview_stop))
                        }
                    }
                }
            }
        }
    }

    // 逐条下载进度（与直链模式共用同一个下载 Job）。
    if (state.downloadingId != null) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
                    Text(
                        text = stringResource(R.string.download_saving),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    if (state.downloadProgress >= 0) {
                        LinearProgressIndicator(
                            progress = { state.downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(R.string.link_download_progress, state.downloadProgress),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = stringResource(R.string.link_download_progress_unknown),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(R.string.download_cancel))
                    }
                }
            }
        }
    }

    state.savedName?.let { name ->
        item {
            ResultCard(
                text = stringResource(R.string.download_saved, name),
                onDismiss = onDismissSaved
            )
        }
    }

    // 空结果直接走空态卡片，不重复弹一张错误卡。
    val searchError = state.searchError
    if (searchError != null && searchError != DownloadError.SEARCH_EMPTY) {
        item {
            ErrorCard(
                message = stringResource(searchError.toMessageRes()),
                detail = state.searchErrorDetail?.let {
                    stringResource(R.string.download_error_detail, it)
                },
                onDismiss = onConsumeSearchError
            )
        }
    }

    if (state.results.isEmpty() && !state.isSearching) {
        item {
            if (state.hasSearched) {
                EmptyState(
                    title = stringResource(R.string.download_empty_title),
                    body = stringResource(R.string.download_empty_body)
                )
            } else {
                EmptyState(
                    title = stringResource(R.string.download_idle_title),
                    body = stringResource(R.string.download_idle_body)
                )
            }
        }
    } else if (state.results.isNotEmpty()) {
        item {
            Text(
                text = stringResource(R.string.download_results_count, state.results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(state.results, key = { it.id }) { song ->
            SongResultItem(
                song = song,
                isPreviewing = state.previewId == song.id,
                isPlaying = state.isPreviewPlaying,
                isDownloading = state.downloadingId != null,
                onPreview = { onPreviewClick(song) },
                onDownload = { onDownloadSong(song) }
            )
        }
        if (state.hasMore) {
            item {
                OutlinedButton(
                    onClick = onLoadMore,
                    enabled = !state.isLoadingMore && !state.isSearching,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.isLoadingMore) {
                            stringResource(R.string.download_loading_more)
                        } else {
                            stringResource(R.string.download_load_more)
                        }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.linkSection(
    state: DownloadUiState,
    onUrlChange: (String) -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onImport: () -> Unit,
    onReset: () -> Unit,
    onConsumeError: () -> Unit
) {
    item { InfoCard(text = stringResource(R.string.link_download_body)) }
    item {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ShiyinDimens.CardCorner),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = stringResource(R.string.link_download_notice),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
            )
        }
    }
    item {
        OutlinedTextField(
            value = state.url,
            onValueChange = onUrlChange,
            label = { Text(text = stringResource(R.string.link_download_label)) },
            placeholder = { Text(text = stringResource(R.string.link_download_hint)) },
            singleLine = true,
            enabled = !state.isDownloading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )
    }
    item {
        Button(
            onClick = onDownload,
            enabled = !state.isDownloading && state.url.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.link_download_action))
        }
    }

    if (state.isDownloading) {
        item {
            Column {
                if (state.progress >= 0) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = stringResource(R.string.link_download_progress, state.progress),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = stringResource(R.string.link_download_progress_unknown),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(R.string.link_download_cancel))
                }
            }
        }
    }

    when (val export = state.exportState) {
        ExportState.Idle, ExportState.Running -> Unit
        is ExportState.Success -> item {
            ResultCard(
                text = stringResource(R.string.link_download_success, export.message),
                onDismiss = onReset
            )
        }
        is ExportState.Failure -> item {
            Text(
                text = export.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }

    state.error?.let { error ->
        item {
            ErrorCard(
                message = stringResource(error.toMessageRes()),
                detail = state.errorDetail?.let {
                    stringResource(R.string.link_download_detail, it)
                },
                onDismiss = onConsumeError
            )
        }
    }

    item { InfoCard(text = stringResource(R.string.link_download_import_section)) }
    item {
        OutlinedButton(
            onClick = onImport,
            enabled = !state.isDownloading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.editor_pick_audio))
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShiyinDimens.CardCorner),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
        )
    }
}

@Composable
private fun ResultCard(text: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShiyinDimens.CardCorner),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(ProvisionalDimens.CardInnerPadding)
            )
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.editor_result_dismiss))
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String, detail: String?, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShiyinDimens.CardCorner),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            detail?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.editor_result_dismiss))
            }
        }
    }
}

private fun DownloadError.toMessageRes(): Int = when (this) {
    DownloadError.INVALID_URL -> R.string.link_error_invalid_url
    DownloadError.NO_NETWORK -> R.string.link_error_no_network
    DownloadError.REQUEST_FAILED -> R.string.link_error_request_failed
    DownloadError.HTTP_ERROR -> R.string.link_error_http
    DownloadError.UNSUPPORTED_MEDIA -> R.string.link_error_unsupported
    DownloadError.FILE_TOO_LARGE -> R.string.link_error_too_large
    DownloadError.EMPTY_FILE -> R.string.link_error_empty
    DownloadError.WRITE_FAILED -> R.string.link_error_write_failed
    DownloadError.IMPORT_FAILED -> R.string.link_error_import_failed
    DownloadError.EMPTY_QUERY -> R.string.download_error_empty_query
    DownloadError.SEARCH_FAILED -> R.string.download_error_search
    DownloadError.SEARCH_EMPTY -> R.string.download_error_empty
    DownloadError.PREVIEW_FAILED -> R.string.download_error_preview
}

private const val MODE_SEARCH = 0
private const val MIME_AUDIO = "audio/*"
