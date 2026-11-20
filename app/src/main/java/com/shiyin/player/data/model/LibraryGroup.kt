package com.shiyin.player.data.model

/** 曲库分组维度：专辑 / 歌手。 */
enum class LibraryGroupKind { ALBUM, ARTIST }

/**
 * 当前打开的分组二级页。
 * [key] 为规范化后的分组值，空串表示「未知专辑 / 未知歌手」（UI 层本地化）。
 */
data class LibraryGroupDetail(
    val kind: LibraryGroupKind,
    val key: String
)
