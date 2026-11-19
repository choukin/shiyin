package com.shiyin.player.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyin.player.data.media.MediaEditRepository
import com.shiyin.player.data.media.WaveformExtractor
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
 */
class EditorViewModel(
    private val repository: MediaEditRepository,
    private val waveformExtractor: WaveformExtractor,
    private val removeVideo: Boolean,
    defaultMode: ExtractMode = ExtractMode.RANGE
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState(mode = defaultMode))
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    /** SAF 选中文件后：读元信息 + 解码真实 PCM 波形。 */
    fun onSourceSelected(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    source = null,
                    waveform = emptyList(),
                    isProbing = true,
                    error = null,
                    exportState = ExportState.Idle
                )
            }
            runCatching { repository.probe(uri) }
                .onSuccess { source ->
                    val bars = waveformExtractor.extract(uri)
                    _uiState.update {
                        it.copy(
                            source = source,
                            waveform = bars,
                            clipRange = ClipRange.full(source.durationMs),
                            isProbing = false,
                            error = if (source.hasAudio) null else EditorError.NO_AUDIO
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
        _uiState.update { state ->
            val duration = state.source?.durationMs ?: 0L
            state.copy(
                clipRange = ClipRange(startMs, endMs).clamp(duration),
                exportState = ExportState.Idle
            )
        }
    }

    fun onModeChange(mode: ExtractMode) {
        _uiState.update { it.copy(mode = mode, exportState = ExportState.Idle) }
    }

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
                baseName = source.displayName
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

    fun resetExportState() {
        _uiState.update { it.copy(exportState = ExportState.Idle) }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }
}
