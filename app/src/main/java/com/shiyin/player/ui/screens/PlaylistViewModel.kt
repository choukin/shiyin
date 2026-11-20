package com.shiyin.player.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyin.player.data.model.Playlist
import com.shiyin.player.data.model.Track
import com.shiyin.player.data.repository.LibraryRepository
import com.shiyin.player.data.repository.PlaylistRepository
import com.shiyin.player.playback.PlayerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val allTracks: List<Track> = emptyList(),
    val selectedId: String? = null,
    val showCreateDialog: Boolean = false
) {
    val selected: Playlist? get() = playlists.firstOrNull { it.id == selectedId }
}

/** 歌单 ViewModel：歌单 CRUD + 加歌 + 播放整单。 */
class PlaylistViewModel(
    private val playlistRepository: PlaylistRepository,
    private val libraryRepository: LibraryRepository,
    private val playerController: PlayerController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playlistRepository.playlists.collect { list ->
                _uiState.update { it.copy(playlists = list) }
            }
        }
        viewModelScope.launch {
            runCatching { libraryRepository.loadTracks() }
                .onSuccess { tracks -> _uiState.update { it.copy(allTracks = tracks) } }
        }
    }

    fun showCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show) }
    }

    fun create(name: String) {
        viewModelScope.launch {
            val created = playlistRepository.create(name)
            _uiState.update { it.copy(showCreateDialog = false, selectedId = created.id) }
        }
    }

    fun select(id: String?) {
        _uiState.update { it.copy(selectedId = id) }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            playlistRepository.delete(id)
            if (_uiState.value.selectedId == id) _uiState.update { it.copy(selectedId = null) }
        }
    }

    fun addTracks(id: String, trackIds: List<Long>) {
        if (trackIds.isEmpty()) return
        viewModelScope.launch { playlistRepository.addTracks(id, trackIds) }
    }

    fun removeTrack(id: String, trackId: Long) {
        viewModelScope.launch { playlistRepository.removeTrack(id, trackId) }
    }

    /** 播放整张歌单：按 trackIds 顺序从曲库取出真实 Track，设置播放队列。 */
    fun play(playlist: Playlist) {
        val byId = _uiState.value.allTracks.associateBy { it.id }
        val queue = playlist.trackIds.mapNotNull { byId[it] }
        if (queue.isNotEmpty()) playerController.setQueue(queue, 0)
    }
}
