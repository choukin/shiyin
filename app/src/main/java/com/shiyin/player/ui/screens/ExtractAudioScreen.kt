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
import com.shiyin.player.data.model.ExtractMode
import com.shiyin.player.ui.components.EditorWorkspace
import com.shiyin.player.ui.components.ShiyinScaffold

/**
 * 06 视频提取音频（二期真实实现）：SAF 选本地视频 → 整段 / 所选片段 →
 * Media3 EditedMediaItem.setRemoveVideo(true) → 写入音乐库。
 * 一期的 DisabledOption 置灰示意已替换为可点击的整段/片段模式。
 * 纯本地处理，零网络。
 */
@Composable
fun ExtractAudioScreen(
    state: EditorUiState,
    onSourceSelected: (Uri?) -> Unit,
    onRangeChange: (Long, Long) -> Unit,
    onModeChange: (ExtractMode) -> Unit,
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
        title = stringResource(R.string.extract_audio),
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        EditorWorkspace(
            state = state,
            body = stringResource(R.string.extract_body),
            pickLabel = stringResource(R.string.editor_pick_video),
            showModeSelector = true,
            onPick = { picker.launch(arrayOf(MIME_VIDEO)) },
            onRangeChange = onRangeChange,
            onModeChange = onModeChange,
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

private const val MIME_VIDEO = "video/*"
