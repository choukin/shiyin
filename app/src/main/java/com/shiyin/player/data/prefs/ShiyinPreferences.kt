package com.shiyin.player.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "shiyin_prefs")

/** 轻量持久化：广告条关闭态、收藏。曲库列表本身不落库。 */
class ShiyinPreferences(private val context: Context) {
    private val appContext = context.applicationContext

    private object Keys {
        val AD_DISMISSED = booleanPreferencesKey("ad_banner_dismissed")
        val FAVORITES = stringSetPreferencesKey("favorites")
    }

    val adDismissed: Flow<Boolean> = appContext.dataStore.data
        .map { prefs -> prefs[Keys.AD_DISMISSED] ?: false }

    val favorites: Flow<Set<String>> = appContext.dataStore.data
        .map { prefs -> prefs[Keys.FAVORITES] ?: emptySet() }

    suspend fun dismissAd() {
        appContext.dataStore.edit { prefs -> prefs[Keys.AD_DISMISSED] = true }
    }

    suspend fun toggleFavorite(trackId: String) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITES] ?: emptySet()
            prefs[Keys.FAVORITES] = if (trackId in current) current - trackId else current + trackId
        }
    }
}
