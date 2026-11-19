package com.shiyin.player.di

import android.content.Context
import com.shiyin.player.data.local.AlbumArtResolver
import com.shiyin.player.data.local.MediaStoreAudioDataSource
import com.shiyin.player.data.media.MediaEditRepository
import com.shiyin.player.data.media.WaveformExtractor
import com.shiyin.player.data.prefs.ShiyinPreferences
import com.shiyin.player.data.repository.LibraryRepository
import com.shiyin.player.playback.PlayerController

/**
 * 手动 DI 容器（一期刻意不引 Hilt/Koin/KSP）。
 * 全部 lazy 单例；PlayerController 只在此创建，实际连接由其 connect() 懒触发。
 * MediaSessionService 由系统实例化，不在此容器内。
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferences: ShiyinPreferences by lazy { ShiyinPreferences(appContext) }
    val audioDataSource: MediaStoreAudioDataSource by lazy { MediaStoreAudioDataSource(appContext) }
    val albumArtResolver: AlbumArtResolver by lazy { AlbumArtResolver(appContext) }
    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(audioDataSource, preferences)
    }
    val playerController: PlayerController by lazy { PlayerController(appContext) }

    // 二期：纯本地的波形提取与媒体编辑/导入管线（零网络）。
    val waveformExtractor: WaveformExtractor by lazy { WaveformExtractor(appContext) }
    val mediaEditRepository: MediaEditRepository by lazy { MediaEditRepository(appContext) }
}
