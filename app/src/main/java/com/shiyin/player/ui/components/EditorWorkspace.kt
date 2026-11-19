package com.shiyin.player.ui.components

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
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shiyin.player.R
import com.shiyin.player.data.model.EditorError
import com.shiyin.player.data.model.EditorUiState
import com.shiyin.player.data.model.ExportState
import com.shiyin.player.data.model.ExtractMode
import com.shiyin.player.data.util.Formatters
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinDimens

/**
 * 05 音乐裁剪 / 06 视频提取音频的共用工作区。
 *
 * 刻意不含任何广告参数或 AdBannerPlaceholder（docs/AD_SPEC.md：编辑类页面零广告），
 * 也不含网络相关入口。所有尺寸取自 ProvisionalDimens / ShiyinDimens，屏幕层零魔数。
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
    onExport: () -> Unit,
    onResetExport: () -> Unit,
    onConsumeError: () -> Unit,
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
                selectionEndRatio = selectionEnd
            )
        } else {
            WaveformPlaceholder()
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

private fun EditorError.toMessageRes(): Int = when (this) {
    EditorError.NO_SOURCE -> R.string.editor_error_no_source
    EditorError.NO_AUDIO -> R.string.editor_error_no_audio
    EditorError.PROBE_FAILED -> R.string.editor_error_probe_failed
    EditorError.EXPORT_FAILED -> R.string.editor_error_export_failed
}
