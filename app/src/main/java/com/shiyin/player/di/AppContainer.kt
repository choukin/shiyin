package com.shiyin.player.di

import android.content.Context
import com.shiyin.player.data.local.AlbumArtResolver
import com.shiyin.player.data.local.MediaStoreAudioDataSource
import com.shiyin.player.data.media.LinkDownloadRepository
import com.shiyin.player.data.media.MediaDeleteRepository
import com.shiyin.player.data.media.MediaEditRepository
import com.shiyin.player.data.media.MusicSearchRepository
import com.shiyin.player.data.media.WaveformExtractor
import com.shiyin.player.data.prefs.ShiyinPreferences
import com.shiyin.player.data.repository.LibraryRepository
import com.shiyin.player.data.repository.PlaylistRepository
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
    val playlistRepository: PlaylistRepository by lazy { PlaylistRepository(preferences) }
    val playerController: PlayerController by lazy { PlayerController(appContext) }
    // 曲库删除真实媒体文件（回收站 / 永久删除，均走系统确认弹窗）。
    val mediaDeleteRepository: MediaDeleteRepository by lazy { MediaDeleteRepository(appContext) }

    // 二期：05/06 纯本地的波形提取与媒体编辑管线。
    val waveformExtractor: WaveformExtractor by lazy { WaveformExtractor(appContext) }
    val mediaEditRepository: MediaEditRepository by lazy { MediaEditRepository(appContext) }
    // 二期：04 下载，OkHttp 直链 GET → 缓存 → 复用 mediaEditRepository 入库。
    val linkDownloadRepository: LinkDownloadRepository by lazy {
        LinkDownloadRepository(appContext, mediaEditRepository)
    }
    // 04 下载页「搜歌」：OkHttp POST myfreemp3 公开接口，返回列表供试听 / 下载。
    // 试听播放器带生命周期，由 ViewModel 持有，不放进全局容器（与 ClipPreviewPlayer 同策略）。
    val musicSearchRepository: MusicSearchRepository by lazy { MusicSearchRepository() }
}
