package com.shiyin.player.data.util

import com.shiyin.player.data.model.Playlist
import java.util.Base64

/**
 * 歌单持久化编解码（纯 JVM，可单测）。
 *
 * 存储格式：每个歌单一行，字段用 \u0001 分隔：
 *   id \u0001 nameBase64 \u0001 createdAt \u0001 trackId1,trackId2,...
 * 用 Base64 编码名称，避免分隔符/换行注入；不依赖 org.json，本地单测可直接跑。
 */
object PlaylistCodec {

    private const val FIELD = "\u0001"
    private const val LINE = "\n"
    private val b64 = Base64.getEncoder()
    private val b64dec = Base64.getDecoder()

    fun encode(playlists: List<Playlist>): String = buildString {
        playlists.forEach { p ->
            append(p.id).append(FIELD)
            append(b64.encodeToString(p.name.toByteArray(Charsets.UTF_8))).append(FIELD)
            append(p.createdAt).append(FIELD)
            append(p.trackIds.joinToString(","))
            append(LINE)
        }
    }

    fun decode(raw: String): List<Playlist> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val parts = line.split(FIELD)
            if (parts.size != 4) return@mapNotNull null
            runCatching {
                Playlist(
                    id = parts[0],
                    name = String(b64dec.decode(parts[1]), Charsets.UTF_8),
                    createdAt = parts[2].toLongOrNull() ?: 0L,
                    trackIds = if (parts[3].isBlank()) {
                        emptyList()
                    } else {
                        parts[3].split(",").mapNotNull { it.toLongOrNull() }
                    }
                )
            }.getOrNull()
        }.toList()
    }
}
