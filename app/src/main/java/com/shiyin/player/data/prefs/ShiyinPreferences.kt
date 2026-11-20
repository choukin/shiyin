package com.shiyin.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shiyin.player.data.model.Playlist
import com.shiyin.player.data.model.TrackMetadata
import com.shiyin.player.data.util.PlaylistCodec
import com.shiyin.player.data.util.TrackMetadataCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shiyin_prefs")

/** 轻量持久化：收藏、歌单、曲目信息覆盖、仅从列表移除的曲目 id。曲库列表本身不落库。 */
class ShiyinPreferences(private val context: Context) {
    private val appContext = context.applicationContext

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val PLAYLISTS = stringPreferencesKey("playlists")
        val HIDDEN_TRACKS = stringSetPreferencesKey("hidden_tracks")
        val TRACK_METADATA = stringPreferencesKey("track_metadata")
    }

    val favorites: Flow<Set<String>> = appContext.dataStore.data
        .map { prefs -> prefs[Keys.FAVORITES] ?: emptySet() }

    suspend fun toggleFavorite(trackId: String) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (trackId in current) current - trackId else current + trackId
        }
    }

    val playlists: Flow<List<Playlist>> = appContext.dataStore.data
        .map { prefs -> PlaylistCodec.decode(prefs[Keys.PLAYLISTS].orEmpty()) }

    suspend fun savePlaylists(playlists: List<Playlist>) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.PLAYLISTS] = PlaylistCodec.encode(playlists)
        }
    }

    /** 「仅从列表移除」的曲目：文件仍在手机里，只是曲库不再展示，可整体恢复。 */
    val hiddenTrackIds: Flow<Set<String>> = appContext.dataStore.data
        .map { prefs -> prefs[Keys.HIDDEN_TRACKS] ?: emptySet() }

    suspend fun hideTrack(trackId: String) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[Keys.HIDDEN_TRACKS] ?: emptySet()
            prefs[Keys.HIDDEN_TRACKS] = current + trackId
        }
    }

    suspend fun restoreHiddenTracks() {
        appContext.dataStore.edit { prefs -> prefs[Keys.HIDDEN_TRACKS] = emptySet() }
    }

    val trackMetadata: Flow<Map<Long, TrackMetadata>> = appContext.dataStore.data
        .map { prefs -> TrackMetadataCodec.decode(prefs[Keys.TRACK_METADATA].orEmpty()) }

    /** 合并写入；三项都为空时移除该条目，避免无意义的存储。 */
    suspend fun saveTrackMetadata(metadata: TrackMetadata) {
        appContext.dataStore.edit { prefs ->
            val current = TrackMetadataCodec.decode(prefs[Keys.TRACK_METADATA].orEmpty()).toMutableMap()
            if (metadata.isBlank) {
                current.remove(metadata.trackId)
            } else {
                current[metadata.trackId] = metadata
            }
            prefs[Keys.TRACK_METADATA] = TrackMetadataCodec.encode(current.values)
        }
    }
}
