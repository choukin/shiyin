package com.shiyin.player.data.local

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.shiyin.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 封面解析入口：优先用曲目编辑时设置的自定义封面，否则走 MediaStore 专辑图 / 文件内嵌封面。
 * 编辑弹窗的选图预览也复用 [load]。
 */
class TrackArtworkResolver(
    private val context: Context,
    private val albumArtResolver: AlbumArtResolver
) {
    private val appContext = context.applicationContext

    suspend fun resolve(track: Track): ImageBitmap? = withContext(Dispatchers.IO) {
        track.artworkUri?.let { decode(it) } ?: albumArtResolver.resolve(track.albumId, track.uri)
    }

    suspend fun load(uri: Uri): ImageBitmap? = withContext(Dispatchers.IO) { decode(uri) }

    private fun decode(uri: Uri): ImageBitmap? = runCatching {
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull()
}
