package com.shiyin.player.data.repository

import com.shiyin.player.data.local.MediaStoreAudioDataSource
import com.shiyin.player.data.model.Track
import com.shiyin.player.data.prefs.ShiyinPreferences
import kotlinx.coroutines.flow.Flow

/** 聚合本机音频查询与轻量偏好；不含网络、不含数据库。 */
class LibraryRepository(
    private val dataSource: MediaStoreAudioDataSource,
    private val preferences: ShiyinPreferences
) {
    suspend fun loadTracks(): List<Track> = dataSource.queryAll()

    fun observeAdDismissed(): Flow<Boolean> = preferences.adDismissed

    fun observeFavorites(): Flow<Set<String>> = preferences.favorites

    suspend fun dismissAd() = preferences.dismissAd()

    suspend fun toggleFavorite(trackId: String) = preferences.toggleFavorite(trackId)
}
