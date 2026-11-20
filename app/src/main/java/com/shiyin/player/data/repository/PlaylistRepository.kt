package com.shiyin.player.data.repository

import com.shiyin.player.data.model.Playlist
import com.shiyin.player.data.prefs.ShiyinPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

/** 歌单仓库：CRUD 全部基于 preferences 里的序列化字符串，无数据库。 */
class PlaylistRepository(private val preferences: ShiyinPreferences) {

    val playlists: Flow<List<Playlist>> = preferences.playlists

    suspend fun create(name: String): Playlist {
        val playlist = Playlist(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "新建歌单" },
            trackIds = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        preferences.savePlaylists(snapshot() + playlist)
        return playlist
    }

    suspend fun rename(id: String, newName: String) {
        preferences.savePlaylists(
            snapshot().map { if (it.id == id) it.copy(name = newName.trim().ifBlank { it.name }) else it }
        )
    }

    suspend fun delete(id: String) {
        preferences.savePlaylists(snapshot().filterNot { it.id == id })
    }

    suspend fun addTracks(id: String, trackIds: List<Long>) {
        preferences.savePlaylists(
            snapshot().map { p ->
                if (p.id == id) p.copy(trackIds = (p.trackIds + trackIds).distinct()) else p
            }
        )
    }

    suspend fun removeTrack(id: String, trackId: Long) {
        preferences.savePlaylists(
            snapshot().map { p ->
                if (p.id == id) p.copy(trackIds = p.trackIds.filterNot { it == trackId }) else p
            }
        )
    }

    /** 读取当前快照。Flow 是冷的，这里用 first() 取最新值。 */
    private suspend fun snapshot(): List<Playlist> = preferences.playlists.first()
}
