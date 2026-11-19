package com.shiyin.player.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.shiyin.player.MainActivity

/**
 * 后台播放核心。用 MediaSessionService 而非 Activity 持有 ExoPlayer：
 *  - 服务生命周期独立于 UI，切后台 / 配置变更 / 进程回收不中断播放；
 *  - MediaSession 向系统发布状态与元数据，锁屏、通知、蓝牙、Android Auto 统一控制；
 *  - Media3 自动生成 media-style 通知并在播放开始时把服务置为前台。
 *
 * 播放队列不由服务自己「造」：UI 侧 MediaController 通过 setMediaItems 直接设置
 * （MediaController 实现 Player 接口，默认 onConnect 已授予 COMMAND_SET_MEDIA_ITEM）。
 * 因此这里刻意不覆盖 MediaSession.Callback：既不需要自定义 SessionCommand / Guava
 * Future 编解码，也避免 Media3 1.11.1 的 ConnectionResult API 签名差异导致编译失败。
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        // handleAudioFocus = true：由 ExoPlayer 统一处理音频焦点，避免与其它播放器抢焦点
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .build()

        // 通知 / 锁屏点击回到播放器界面
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onDestroy() {
        mediaSession?.let { session ->
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
