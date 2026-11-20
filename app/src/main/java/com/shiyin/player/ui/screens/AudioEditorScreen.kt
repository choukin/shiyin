package com.shiyin.player.ui.screens

import android.net.Uri
import android.view.TextureView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.shiyin.player.R
import com.shiyin.player.data.media.Mp3Encoder
import com.shiyin.player.data.model.AudioFormat
import com.shiyin.player.data.model.EditorTool
import com.shiyin.player.data.model.EditorUiState
import com.shiyin.player.data.model.ExtractMode
import com.shiyin.player.ui.components.CapsuleTabBar
import com.shiyin.player.ui.components.EditorWorkspace
import com.shiyin.player.ui.components.ShiyinScaffold

/**
 * 「音频编辑」统一入口：顶部 Tab 切换「音频裁剪」与「提取音频」，共用同一工作区。
 *
 * 两个工具此前是两屏两条路由，管线完全一致（probe → 波形 → clipping → Transformer），
 * 差异只在源文件类型、默认模式与导出时是否去掉视频轨，现已全部收拢到 [EditorTool]：
 * 本屏只负责按当前 tool 提供文案、MIME 与模式选择器开关，状态与重置由 EditorViewModel 承担。
 */
@Composable
fun AudioEditorScreen(
    state: EditorUiState,
    onToolChange: (EditorTool) -> Unit,
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
    val tool = state.tool
    val tabs = listOf(
        stringResource(R.string.audio_editor_tab_trim),
        stringResource(R.string.audio_editor_tab_extract)
    )
    val selectedTab = when (tool) {
        EditorTool.TRIM -> 0
        EditorTool.EXTRACT -> 1
    }
    val bodyRes = when (tool) {
        EditorTool.TRIM -> R.string.trim_body
        EditorTool.EXTRACT -> R.string.extract_body
    }
    val pickLabelRes = when (tool) {
        EditorTool.TRIM -> R.string.editor_pick_audio
        EditorTool.EXTRACT -> R.string.editor_pick_video
    }
    val mimeTypes = when (tool) {
        EditorTool.TRIM -> arrayOf(MIME_AUDIO)
        EditorTool.EXTRACT -> arrayOf(MIME_VIDEO)
    }

    // 单个 picker 即可：launch 时按当前工具传不同 MIME。
    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> onSourceSelected(uri) }

    ShiyinScaffold(
        title = stringResource(R.string.audio_editor),
        onBack = onBack,
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            CapsuleTabBar(
                tabs = tabs,
                selectedIndex = selectedTab,
                onSelected = { index ->
                    onToolChange(
                        when (index) {
                            0 -> EditorTool.TRIM
                            else -> EditorTool.EXTRACT
                        }
                    )
                }
            )
            EditorWorkspace(
                state = state,
                body = stringResource(bodyRes),
                pickLabel = stringResource(pickLabelRes),
                // 提取音频可切「整段 / 所选片段」；裁剪固定按选区，不显示模式选择器。
                showModeSelector = tool == EditorTool.EXTRACT,
                onPick = { picker.launch(mimeTypes) },
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
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private const val MIME_AUDIO = "audio/*"
private const val MIME_VIDEO = "video/*"
