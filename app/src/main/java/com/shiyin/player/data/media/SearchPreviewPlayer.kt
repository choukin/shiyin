package com.shiyin.player.data.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.shiyin.player.data.util.SearchLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 04 下载页「搜歌」结果的试听播放器。
 *
 * 与 ClipPreviewPlayer 同理，刻意独立于后台曲库播放会话（PlaybackService）：
 * 试听的是网络直链，不应写进曲库队列，也不该改变「正在播放」的本地歌曲。
 *
 * 关键点：播放地址是 https，但 302 后会落到 http 的 126.net CDN，
 * 故显式开启 [DefaultHttpDataSource.Factory.setAllowCrossProtocolRedirects]，
 * 否则 ExoPlayer 会因跨协议重定向被拒而报错。
 */
class SearchPreviewPlayer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var ticker: Job? = null
    private var currentId: String? = null

    /** 播放态 / 进度回调，由 ViewModel 写入 DownloadUiState。 */
    var onStateChanged: ((isPlaying: Boolean, positionMs: Long, durationMs: Long) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            emit()
            if (isPlaying) startTicker() else stopTicker()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) emit()
        }

        override fun onPlayerError(error: PlaybackException) {
            stopTicker()
            onError?.invoke(error.message.orEmpty())
        }
    }

    /** 播放指定歌曲；同一首歌再次调用等价于继续播放。 */
    fun play(id: String, url: String) {
        if (currentId == id && player != null) {
            player?.play()
            return
        }
        releasePlayer()
        currentId = id

        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(SearchLink.USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        player = ExoPlayer.Builder(appContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setMediaSourceFactory(ProgressiveMediaSource.Factory(httpFactory))
            .build()
            .apply {
                addListener(listener)
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                play()
            }
        emit()
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun stop() {
        releasePlayer()
        onStateChanged?.invoke(false, 0L, 0L)
    }

    fun release() {
        stop()
        scope.cancel()
        onStateChanged = null
        onError = null
    }

    private fun releasePlayer() {
        stopTicker()
        player?.let {
            it.removeListener(listener)
            it.stop()
            it.release()
        }
        player = null
        currentId = null
    }

    private fun emit() {
        val current = player
        val duration = current?.duration?.takeIf { it > 0 && it != C.TIME_UNSET } ?: 0L
        val position = current?.currentPosition?.takeIf { it >= 0 } ?: 0L
        onStateChanged?.invoke(current?.isPlaying == true, position, duration)
    }

    private fun startTicker() {
        stopTicker()
        ticker = scope.launch {
            while (isActive) {
                emit()
                delay(PROGRESS_INTERVAL_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 500L
    }
}
