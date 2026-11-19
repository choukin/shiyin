package com.shiyin.player.data.local

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 专辑封面解析（三级兜底，全部失败返回 null，由 GradientAlbumArt 用渐变兜底）：
 *  1. API 29+ 用 ContentResolver.loadThumbnail(albumUri) —— 不读废弃的 ALBUM_ART 列；
 *  2. 全版本可用 MediaMetadataRetriever 读文件内嵌封面；
 *  3. 仍失败 → null。
 */
class AlbumArtResolver(private val context: Context) {

    suspend fun resolve(albumId: Long, trackUri: Uri): ImageBitmap? = withContext(Dispatchers.IO) {
        albumThumbnail(albumId) ?: embeddedPicture(trackUri)
    }

    private fun albumThumbnail(albumId: Long): ImageBitmap? {
        if (albumId <= 0L || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            val albumUri = ContentUris.withAppendedId(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                albumId
            )
            context.applicationContext.contentResolver
                .loadThumbnail(albumUri, Size(512, 512), null)
                .asImageBitmap()
        }.getOrNull()
    }

    private fun embeddedPicture(trackUri: Uri): ImageBitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context.applicationContext, trackUri)
            retriever.embeddedPicture?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }
}
