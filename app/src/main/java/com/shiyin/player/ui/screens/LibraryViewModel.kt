package com.shiyin.player.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyin.player.data.model.LibraryGroupDetail
import com.shiyin.player.data.model.LibraryGroupKind
import com.shiyin.player.data.model.LibraryUiState
import com.shiyin.player.data.model.PermissionState
import com.shiyin.player.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeFavorites().collect { favorites ->
                _uiState.update { it.copy(favorites = favorites) }
            }
        }
    }

    fun onPermissionStateChanged(permission: PermissionState) {
        _uiState.update { it.copy(permission = permission) }
        if (permission == PermissionState.Granted) refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { repository.loadTracks() }
                .onSuccess { tracks -> _uiState.update { it.copy(isLoading = false, tracks = tracks) } }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }
    }

    /** 切 Tab 时退出二级详情，避免返回后落在旧分组里。 */
    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index, detail = null) }
    }

    /** 打开专辑 / 歌手二级页。key 为规范化后的分组值，空串代表未知。 */
    fun openGroup(kind: LibraryGroupKind, key: String) {
        _uiState.update { it.copy(detail = LibraryGroupDetail(kind, key)) }
    }

    fun closeGroup() {
        _uiState.update { it.copy(detail = null) }
    }

    fun toggleFavorite(trackId: String) {
        viewModelScope.launch { repository.toggleFavorite(trackId) }
    }
}
