package com.shiyin.player.playback

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.shiyin.player.data.model.PlayerUiState
import com.shiyin.player.data.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * UI 侧播放控制器：连接 PlaybackService 的 MediaSession，把 MediaStore 的
 * content:// URI 装配成 MediaItem 队列，并把播放状态发布成 StateFlow。
 *
 * 零新增依赖的关键点：buildAsync() 返回 ListenableFuture，这里用
 * addListener(Runnable, mainExecutor) 而非 .await()（后者需要 kotlinx-coroutines-guava）。
 * 释放用 MediaController.release()，不调用 releaseFuture，源码里无需 import Guava 类型。
 */
class PlayerController(private val context: Context) {

    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private val mainHandler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private var playerListener: Player.Listener? = null
    private var pendingQueue: List<Track> = emptyList()
    private var pendingIndex = 0

    @Volatile
    private var released = false

    private val progressTicker = object : Runnable {
        override fun run() {
            val player = controller ?: return
            _state.update {
                it.copy(
                    isPlaying = player.isPlaying,
                    positionMs = player.currentPosition.coerceAtLeast(0L),
                    durationMs = player.duration.takeIf { d -> d > 0 } ?: 0L
                )
            }
            mainHandler.postDelayed(this, 500L)
        }
    }

    /** 懒连接：首次进入播放器 / 首次设置队列时才调用，Application 不主动连服务。 */
    fun connect() {
        // 允许 release() 之后重新连接（再次进入播放器时）；不检查 released 标志
        if (controller != null) return
        released = false
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener(
            Runnable {
                val mediaController = runCatching { future.get() }.getOrNull()
                if (released) {
                    mediaController?.release()
                } else if (mediaController != null) {
                    attach(mediaController)
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    private fun attach(mediaController: MediaController) {
        controller = mediaController
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _state.update { it.copy(durationMs = mediaController.duration.coerceAtLeast(0L)) }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val queue = _state.value.queue
                _state.update {
                    it.copy(currentTrack = queue.getOrNull(mediaController.currentMediaItemIndex))
                }
            }
        }
        mediaController.addListener(listener)
        playerListener = listener
        _state.update {
            it.copy(
                isConnected = true,
                isPlaying = mediaController.isPlaying,
                durationMs = mediaController.duration.coerceAtLeast(0L)
            )
        }
        mainHandler.post(progressTicker)

        if (pendingQueue.isNotEmpty()) {
            setQueue(pendingQueue, pendingIndex)
            pendingQueue = emptyList()
        }
    }

    /** 把整份曲库（或选中列表）设为播放队列并从 startIndex 开始播放。 */
    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        _state.update {
            it.copy(queue = tracks, currentTrack = tracks.getOrNull(startIndex))
        }
        val player = controller
        if (player == null) {
            pendingQueue = tracks
            pendingIndex = startIndex
            connect()
            return
        }
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setUri(track.uri)
                .setMediaId(track.id.toString())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.title)
                        .setArtist(track.artist)
                        .setAlbumTitle(track.album)
                        .build()
                )
                .build()
        }
        player.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), C.TIME_UNSET)
        player.prepare()
        player.play()
    }

    fun play() = controller?.play()
    fun pause() = controller?.pause()
    fun next() = controller?.seekToNextMediaItem()
    fun previous() = controller?.seekToPreviousMediaItem()
    fun seekTo(positionMs: Long) = controller?.seekTo(positionMs)

    fun release() {
        released = true
        mainHandler.removeCallbacks(progressTicker)
        playerListener?.let { controller?.removeListener(it) }
        playerListener = null
        controller?.release()
        controller = null
        pendingQueue = emptyList()
        _state.update { PlayerUiState() }
    }
}
