package com.shiyin.player.ui.screens

import androidx.lifecycle.ViewModel
import com.shiyin.player.data.model.PlayerUiState
import com.shiyin.player.data.model.Track
import com.shiyin.player.playback.PlayerController
import kotlinx.coroutines.flow.StateFlow

class PlayerViewModel(private val controller: PlayerController) : ViewModel() {

    val uiState: StateFlow<PlayerUiState> = controller.state

    init {
        controller.connect()
    }

    fun setQueue(tracks: List<Track>, startIndex: Int = 0) =
        controller.setQueue(tracks, startIndex)

    fun togglePlayPause() {
        if (uiState.value.isPlaying) controller.pause() else controller.play()
    }

    fun play() = controller.play()
    fun pause() = controller.pause()
    fun next() = controller.next()
    fun previous() = controller.previous()
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    // 不在此 release()：切出播放器再进入时应复用同一条 MediaController 连接。
}
