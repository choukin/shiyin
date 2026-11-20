package com.shiyin.player.data.util

import com.shiyin.player.data.model.LibraryGroupKind
import java.text.Collator
import java.util.Locale

/**
 * 分组用的纯数据投影：刻意不含 android.net.Uri，使分组逻辑可在 JVM 单测中直接验证。
 * 由 UI 层从 Track 映射而来。
 */
data class GroupSource(
    val id: Long,
    val artist: String,
    val album: String,
    val albumId: Long
)

/**
 * 一个专辑 / 歌手分组。
 * [key] 空串代表未知；[trackIds] 用于二级页从全量曲库按原顺序取回曲目。
 */
data class LibraryGroup(
    val key: String,
    val trackCount: Int,
    val trackIds: List<Long>,
    val albumId: Long,
    val representativeId: Long
)

object LibraryGrouping {

    /** MediaStore 对缺失元数据返回 `<unknown>`；统一折叠为空 key，由 UI 本地化。 */
    const val UNKNOWN_KEY = ""

    private val UNKNOWN_TOKENS = setOf("<unknown>", "(unknown)", "unknown")

    /** 中文专辑 / 歌手按拼音序（PRIMARY 强度同时忽略英文大小写），纯 JDK 实现。 */
    private val collator = Collator.getInstance(Locale.CHINA).apply {
        strength = Collator.PRIMARY
    }

    fun normalize(raw: String): String {
        val value = raw.trim()
        return if (value.isEmpty() || UNKNOWN_TOKENS.any { it.equals(value, ignoreCase = true) }) {
            UNKNOWN_KEY
        } else {
            value
        }
    }

    /** DataStore 里收藏 id 存的是 String，Track.id 是 Long；非法项静默忽略。 */
    fun parseFavoriteIds(favorites: Set<String>): Set<Long> =
        favorites.mapNotNull { it.toLongOrNull() }.toSet()

    /** 按专辑或歌手分组，中文按拼音序、英文忽略大小写，未知项恒排末尾。 */
    fun group(sources: List<GroupSource>, kind: LibraryGroupKind): List<LibraryGroup> =
        sources
            .groupBy { normalize(if (kind == LibraryGroupKind.ALBUM) it.album else it.artist) }
            .map { (key, items) ->
                LibraryGroup(
                    key = key,
                    trackCount = items.size,
                    trackIds = items.map { it.id },
                    albumId = items.firstOrNull { it.albumId > 0 }?.albumId ?: 0L,
                    representativeId = items.first().id
                )
            }
            .sortedWith { a, b ->
                when {
                    a.key.isEmpty() != b.key.isEmpty() -> if (a.key.isEmpty()) 1 else -1
                    else -> collator.compare(a.key, b.key)
                }
            }
}
