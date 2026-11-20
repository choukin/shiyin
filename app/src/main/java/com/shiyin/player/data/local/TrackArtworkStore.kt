package com.shiyin.player.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File

/**
 * 自定义封面存储在应用私有 filesDir/covers/，不写进系统相册、不需要存储权限。
 * 选图通过 SAF 拿到 content:// 后复制到这里并压成 JPEG，避免外部 Uri 授权失效。
 */
class TrackArtworkStore(private val context: Context) {

    private val appContext = context.applicationContext

    private fun coverDir(): File = File(appContext.filesDir, COVER_DIR).apply { mkdirs() }

    /** 文件名 → 可读取的 Uri；文件已丢失时返回 null，让 UI 回落到系统封面 / 渐变。 */
    fun uriFor(fileName: String): Uri? {
        if (fileName.isBlank()) return null
        val file = File(coverDir(), fileName)
        return if (file.isFile) Uri.fromFile(file) else null
    }

    /**
     * 复制并压缩一张图片为固定文件名，返回文件名；失败返回 null。
     * 同名文件直接覆盖，因此重复换图不会累积垃圾文件。
     */
    fun save(trackId: Long, source: Uri): String? = runCatching {
        val bitmap = decode(source)
        if (bitmap == null) {
            Log.w(TAG, "cover decode returned null for $source")
            return null
        }
        val fileName = "cover_${trackId}.jpg"
        try {
            val target = File(coverDir(), fileName)
            target.outputStream().use { out ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)) {
                    "cover compress failed"
                }
            }
        } finally {
            bitmap.recycle()
        }
        fileName
    }.onFailure { error ->
        Log.w(TAG, "cover save failed for $source", error)
    }.getOrNull()

    fun remove(fileName: String) {
        if (fileName.isBlank()) return
        runCatching { File(coverDir(), fileName).delete() }
    }

    /**
     * 单遍解码后缩到 640px 以内。
     *
     * 不做 inJustDecodeBounds 的两遍解码：Photo Picker 返回的是
     * content://media/picker_get_content/... 这类带 redaction 的 Uri，
     * 第二遍重新 openInputStream 时 bounds 解码会拿不到像素流，导致直接返回 null。
     * 用 RGB_565 把 12MP 级图片的峰值内存压到约 24MB，缩完立即回收原图。
     */
    private fun decode(source: Uri): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val decoded = appContext.contentResolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val scale = MAX_EDGE.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
        if (scale >= 1f) return decoded
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled != decoded) decoded.recycle()
        return scaled
    }

    private companion object {
        const val TAG = "TrackArtworkStore"
        const val COVER_DIR = "covers"
        const val MAX_EDGE = 640
    }
}
