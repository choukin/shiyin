package com.shiyin.player.ui.components

import android.view.TextureView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.shiyin.player.R
import com.shiyin.player.data.model.AudioFormat
import com.shiyin.player.data.model.EditorError
import com.shiyin.player.data.model.EditorUiState
import com.shiyin.player.data.model.ExportState
import com.shiyin.player.data.model.ExtractMode
import com.shiyin.player.data.util.ClipRange
import com.shiyin.player.data.util.Formatters
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 「音频编辑」统一入口（音频裁剪 / 提取音频两个 Tab）的共用工作区。
 *
 * 纯本地处理，不含网络相关入口。
 * 所有尺寸取自 ProvisionalDimens / ShiyinDimens，屏幕层零魔数。
 */
@Composable
fun EditorWorkspace(
    state: EditorUiState,
    body: String,
    pickLabel: String,
    showModeSelector: Boolean,
    onPick: () -> Unit,
    onRangeChange: (Long, Long) -> Unit,
    onModeChange: (ExtractMode) -> Unit,
    onExportFileNameChange: (String) -> Unit,
    onExportFormatChange: (AudioFormat) -> Unit,
    mp3Available: Boolean,
    onExport: () -> Unit,
    onResetExport: () -> Unit,
    onConsumeError: () -> Unit,
    onTogglePreview: () -> Unit,
    onStopPreview: () -> Unit,
    onAttachVideo: (TextureView) -> Unit,
    onDetachVideo: (TextureView) -> Unit,
    modifier: Modifier = Modifier
) {
    val source = state.source
    val durationMs = source?.durationMs ?: 0L
    val hasDuration = durationMs > 0L
    val useRange = state.mode == ExtractMode.RANGE || !showModeSelector
    val selectionStart = if (state.mode == ExtractMode.WHOLE && showModeSelector) {
        0f
    } else {
        state.clipRange.startMs.toFloat() / durationMs.coerceAtLeast(1L)
    }
    val selectionEnd = if (state.mode == ExtractMode.WHOLE && showModeSelector) {
        1f
    } else {
        state.clipRange.endMs.toFloat() / durationMs.coerceAtLeast(1L)
    }
    val canExport = source?.hasAudio == true &&
        state.exportState !is ExportState.Running &&
        !state.isProbing
    val playbackRatio = if (hasDuration && state.previewPositionMs in 0L..durationMs) {
        state.previewPositionMs.toFloat() / durationMs
    } else {
        -1f
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = ProvisionalDimens.ScreenHorizontalPadding,
                vertical = ProvisionalDimens.ContentVerticalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ShiyinDimens.CardCorner),
            color = MaterialTheme.colorScheme.surface
        ) {
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)
            )
        }

        Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Text(text = pickLabel)
        }

        if (state.isProbing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (source != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
                    Text(
                        text = stringResource(R.string.editor_source_label, source.displayName),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    Text(
                        text = stringResource(
                            R.string.editor_duration,
                            if (hasDuration) {
                                Formatters.formatDuration(durationMs)
                            } else {
                                stringResource(R.string.editor_duration_unknown)
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing / 2))
                    Text(
                        text = stringResource(
                            R.string.editor_file_size,
                            Formatters.formatFileSize(source.sizeBytes)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.editor_no_source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.waveform.isNotEmpty()) {
            WaveformView(
                amplitudes = state.waveform,
                selectionStartRatio = selectionStart,
                selectionEndRatio = selectionEnd,
                playbackPositionRatio = playbackRatio
            )
        } else {
            WaveformPlaceholder()
        }

        // 试听预览：视频源（hasVideo）额外挂 TextureView 显示画面；纯音频源只出声。
        if (source != null && source.hasAudio) {
            PreviewControls(
                isPlaying = state.isPreviewPlaying,
                hasVideo = source.hasVideo,
                positionMs = state.previewPositionMs,
                range = if (useRange) state.clipRange else ClipRange(0L, durationMs),
                onToggle = onTogglePreview,
                onStop = onStopPreview,
                onAttachVideo = onAttachVideo,
                onDetachVideo = onDetachVideo
            )
        }

        if (showModeSelector) {
            Row(horizontalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)) {
                FilterChip(
                    selected = state.mode == ExtractMode.WHOLE,
                    onClick = { onModeChange(ExtractMode.WHOLE) },
                    label = { Text(stringResource(R.string.editor_mode_whole)) }
                )
                FilterChip(
                    selected = state.mode == ExtractMode.RANGE,
                    onClick = { onModeChange(ExtractMode.RANGE) },
                    label = { Text(stringResource(R.string.editor_mode_range)) }
                )
            }
        }

        if (source != null && hasDuration && useRange) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
                    Text(
                        text = stringResource(R.string.editor_range_label),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(R.string.editor_range_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    RangeSlider(
                        value = state.clipRange.startMs.toFloat()..state.clipRange.endMs.toFloat(),
                        onValueChange = { range ->
                            onRangeChange(range.start.toLong(), range.endInclusive.toLong())
                        },
                        valueRange = 0f..durationMs.toFloat()
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(
                                R.string.editor_start,
                                Formatters.formatDuration(state.clipRange.startMs)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = stringResource(
                                R.string.editor_end,
                                Formatters.formatDuration(state.clipRange.endMs)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        } else if (source != null && !hasDuration) {
            Text(
                text = stringResource(R.string.editor_no_duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (source != null && source.hasAudio) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
                    Text(
                        text = stringResource(R.string.editor_export_settings),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    OutlinedTextField(
                        value = state.exportFileName,
                        onValueChange = onExportFileNameChange,
                        label = { Text(stringResource(R.string.editor_file_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    Text(
                        text = stringResource(R.string.editor_format_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing / 2))
                    Row(horizontalArrangement = Arrangement.spacedBy(ProvisionalDimens.ItemVerticalSpacing)) {
                        ExportFormatChip(
                            format = AudioFormat.M4A,
                            selected = state.exportFormat == AudioFormat.M4A,
                            onClick = { onExportFormatChange(AudioFormat.M4A) }
                        )
                        ExportFormatChip(
                            format = AudioFormat.WAV,
                            selected = state.exportFormat == AudioFormat.WAV,
                            onClick = { onExportFormatChange(AudioFormat.WAV) }
                        )
                        ExportFormatChip(
                            format = AudioFormat.MP3,
                            selected = state.exportFormat == AudioFormat.MP3,
                            enabled = mp3Available,
                            onClick = { onExportFormatChange(AudioFormat.MP3) }
                        )
                    }
                    if (!mp3Available) {
                        Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing / 2))
                        Text(
                            text = stringResource(R.string.editor_mp3_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Button(
            onClick = onExport,
            enabled = canExport,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.editor_export))
        }

        when (val export = state.exportState) {
            ExportState.Idle -> Unit
            ExportState.Running -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(ProvisionalDimens.IconSizeMedium))
                Spacer(modifier = Modifier.width(ProvisionalDimens.ItemVerticalSpacing))
                Text(
                    text = stringResource(R.string.editor_exporting),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            is ExportState.Success -> Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.editor_export_success, export.message),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(ProvisionalDimens.CardInnerPadding)
                    )
                    TextButton(onClick = onResetExport) {
                        Text(text = stringResource(R.string.editor_result_dismiss))
                    }
                }
            }
            is ExportState.Failure -> Text(
                text = export.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        state.error?.let { error ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(ShiyinDimens.CardCorner),
                color = MaterialTheme.colorScheme.errorContainer
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(error.toMessageRes()),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(ProvisionalDimens.CardInnerPadding)
                    )
                    TextButton(onClick = onConsumeError) {
                        Text(text = stringResource(R.string.editor_result_dismiss))
                    }
                }
            }
        }
    }
}

/**
 * 试听控制条 + 视频画面。
 * - 播放/停止按钮复用自绘 Glyph.Play / Glyph.Pause / Glyph.Close；
 * - 视频源显示 TextureView，用 AndroidView 生命周期做 attach/detach；
 * - 进度文字显示「当前 / 选区终点」，让试听位置与裁剪区间一一对应。
 */
@Composable
private fun PreviewControls(
    isPlaying: Boolean,
    hasVideo: Boolean,
    positionMs: Long,
    range: ClipRange,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onAttachVideo: (TextureView) -> Unit,
    onDetachVideo: (TextureView) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ShiyinDimens.CardCorner),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(ProvisionalDimens.CardInnerPadding)) {
            Text(
                text = stringResource(R.string.editor_preview_label),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(R.string.editor_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))

            if (hasVideo) {
                AndroidView(
                    factory = { ctx -> TextureView(ctx) },
                    update = { view -> onAttachVideo(view) },
                    onRelease = { view -> onDetachVideo(view) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PREVIEW_VIDEO_HEIGHT)
                )
                Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle) {
                    ShiyinGlyph(
                        glyph = if (isPlaying) Glyph.Pause else Glyph.Play,
                        modifier = Modifier.size(ProvisionalDimens.PlayControlIconSize)
                    )
                }
                IconButton(onClick = onStop) {
                    ShiyinGlyph(
                        glyph = Glyph.Close,
                        modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                    )
                }
                Text(
                    text = stringResource(
                        R.string.editor_preview_progress,
                        Formatters.formatDuration(positionMs),
                        Formatters.formatDuration(range.lengthMs)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private val PREVIEW_VIDEO_HEIGHT = 180.dp

@Composable
private fun ExportFormatChip(
    format: AudioFormat,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = { Text(format.label) }
    )
}

private fun EditorError.toMessageRes(): Int = when (this) {
    EditorError.NO_SOURCE -> R.string.editor_error_no_source
    EditorError.NO_AUDIO -> R.string.editor_error_no_audio
    EditorError.PROBE_FAILED -> R.string.editor_error_probe_failed
    EditorError.EXPORT_FAILED -> R.string.editor_error_export_failed
}
