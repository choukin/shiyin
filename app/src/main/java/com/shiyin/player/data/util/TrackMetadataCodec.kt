package com.shiyin.player.data.util

import com.shiyin.player.data.model.TrackMetadata
import java.util.Base64

/**
 * 曲目覆盖信息的持久化编解码（纯 JVM，可单测）。
 *
 * 存储格式：每条一行，字段以 \u0001 分隔：
 *   trackId \u0001 titleBase64 \u0001 descriptionBase64 \u0001 artworkFile
 * 文本字段用 Base64 避免换行 / 分隔符注入；artworkFile 是应用内生成的文件名，
 * 不含分隔符。兼容早期只写了 3 个字段的记录（视为无自定义封面）。
 */
object TrackMetadataCodec {

    private const val FIELD = "\u0001"
    private const val LINE = "\n"
    private val encoder = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    fun encode(items: Collection<TrackMetadata>): String = buildString {
        items.sortedBy { it.trackId }.forEach { meta ->
            if (meta.isBlank) return@forEach
            append(meta.trackId).append(FIELD)
            append(encoder.encodeToString(meta.title.toByteArray(Charsets.UTF_8))).append(FIELD)
            append(encoder.encodeToString(meta.description.toByteArray(Charsets.UTF_8))).append(FIELD)
            append(meta.artworkFile)
            append(LINE)
        }
    }

    fun decode(raw: String): Map<Long, TrackMetadata> {
        if (raw.isBlank()) return emptyMap()
        return raw.lineSequence().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split(FIELD)
            if (parts.size < 3) return@mapNotNull null
            val id = parts[0].toLongOrNull() ?: return@mapNotNull null
            runCatching {
                TrackMetadata(
                    trackId = id,
                    title = String(decoder.decode(parts[1]), Charsets.UTF_8),
                    description = String(decoder.decode(parts[2]), Charsets.UTF_8),
                    artworkFile = parts.getOrNull(3).orEmpty()
                )
            }.getOrNull()
        }.associateBy { it.trackId }
    }
}
