package com.shiyin.player.data.util

import com.shiyin.player.data.model.TrackMetadata

/**
 * 纯派生：把 MediaStore 原始曲目与「应用内覆盖 + 隐藏集合」合成展示字段。
 * 不含 Android 依赖，便于本地单测；Uri 解析由 Repository 负责。
 */
object TrackMetadataApplier {

    data class Fields(
        val title: String,
        val description: String,
        val artworkFile: String
    )

    /** 返回 null 表示该曲目已被「仅从列表移除」，不应出现在曲库。 */
    fun resolve(
        trackId: Long,
        originalTitle: String,
        metadata: Map<Long, TrackMetadata>,
        hiddenIds: Set<String>
    ): Fields? {
        if (trackId.toString() in hiddenIds) return null
        val meta = metadata[trackId]
        return Fields(
            // 空名称视为恢复 MediaStore 原始名称，避免曲库出现无标题行。
            title = meta?.title?.takeIf { it.isNotBlank() } ?: originalTitle,
            description = meta?.description.orEmpty(),
            artworkFile = meta?.artworkFile.orEmpty()
        )
    }
}
