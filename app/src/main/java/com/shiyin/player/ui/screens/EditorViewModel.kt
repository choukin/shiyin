package com.shiyin.player.ui.screens

import android.net.Uri
import android.view.TextureView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyin.player.data.media.ClipPreviewPlayer
import com.shiyin.player.data.media.MediaEditRepository
import com.shiyin.player.data.media.WaveformExtractor
import com.shiyin.player.data.model.AudioFormat
import com.shiyin.player.data.model.EditorError
import com.shiyin.player.data.model.EditorUiState
import com.shiyin.player.data.model.ExportState
import com.shiyin.player.data.model.ExtractMode
import com.shiyin.player.data.util.ClipRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 05 音乐裁剪 / 06 视频提取音频共用 ViewModel。
 * 两者只差 removeVideo 与默认模式，管线（probe → 波形 → clipping → Transformer）完全一致。
 *
 * 试听预览由独立的 [ClipPreviewPlayer] 承担，与后台曲库播放会话隔离；
 * ViewModel 负责把播放器回调映射进 EditorUiState，并在生命周期结束时释放。
 */
class EditorViewModel(
    private val repository: MediaEditRepository,
    private val waveformExtractor: WaveformExtractor,
    private val previewPlayer: ClipPreviewPlayer,
    private val removeVideo: Boolean,
    defaultMode: ExtractMode = ExtractMode.RANGE
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(mode = defaultMode))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        previewPlayer.onPlayingChanged = { playing ->
            _uiState.update { it.copy(isPreviewPlaying = playing) }
        }
        previewPlayer.onProgress = { positionMs ->
            _uiState.update { it.copy(previewPositionMs = positionMs) }
        }
    }

    /** SAF 选中文件后：读元信息 + 解码真实 PCM 波形 + 重建试听播放器。 */
    fun onSourceSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    source = null,
                    waveform = emptyList(),
                    isProbing = true,
                    error = null,
                    exportState = ExportState.Idle,
                    isPreviewPlaying = false,
                    previewPositionMs = 0L
                )
            }
            previewPlayer.stop()
            runCatching { repository.probe(uri) }
                .onSuccess { source ->
                    val bars = waveformExtractor.extract(uri)
                    _uiState.update {
                        it.copy(
                            source = source,
                            waveform = bars,
                            clipRange = ClipRange.full(source.durationMs),
                            isProbing = false,
                            error = if (source.hasAudio) null else EditorError.NO_AUDIO,
                            // 默认文件名 = 源文件 stem，用户可在导出设置里改。
                            exportFileName = source.displayName.substringBeforeLast('.')
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(isProbing = false, error = EditorError.PROBE_FAILED)
                    }
                }
        }
    }

    /** RangeSlider 回调；钳制逻辑集中在 ClipRange.clamp，UI 不自己算边界。 */
    fun onRangeChange(startMs: Long, endMs: Long) {
        val duration = _uiState.value.source?.durationMs ?: 0L
        val clamped = ClipRange(startMs, endMs).clamp(duration)
        _uiState.update { state ->
            state.copy(
                clipRange = clamped,
                exportState = ExportState.Idle
            )
        }
        // 拖动滑块时同步重建试听区间；正在播放则续播（updateRange 内部判断）。
        if (_uiState.value.source != null && clamped.lengthMs > 0L) {
            previewPlayer.updateRange(clamped)
        }
    }

    fun onModeChange(mode: ExtractMode) {
        _uiState.update { it.copy(mode = mode, exportState = ExportState.Idle) }
    }

    fun onExportFileNameChange(name: String) {
        _uiState.update { it.copy(exportFileName = name, exportState = ExportState.Idle) }
    }

    fun onExportFormatChange(format: AudioFormat) {
        _uiState.update { it.copy(exportFormat = format, exportState = ExportState.Idle) }
    }

    /** 播放/暂停试听。播放范围 = 当前选区（RANGE）或整段（WHOLE）。 */
    fun togglePreview() {
        val current = _uiState.value
        val source = current.source ?: return
        if (!source.hasAudio) return
        when {
            current.isPreviewPlaying -> previewPlayer.pause()
            current.previewPositionMs > 0L && current.previewPositionMs >= current.clipRange.startMs ->
                previewPlayer.play()
            else -> previewPlayer.prepare(
                uri = source.uri,
                range = previewRange(current),
                hasVideo = source.hasVideo,
                autoPlay = true
            )
        }
    }

    /** 停止试听并复位进度。 */
    fun stopPreview() {
        previewPlayer.stop()
        _uiState.update { it.copy(isPreviewPlaying = false, previewPositionMs = 0L) }
    }

    /** 视频画面 attach/detach，由 Compose 的 AndroidView 生命周期调用。 */
    fun attachVideoView(view: TextureView) = previewPlayer.attachVideoView(view)
    fun detachVideoView(view: TextureView) = previewPlayer.detachVideoView(view)

    fun export() {
        val current = _uiState.value
        val source = current.source
        if (source == null) {
            _uiState.update { it.copy(error = EditorError.NO_SOURCE) }
            return
        }
        if (!source.hasAudio) {
            _uiState.update { it.copy(error = EditorError.NO_AUDIO) }
            return
        }
        // 导出时停掉试听，避免两条音频流同时出声。
        previewPlayer.stop()
        _uiState.update { it.copy(isPreviewPlaying = false, previewPositionMs = 0L) }
        // WHOLE 模式传空区间：Transformer 不设 clipping，按整段导出。
        val range = if (current.mode == ExtractMode.WHOLE) {
            ClipRange(0L, 0L)
        } else {
            current.clipRange.clamp(source.durationMs)
        }
        viewModelScope.launch {
            _uiState.update { it.copy(exportState = ExportState.Running, error = null) }
            repository.exportAudio(
                sourceUri = source.uri,
                clipRange = range,
                removeVideo = removeVideo,
                baseName = source.displayName,
                customName = current.exportFileName,
                format = current.exportFormat
            ).onSuccess { fileName ->
                _uiState.update {
                    it.copy(exportState = ExportState.Success(fileName))
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        exportState = ExportState.Idle,
                        error = EditorError.EXPORT_FAILED
                    )
                }
            }
        }
    }

    /** 试听/导出共用同一区间语义：WHOLE=整段，RANGE=所选片段。 */
    private fun previewRange(state: EditorUiState): ClipRange {
        val source = state.source ?: return ClipRange(0L, 0L)
        return if (state.mode == ExtractMode.WHOLE) {
            ClipRange.full(source.durationMs)
        } else {
            state.clipRange.clamp(source.durationMs)
        }
    }

    fun resetExportState() {
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        previewPlayer.release()
        super.onCleared()
    }
}
