package com.shiyin.player.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyin.player.data.media.DownloadResult
import com.shiyin.player.data.media.LinkDownloadRepository
import com.shiyin.player.data.media.MediaEditRepository
import com.shiyin.player.data.model.DownloadError
import com.shiyin.player.data.model.DownloadUiState
import com.shiyin.player.data.model.ExportState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 04 链接下载：主入口为直链 GET 下载入库，次要入口保留 SAF 本地导入。
 * 不持有 Context，错误只用 [DownloadError] 枚举 + 技术 detail 表达，文案全部在 strings.xml。
 */
class LinkDownloadViewModel(
    private val downloadRepository: LinkDownloadRepository,
    private val mediaEditRepository: MediaEditRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    fun onUrlChange(value: String) {
        _uiState.update { it.copy(url = value, error = null, errorDetail = null) }
    }

    fun download() {
        if (_uiState.value.isDownloading) return
        val url = _uiState.value.url
        _uiState.update {
            it.copy(
                isDownloading = true,
                progress = -1,
                exportState = ExportState.Idle,
                error = null,
                errorDetail = null
            )
        }
        downloadJob = viewModelScope.launch {
            val result = downloadRepository.download(url) { progress ->
                _uiState.update { it.copy(progress = progress) }
            }
            _uiState.update { current ->
                when (result) {
                    is DownloadResult.Success -> current.copy(
                        isDownloading = false,
                        progress = 100,
                        exportState = ExportState.Success(result.displayName)
                    )
                    is DownloadResult.Failure -> current.copy(
                        isDownloading = false,
                        progress = -1,
                        error = result.error,
                        errorDetail = result.detail
                    )
                }
            }
        }
    }

    fun cancel() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update { it.copy(isDownloading = false, progress = -1) }
    }

    /** 次要入口：把 SAF 选中的本机音频原样导入音乐库。 */
    fun import(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(exportState = ExportState.Idle, error = null, errorDetail = null)
            }
            mediaEditRepository.importAudio(uri)
                .onSuccess { name ->
                    _uiState.update { it.copy(exportState = ExportState.Success(name)) }
                }
                .onFailure {
                    _uiState.update { it.copy(error = DownloadError.IMPORT_FAILED) }
                }
        }
    }

    fun reset() {
        _uiState.update {
            it.copy(
                exportState = ExportState.Idle,
                error = null,
                errorDetail = null,
                progress = -1
            )
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null, errorDetail = null) }
    }

    override fun onCleared() {
        downloadJob?.cancel()
        super.onCleared()
    }
}
