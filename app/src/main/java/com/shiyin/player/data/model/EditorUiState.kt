package com.shiyin.player.data.model

import android.net.Uri
import com.shiyin.player.data.util.ClipRange

/** 通过 SAF 选中的本地媒体（音频或视频），不含任何网络来源。 */
data class MediaSource(
    val uri: Uri,
    val displayName: String,
    val durationMs: Long,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
    val sizeBytes: Long = 0L
)

/**
 * 导出格式。Android 平台与 Media3 Transformer 均无 MP3 编码器：
 * - M4A：走 Transformer（AAC），最快且兼容性最好；
 * - WAV：MediaCodec 解码 PCM 后写标准 WAV 头（无损、体积大）；
 * - MP3：MediaCodec 解码 PCM 后交给 LAME（libLame.so，仅 arm64-v8a）。
 */
enum class AudioFormat(
    val extension: String,
    val mime: String,
    val label: String
) {
    M4A("m4a", "audio/mp4", "M4A"),
    WAV("wav", "audio/wav", "WAV"),
    MP3("mp3", "audio/mpeg", "MP3");

    companion object {
        fun fromName(name: String): AudioFormat =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: M4A
    }
}

/** 导出/导入的统一结果态，替换一期的「导出成功（设计示意）」静态文案。 */
sealed interface ExportState {
    data object Idle : ExportState
    data object Running : ExportState
    data class Success(val message: String) : ExportState
    data class Failure(val message: String) : ExportState
}

/** 提取模式：整段提取 or 所选片段。 */
enum class ExtractMode { WHOLE, RANGE }

/**
 * 「音频编辑」统一入口下的两个工具，由页面顶部 Tab 切换。
 * 二者共用同一条编辑管线，仅 removeVideo（导出时是否去掉视频轨）与默认模式不同：
 * - [TRIM] 音频裁剪：处理音频源，固定按选区裁剪；
 * - [EXTRACT] 提取音频：处理视频源，默认整段、可选片段。
 */
enum class EditorTool(
    val removeVideo: Boolean,
    val defaultMode: ExtractMode
) {
    TRIM(removeVideo = false, defaultMode = ExtractMode.RANGE),
    EXTRACT(removeVideo = true, defaultMode = ExtractMode.WHOLE)
}

/**
 * 错误类型用枚举而非裸字符串：所有 UI 文案集中在 strings.xml，
 * ViewModel 不持有 Context、不硬编码中文。
 */
enum class EditorError { NO_SOURCE, NO_AUDIO, PROBE_FAILED, EXPORT_FAILED }

/**
 * 「音频编辑」统一入口的共用状态（音频裁剪 / 提取音频两个 Tab 复用）。
 * 两者管线相同（Media3 Transformer + ClippingConfiguration），
 * 仅 [tool] 携带的 removeVideo 与默认 mode 不同。
 */
data class EditorUiState(
    /** 当前激活的工具 Tab，决定 select 文件类型、默认模式与导出语义。 */
    val tool: EditorTool = EditorTool.TRIM,
    val source: MediaSource? = null,
    val waveform: List<Float> = emptyList(),
    val clipRange: ClipRange = ClipRange(0L, 0L),
    val mode: ExtractMode = ExtractMode.WHOLE,
    val exportState: ExportState = ExportState.Idle,
    val isProbing: Boolean = false,
    val error: EditorError? = null,
    // 导出设置：自定义文件名（不含扩展名）+ 目标格式。
    val exportFileName: String = "",
    val exportFormat: AudioFormat = AudioFormat.M4A,
    // 试听预览：播放态 + 当前位置（毫秒，相对媒体起点），仅用于 UI，不参与导出。
    val isPreviewPlaying: Boolean = false,
    val previewPositionMs: Long = 0L
)
