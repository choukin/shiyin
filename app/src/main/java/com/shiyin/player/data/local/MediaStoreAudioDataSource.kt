package com.shiyin.player.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.shiyin.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 曲库数据源：一期纯 MediaStore 查询，不落库、不联网、不引 Room。
 * 查询在 Dispatchers.IO 执行；结果按 DATE_ADDED DESC 即「最近添加」。
 *
 * 说明：content:// URI（ContentUris.withAppendedId）在 API 26–37 通用，
 * 因此不读取 API ≤28 的 DATA 列，也不读取已废弃的 ALBUM_ART 列。
 */
class MediaStoreAudioDataSource(private val context: Context) {

    suspend fun queryAll(): List<Track> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        val resolver = context.applicationContext.contentResolver
        val tracks = mutableListOf<Track>()

        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val displayName = cursor.getString(displayNameCol).orEmpty()
                val title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                    ?: displayName.substringBeforeLast('.')
                tracks += Track(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                    title = title,
                    artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() }.orEmpty(),
                    album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }.orEmpty(),
                    albumId = cursor.getLong(albumIdCol),
                    durationMs = cursor.getLong(durationCol),
                    dateAddedSec = cursor.getLong(dateAddedCol),
                    displayName = displayName
                )
            }
        }
        tracks
    }
}
