package com.shiyin.player.ui.screens

import android.net.Uri
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shiyin.player.R
import com.shiyin.player.data.media.Mp3Encoder
import com.shiyin.player.data.model.AudioFormat
import com.shiyin.player.data.model.EditorUiState
import com.shiyin.player.ui.components.EditorWorkspace
import com.shiyin.player.ui.components.ShiyinScaffold

/**
 * 05 音乐裁剪（二期真实实现）：SAF 选本地音频 → 真实 PCM 波形 →
 * RangeSlider 选区 → Media3 Transformer clipping → 写入音乐库。
 * 纯本地处理，零网络。
 */
@Composable
fun MusicTrimScreen(
    state: EditorUiState,
    onSourceSelected: (Uri?) -> Unit,
    onRangeChange: (Long, Long) -> Unit,
    onExportFileNameChange: (String) -> Unit,
    onExportFormatChange: (AudioFormat) -> Unit,
    onExport: () -> Unit,
    onResetExport: () -> Unit,
    onConsumeError: () -> Unit,
    onTogglePreview: () -> Unit,
    onStopPreview: () -> Unit,
    onAttachVideo: (TextureView) -> Unit,
    onDetachVideo: (TextureView) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> onSourceSelected(uri) }

    ShiyinScaffold(
        title = stringResource(R.string.music_trim),
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        EditorWorkspace(
            state = state,
            body = stringResource(R.string.trim_body),
            pickLabel = stringResource(R.string.editor_pick_audio),
            showModeSelector = false,
            onPick = { picker.launch(arrayOf(MIME_AUDIO)) },
            onRangeChange = onRangeChange,
            onModeChange = {},
            onExportFileNameChange = onExportFileNameChange,
            onExportFormatChange = onExportFormatChange,
            mp3Available = Mp3Encoder.isAvailable,
            onExport = onExport,
            onResetExport = onResetExport,
            onConsumeError = onConsumeError,
            onTogglePreview = onTogglePreview,
            onStopPreview = onStopPreview,
            onAttachVideo = onAttachVideo,
            onDetachVideo = onDetachVideo,
            modifier = Modifier.padding(padding)
        )
    }
}

private const val MIME_AUDIO = "audio/*"
