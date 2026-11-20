package com.shiyin.player.data.media

import android.content.Context
import android.net.Uri
import android.view.TextureView
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.shiyin.player.data.util.ClipRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 05 音乐裁剪 / 06 视频提取音频的试听播放器。
 *
 * 刻意独立于后台曲库播放会话（PlaybackService），原因：
 *  - 试听只播放 SAF 选中的单个文件，不走曲库队列；
 *  - 试听范围必须与 Transformer 导出范围完全一致 —— 两者共用 ClipRange + Media3 ClippingConfiguration；
 *  - repeatMode=ONE 让选区循环播放，方便反复拖滑块对比裁剪点。
 *
 * 生命周期由 EditorViewModel 持有：选源 prepare、改区间 updateRange、onCleared release。
 */
class ClipPreviewPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var videoView: TextureView? = null
    private var progressJob: Job? = null

    private var currentUri: Uri? = null
    private var currentRange: ClipRange? = null
    private var currentHasVideo = false

    /** 播放态变化回调，由 ViewModel 写入 EditorUiState。 */
    var onPlayingChanged: ((Boolean) -> Unit)? = null
    /** 播放进度轮询（毫秒，相对媒体起点），由 ViewModel 写入 EditorUiState。 */
    var onProgress: ((Long) -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            onPlayingChanged?.invoke(isPlaying)
            if (isPlaying) startTicker() else stopTicker()
        }
    }

    /**
     * 建立（或重建）播放器。range 决定试听区间；autoPlay 控制建立后是否立即播放。
     * 视频画面通过 [attachVideoView] 挂载，两种先后调用顺序都能正确 attach。
     */
    fun prepare(uri: Uri, range: ClipRange, hasVideo: Boolean, autoPlay: Boolean = true) {
        releasePlayer()
        currentUri = uri
        currentRange = range
        currentHasVideo = hasVideo

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(range.startMs)
                    .setEndPositionMs(range.endMs)
                    .build()
            )
            .build()

        player = ExoPlayer.Builder(appContext)
            .setAudioAttributes(audioAttributes, true)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                setMediaItem(mediaItem)
                if (hasVideo) videoView?.let { setVideoTextureView(it) }
                addListener(listener)
                prepare()
                if (autoPlay) play()
            }
        onProgress?.invoke(range.startMs)
    }

    /** RangeSlider 变化后重建选区；若之前在播放则续播，否则保持暂停。 */
    fun updateRange(range: ClipRange) {
        val uri = currentUri ?: return
        val wasPlaying = player?.isPlaying == true
        prepare(uri, range, currentHasVideo, autoPlay = wasPlaying)
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun stop() {
        stopTicker()
        player?.let {
            it.removeListener(listener)
            it.stop()
        }
        onPlayingChanged?.invoke(false)
    }

    /** Compose AndroidView 创建 TextureView 后调用；若播放器已存在则立即 attach。 */
    fun attachVideoView(view: TextureView) {
        videoView = view
        if (currentHasVideo) player?.setVideoTextureView(view)
    }

    fun detachVideoView(view: TextureView) {
        if (videoView === view) {
            player?.setVideoTextureView(null)
            videoView = null
        }
    }

    /** 整个试听器销毁：ViewModel.onCleared 调用。 */
    fun release() {
        releasePlayer()
        scope.cancel()
        onPlayingChanged = null
        onProgress = null
    }

    private fun releasePlayer() {
        stopTicker()
        player?.let {
            it.removeListener(listener)
            it.setVideoTextureView(null)
            it.release()
        }
        player = null
        videoView = null
        onPlayingChanged?.invoke(false)
    }

    private fun startTicker() {
        stopTicker()
        progressJob = scope.launch {
            while (isActive) {
                val p = player
                val range = currentRange
                if (p != null && range != null) {
                    // ClippingConfiguration 下 currentPosition 落在 [startMs, endMs]，钳制兜底防越界。
                    val pos = p.currentPosition.coerceIn(range.startMs, range.endMs)
                    onProgress?.invoke(pos)
                }
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    companion object {
        private const val PROGRESS_INTERVAL_MS = 200L
    }
}
