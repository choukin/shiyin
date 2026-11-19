package com.shiyin.player.data.model

import android.net.Uri
import com.shiyin.player.data.util.ClipRange

/** 通过 SAF 选中的本地媒体（音频或视频），不含任何网络来源。 */
data class MediaSource(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val hasVideo: Boolean,
    val hasAudio: Boolean
)

/** 导出/导入的统一结果态，替换一期的「导出成功（设计示意）」静态文案。 */
sealed interface ExportState {
    data object Idle : ExportState
    data object Running : ExportState
    data class Success(val message: String) : ExportState
    data class Failure(val message: String) : ExportState
}

/** 06 屏：整段提取 or 所选片段。 */
enum class ExtractMode { WHOLE, RANGE }

/**
 * 错误类型用枚举而非裸字符串：所有 UI 文案集中在 strings.xml，
 * ViewModel 不持有 Context、不硬编码中文。
 */
enum class EditorError { NO_SOURCE, NO_AUDIO, PROBE_FAILED, EXPORT_FAILED }

/**
 * 05 音乐裁剪 / 06 视频提取音频共用状态。
 * 两者管线相同（Media3 Transformer + ClippingConfiguration），仅 removeVideo 与 mode 不同。
 */
data class EditorUiState(
    val source: MediaSource? = null,
    val waveform: List<Float> = emptyList(),
    val clipRange: ClipRange = ClipRange(0L, 0L),
    val mode: ExtractMode = ExtractMode.WHOLE,
    val exportState: ExportState = ExportState.Idle,
    val isProbing: Boolean = false,
    val error: EditorError? = null
)

/** 04 导入本地文件（版权合规替代方案）状态。 */
data class ImportUiState(
    val fileName: String? = null,
    val exportState: ExportState = ExportState.Idle,
    val error: EditorError? = null
)
