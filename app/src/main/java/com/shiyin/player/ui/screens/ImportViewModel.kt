package com.shiyin.player.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyin.player.data.media.MediaEditRepository
import com.shiyin.player.data.model.EditorError
import com.shiyin.player.data.model.ExportState
import com.shiyin.player.data.model.ImportUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 04 屏「导入本地文件」：版权合规替代方案。
 * 只把用户通过 SAF 选中的本机音频原样复制进音乐库，不解析链接、不联网、不下载。
 */
class ImportViewModel(private val repository: MediaEditRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun import(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(exportState = ExportState.Running, error = null, fileName = null)
            }
            repository.importAudio(uri)
                .onSuccess { fileName ->
                    _uiState.update {
                        it.copy(exportState = ExportState.Success(fileName), fileName = fileName)
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(exportState = ExportState.Idle, error = EditorError.EXPORT_FAILED)
                    }
                }
        }
    }

    fun reset() {
        _uiState.update { ImportUiState() }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }
}
