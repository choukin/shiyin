package com.shiyin.player.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyin.player.data.model.PermissionState
import com.shiyin.player.data.model.ScanUiState
import com.shiyin.player.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanViewModel(private val repository: LibraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun onPermissionStateChanged(permission: PermissionState) {
        _uiState.update { it.copy(permission = permission) }
    }

    /** granted=false 且永久拒绝 → 不重复弹窗，改引导去系统设置。 */
    fun onPermissionResult(granted: Boolean, permanentlyDenied: Boolean) {
        val permission = when {
            granted -> PermissionState.Granted
            permanentlyDenied -> PermissionState.PermanentlyDenied
            else -> PermissionState.Denied
        }
        _uiState.update { it.copy(permission = permission) }
        if (granted) scan()
    }

    fun scan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, error = null, finished = false) }
            runCatching { repository.loadTracks() }
                .onSuccess { tracks ->
                    _uiState.update {
                        it.copy(isScanning = false, foundCount = tracks.size, finished = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isScanning = false, error = error.message) }
                }
        }
    }

    fun consumeFinished() {
        _uiState.update { it.copy(finished = false) }
    }
}
