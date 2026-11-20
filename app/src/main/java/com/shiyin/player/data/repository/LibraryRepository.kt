package com.shiyin.player.data.repository

import android.net.Uri
import com.shiyin.player.data.local.MediaStoreAudioDataSource
import com.shiyin.player.data.local.TrackArtworkStore
import com.shiyin.player.data.model.Track
import com.shiyin.player.data.model.TrackMetadata
import com.shiyin.player.data.prefs.ShiyinPreferences
import com.shiyin.player.data.util.TrackMetadataApplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 聚合本机音频查询与轻量偏好；不含网络、不含数据库。
 *
 * 查询时叠加两层应用内状态：
 * - hiddenTrackIds：用户选择「仅从列表移除」的曲目不再出现，但手机文件仍在；
 * - TrackMetadata：名称 / 简介 / 自定义封面覆盖显示，不写回 MediaStore。
 */
class LibraryRepository(
    private val dataSource: MediaStoreAudioDataSource,
    private val preferences: ShiyinPreferences,
    private val artworkStore: TrackArtworkStore
) {
    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val hidden = preferences.hiddenTrackIds.first()
        val metadata = preferences.trackMetadata.first()
        dataSource.queryAll().mapNotNull { track ->
            val fields = TrackMetadataApplier.resolve(
                trackId = track.id,
                originalTitle = track.title,
                metadata = metadata,
                hiddenIds = hidden
            ) ?: return@mapNotNull null
            track.copy(
                title = fields.title,
                description = fields.description,
                artworkUri = artworkStore.uriFor(fields.artworkFile)
            )
        }
    }

    fun observeFavorites(): Flow<Set<String>> = preferences.favorites

    fun observeHiddenTrackIds(): Flow<Set<String>> = preferences.hiddenTrackIds

    suspend fun toggleFavorite(trackId: String) = preferences.toggleFavorite(trackId)

    /**
     * 保存曲目编辑。artworkSource 为 SAF 选图返回的 Uri；removeArtwork=true 时清掉自定义封面。
     * 复制图片在 IO 线程完成，失败时保留原有封面。
     */
    suspend fun saveTrackEdit(
        trackId: Long,
        title: String,
        description: String,
        artworkSource: Uri? = null,
        removeArtwork: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val current = preferences.trackMetadata.first()[trackId] ?: TrackMetadata(trackId)
        val oldFile = current.artworkFile
        val nextFile = when {
            removeArtwork -> {
                artworkStore.remove(oldFile)
                ""
            }
            artworkSource != null -> artworkStore.save(trackId, artworkSource) ?: oldFile
            else -> oldFile
        }
        preferences.saveTrackMetadata(
            current.copy(
                title = title.trim(),
                description = description.trim(),
                artworkFile = nextFile
            )
        )
    }

    suspend fun hideTrack(trackId: Long) = preferences.hideTrack(trackId.toString())

    suspend fun restoreHiddenTracks() = preferences.restoreHiddenTracks()
}
