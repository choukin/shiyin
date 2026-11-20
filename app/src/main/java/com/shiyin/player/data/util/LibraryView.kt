package com.shiyin.player.data.util

import com.shiyin.player.data.model.LibraryGroupKind
import com.shiyin.player.data.model.LibraryUiState
import com.shiyin.player.data.model.Track

/** 曲库首页当前应渲染的形态：曲目列表（全部 / 收藏 / 二级详情）或分组列表。 */
enum class LibraryViewMode { TRACKS, GROUPS }

data class LibraryView(
    val mode: LibraryViewMode,
    val tracks: List<Track>,
    val groups: List<LibraryGroup>,
    val kind: LibraryGroupKind? = null,
    val isDetail: Boolean = false
)

/**
 * 纯派生：把「原始状态」翻译成「这一屏该显示什么」。
 * 不持有 Context / 资源，文案本地化交给 Composable；本函数可单测。
 */
fun deriveLibraryView(state: LibraryUiState): LibraryView {
    val sources = state.tracks.map {
        GroupSource(it.id, it.artist, it.album, it.albumId)
    }

    val detail = state.detail
    if (detail != null) {
        val group = LibraryGrouping.group(sources, detail.kind)
            .firstOrNull { it.key == detail.key }
        val ids = group?.trackIds?.toSet().orEmpty()
        return LibraryView(
            mode = LibraryViewMode.TRACKS,
            tracks = state.tracks.filter { it.id in ids },
            groups = emptyList(),
            kind = detail.kind,
            isDetail = true
        )
    }

    return when (state.selectedTab) {
        1 -> {
            val favoriteIds = LibraryGrouping.parseFavoriteIds(state.favorites)
            LibraryView(
                mode = LibraryViewMode.TRACKS,
                tracks = state.tracks.filter { it.id in favoriteIds },
                groups = emptyList()
            )
        }
        2 -> LibraryView(
            mode = LibraryViewMode.GROUPS,
            tracks = emptyList(),
            groups = LibraryGrouping.group(sources, LibraryGroupKind.ALBUM),
            kind = LibraryGroupKind.ALBUM
        )
        3 -> LibraryView(
            mode = LibraryViewMode.GROUPS,
            tracks = emptyList(),
            groups = LibraryGrouping.group(sources, LibraryGroupKind.ARTIST),
            kind = LibraryGroupKind.ARTIST
        )
        else -> LibraryView(
            mode = LibraryViewMode.TRACKS,
            tracks = state.tracks,
            groups = emptyList()
        )
    }
}
