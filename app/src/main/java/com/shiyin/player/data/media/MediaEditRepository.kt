package com.shiyin.player.data.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.shiyin.player.data.model.MediaSource
import com.shiyin.player.data.util.ClipRange
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * 二期的媒体编辑与入库管线。
 *
 * - [probe]：用 MediaExtractor 读取时长与轨道信息（不解码）。
 * - [exportAudio]：Media3 Transformer 1.11.1。裁剪走
 *   [MediaItem.ClippingConfiguration]，视频提取走
 *   [EditedMediaItem.Builder.setRemoveVideo]；输出 AAC/m4a 到缓存文件。
 * - [publish]：把结果写入 MediaStore 音乐库，写入完成后清 IS_PENDING，
 *   这样 LibraryRepository 的 DATE_ADDED DESC 查询能立刻看到新文件。
 * - [importAudio]：04 屏次要入口「导入本地文件」——把 SAF 选中的音频原样复制到媒体库。
 * - [publishFile]：04 屏主入口「链接下载」——LinkDownloadRepository 把直链下载到缓存后
 *   经此方法入库，复用同一套 IS_PENDING 发布流程。
 *
 * 不使用 ffmpeg-kit（已退役），不申请 MANAGE_EXTERNAL_STORAGE。
 */
class MediaEditRepository(private val context: Context) {

    /** 读取所选媒体的元信息；失败时抛出，由 ViewModel 转成错误态。 */
    suspend fun probe(uri: Uri): MediaSource = withContext(Dispatchers.IO) {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(context, uri, null)
            var durationMs = 0L
            var hasVideo = false
            var hasAudio = false
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                when {
                    mime.startsWith("video/") -> hasVideo = true
                    mime.startsWith("audio/") -> {
                        hasAudio = true
                        if (format.containsKey(MediaFormat.KEY_DURATION)) {
                            durationMs = maxOf(
                                durationMs,
                                format.getLong(MediaFormat.KEY_DURATION) / 1000L
                            )
                        }
                    }
                }
            }
            MediaSource(
                uri = uri,
                displayName = resolveDisplayName(uri),
                durationMs = durationMs,
                hasVideo = hasVideo,
                hasAudio = hasAudio
            )
        } finally {
            extractor.release()
        }
    }

    /**
     * 导出音频。clipRange 为整段时可传 ClipRange.full(durationMs) 或空区间（不设 clipping）。
     * 返回写入媒体库的文件名，供 UI 显示「导出成功」真实态。
     */
    suspend fun exportAudio(
        sourceUri: Uri,
        clipRange: ClipRange,
        removeVideo: Boolean,
        baseName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "shiyin_export_${System.currentTimeMillis()}.m4a")
        runCatching {
            // Transformer 需要一个带 Looper 的线程来构建与派发回调，固定在主线程启动。
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    val listener = object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult
                        ) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    IllegalStateException(
                                        exportException.message ?: "导出失败",
                                        exportException
                                    )
                                )
                            }
                        }
                    }

                    val transformer = Transformer.Builder(context)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(listener)
                        .build()

                    val itemBuilder = MediaItem.Builder().setUri(sourceUri)
                    if (clipRange.lengthMs > 0L) {
                        itemBuilder.setClippingConfiguration(
                            MediaItem.ClippingConfiguration.Builder()
                                .setStartPositionMs(clipRange.startMs)
                                .setEndPositionMs(clipRange.endMs)
                                .build()
                        )
                    }
                    val edited = EditedMediaItem.Builder(itemBuilder.build())
                        .setRemoveVideo(removeVideo)
                        .build()

                    try {
                        transformer.start(edited, temp.absolutePath)
                    } catch (error: Throwable) {
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                    continuation.invokeOnCancellation { transformer.cancel() }
                }
            }

            val displayName = exportName(baseName)
            publish(temp, displayName, MIME_AAC)
            temp.delete()
            displayName
        }.onFailure { temp.delete() }
    }

    /** 04 屏次要入口：把用户选中的本地音频原样导入音乐库（无网络、无转码）。 */
    suspend fun importAudio(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = resolveDisplayName(uri)
            val mime = context.contentResolver.getType(uri) ?: MIME_AUDIO_FALLBACK
            val target = insertPending(displayName, mime)
            val resolver = context.contentResolver
            val input = resolver.openInputStream(uri) ?: error("无法读取所选文件")
            val output = resolver.openOutputStream(target) ?: error("无法写入音乐库")
            input.use { source -> output.use { destination -> source.copyTo(destination) } }
            markReady(target)
            displayName
        }
    }

    /**
     * 供 04 链接下载复用：把缓存目录中的文件写入音乐库并返回显示名。
     * 与 [exportAudio] / [importAudio] 共用 IS_PENDING 发布流程，曲库可立即扫描到。
     */
    fun publishFile(temp: File, displayName: String, mime: String): String {
        publish(temp, displayName, mime)
        return displayName
    }

    private fun publish(temp: File, displayName: String, mime: String) {
        val target = insertPending(displayName, mime)
        try {
            val output = context.contentResolver.openOutputStream(target)
                ?: error("无法写入音乐库")
            temp.inputStream().use { source -> output.use { destination -> source.copyTo(destination) } }
            markReady(target)
        } catch (error: Throwable) {
            // 写入失败不能留下 IS_PENDING=1 的幽灵记录。
            context.contentResolver.delete(target, null, null)
            throw error
        }
    }

    private fun insertPending(displayName: String, mime: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.IS_MUSIC, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC + EXPORT_SUB_DIR
                )
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        return context.contentResolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
        ) ?: error("无法在音乐库创建文件")
    }

    private fun markReady(target: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(target, values, null, null)
        }
    }

    private fun resolveDisplayName(uri: Uri): String {
        val cursor = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return it.getString(index) ?: FALLBACK_NAME
            }
        }
        return uri.lastPathSegment ?: FALLBACK_NAME
    }

    private fun exportName(baseName: String): String {
        val stem = baseName
            .substringBeforeLast('.')
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .ifBlank { FALLBACK_NAME }
        return "$stem$EXPORT_SUFFIX"
    }

    private companion object {
        const val MIME_AAC = "audio/mp4"
        const val MIME_AUDIO_FALLBACK = "audio/mpeg"
        const val EXPORT_SUB_DIR = "/Shiyin"
        const val EXPORT_SUFFIX = "_拾音.m4a"
        const val FALLBACK_NAME = "拾音音频"
    }
}
