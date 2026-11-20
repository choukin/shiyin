package com.shiyin.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shiyin.player.data.model.Playlist
import com.shiyin.player.data.util.PlaylistCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shiyin_prefs")

/** 轻量持久化：收藏、歌单。曲库列表本身不落库。 */
class ShiyinPreferences(private val context: Context) {
    private val appContext = context.applicationContext

    private object Keys {
        val FAVORITES = stringSetPreferencesKey("favorites")
        val PLAYLISTS = stringPreferencesKey("playlists")
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
}
