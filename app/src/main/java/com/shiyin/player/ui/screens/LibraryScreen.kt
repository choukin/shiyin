package com.shiyin.player.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shiyin.player.R
import com.shiyin.player.data.media.MediaDeleteRepository.DeleteMode
import com.shiyin.player.data.model.LibraryGroupKind
import com.shiyin.player.data.model.LibraryUiState
import com.shiyin.player.data.model.PermissionState
import com.shiyin.player.data.model.Track
import com.shiyin.player.data.util.LibraryGrouping
import com.shiyin.player.data.util.LibraryViewMode
import com.shiyin.player.data.util.deriveLibraryView
import com.shiyin.player.ui.components.CapsuleTabBar
import com.shiyin.player.ui.components.EmptyState
import com.shiyin.player.ui.components.Glyph
import com.shiyin.player.ui.components.LibraryGroupCard
import com.shiyin.player.ui.components.MiniPlayerBar
import com.shiyin.player.ui.components.PermissionRationale
import com.shiyin.player.ui.components.QuickTool
import com.shiyin.player.ui.components.QuickToolsRow
import com.shiyin.player.ui.components.SectionHeader
import com.shiyin.player.ui.components.ShiyinGlyph
import com.shiyin.player.ui.components.ShiyinScaffold
import com.shiyin.player.ui.components.TrackListItem
import com.shiyin.player.ui.navigation.Routes
import com.shiyin.player.ui.theme.ProvisionalDimens
import com.shiyin.player.ui.theme.ShiyinTheme

/**
 * 02 曲库首页：四个 Tab 均为真实视图，不是占位。
 * - 全部：最近添加的全量曲目；
 * - 收藏：按 DataStore 收藏 id 过滤，星标可就地切换；
 * - 专辑 / 歌手：按规范化后的元数据分组，点击进入二级详情；
 * - 二级详情：顶栏显示分组名 + 返回按钮，系统返回键由 BackHandler 接管。
 *
 * 分组/过滤统一由 [deriveLibraryView] 纯函数派生，本层只负责渲染与本地化文案。
 * 无权限时整页替换为权限说明态。
 */
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onTabSelected: (Int) -> Unit,
    onOpenGroup: (LibraryGroupKind, String) -> Unit,
    onCloseGroup: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onScanClick: () -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenDesignNotes: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onDeleteRequest: (Track, DeleteMode) -> Unit,
    onOpenSettings: () -> Unit,
    onPlayerBarClick: () -> Unit,
    onPlayerToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    currentTrack: Track? = null,
    resolveArtwork: suspend (Long, Uri) -> ImageBitmap? = { _, _ -> null }
) {
    val view = remember(state) { deriveLibraryView(state) }
    val favoriteIds = remember(state.favorites) {
        LibraryGrouping.parseFavoriteIds(state.favorites)
    }
    val detail = state.detail

    val unknownAlbum = stringResource(R.string.library_unknown_album)
    val unknownArtist = stringResource(R.string.library_unknown_artist)
    val title = when {
        detail == null -> stringResource(R.string.library_title)
        detail.key.isEmpty() -> if (detail.kind == LibraryGroupKind.ALBUM) unknownAlbum else unknownArtist
        else -> detail.key
    }

    // 二级页由内部视图状态承载，系统返回键优先关闭它而不是退出 App。
    BackHandler(enabled = detail != null) { onCloseGroup() }

    val tabs = listOf(
        stringResource(R.string.library_tab_all),
        stringResource(R.string.library_tab_favorites),
        stringResource(R.string.library_tab_albums),
        stringResource(R.string.library_tab_artists)
    )
    val tools = listOf(
        QuickTool(stringResource(R.string.scan_local), Routes.SCAN, Glyph.Scan),
        QuickTool(stringResource(R.string.playlist_title), Routes.PLAYLIST, Glyph.Playlist),
        QuickTool(stringResource(R.string.link_download), Routes.LINK_DOWNLOAD, Glyph.Link),
        QuickTool(stringResource(R.string.music_trim), Routes.TRIM, Glyph.Trim),
        QuickTool(stringResource(R.string.extract_audio), Routes.EXTRACT, Glyph.Extract)
    )

    ShiyinScaffold(
        title = title,
        modifier = modifier,
        onBack = if (detail != null) onCloseGroup else null,
        actions = {
            if (detail == null) {
                IconButton(onClick = onOpenDesignNotes) {
                    ShiyinGlyph(
                        glyph = Glyph.Info,
                        modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                    )
                }
            }
        },
        bottomBar = {
            MiniPlayerBar(
                track = currentTrack,
                isPlaying = isPlaying,
                onToggle = onPlayerToggle,
                onClick = onPlayerBarClick
            )
        }
    ) { padding ->
        if (state.permission != PermissionState.Granted) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(
                        horizontal = ProvisionalDimens.ScreenHorizontalPadding,
                        vertical = ProvisionalDimens.ContentVerticalPadding
                    ),
                verticalArrangement = Arrangement.Center
            ) {
                PermissionRationale(
                    title = stringResource(R.string.permission_audio_title),
                    body = stringResource(R.string.permission_audio_body),
                    primaryLabel = if (state.permission == PermissionState.PermanentlyDenied) {
                        stringResource(R.string.permission_open_settings)
                    } else {
                        stringResource(R.string.scan_local)
                    },
                    onPrimary = if (state.permission == PermissionState.PermanentlyDenied) {
                        onOpenSettings
                    } else {
                        onScanClick
                    },
                    secondaryLabel = stringResource(R.string.permission_open_settings),
                    onSecondary = onOpenSettings
                )
            }
            return@ShiyinScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = ProvisionalDimens.ScreenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(ProvisionalDimens.SectionVerticalSpacing)
        ) {
            if (detail == null) {
                item { CapsuleTabBar(tabs = tabs, selectedIndex = state.selectedTab, onSelected = onTabSelected) }

                item {
                    Column {
                        SectionHeader(title = stringResource(R.string.quick_tools))
                        Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                        QuickToolsRow(tools = tools, onToolClick = onOpenTool)
                    }
                }
            }

            item { SectionHeader(title = sectionTitle(state, view.tracks.size)) }

            when (view.mode) {
                LibraryViewMode.GROUPS -> {
                    val kind = view.kind ?: LibraryGroupKind.ARTIST
                    if (view.groups.isEmpty() && !state.isLoading) {
                        item {
                            EmptyState(
                                title = stringResource(
                                    if (kind == LibraryGroupKind.ALBUM) {
                                        R.string.library_albums_empty_title
                                    } else {
                                        R.string.library_artists_empty_title
                                    }
                                ),
                                body = stringResource(
                                    if (kind == LibraryGroupKind.ALBUM) {
                                        R.string.library_albums_empty_body
                                    } else {
                                        R.string.library_artists_empty_body
                                    }
                                )
                            )
                        }
                    } else {
                        items(view.groups, key = { "$kind:${it.key}" }) { group ->
                            val artwork by produceState<ImageBitmap?>(
                                initialValue = null,
                                key1 = group.representativeId
                            ) {
                                val representative = state.tracks.firstOrNull {
                                    it.id == group.representativeId
                                }
                                value = if (representative != null) {
                                    resolveArtwork(group.albumId, representative.uri)
                                } else {
                                    null
                                }
                            }
                            LibraryGroupCard(
                                name = group.key.ifEmpty {
                                    if (kind == LibraryGroupKind.ALBUM) unknownAlbum else unknownArtist
                                },
                                countLabel = stringResource(
                                    R.string.library_group_track_count,
                                    group.trackCount
                                ),
                                artwork = artwork,
                                seed = group.representativeId,
                                onClick = { onOpenGroup(kind, group.key) }
                            )
                        }
                    }
                }

                LibraryViewMode.TRACKS -> {
                    if (view.tracks.isEmpty() && !state.isLoading) {
                        item { EmptyStateFor(state, onScanClick, onTabSelected) }
                    } else {
                        items(view.tracks, key = { it.id }) { track ->
                            val artwork by produceState<ImageBitmap?>(
                                initialValue = null,
                                key1 = track.id
                            ) {
                                value = resolveArtwork(track.albumId, track.uri)
                            }
                            TrackListItem(
                                track = track,
                                artwork = artwork,
                                isFavorite = track.id in favoriteIds,
                                onToggleFavorite = { onToggleFavorite(track.id.toString()) },
                                onClick = { onTrackClick(track, view.tracks) },
                                onDeleteRequest = { mode -> onDeleteRequest(track, mode) }
                            )
                        }
                    }
                }
            }

            if (detail == null) {
                item {
                    Text(
                        text = stringResource(R.string.library_design_notes_action),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** 章节标题：二级页显示曲目总数，其余按当前 Tab 命名。 */
@Composable
private fun sectionTitle(state: LibraryUiState, trackCount: Int): String {
    val detail = state.detail
    if (detail != null) {
        return stringResource(R.string.library_detail_track_count, trackCount)
    }
    return stringResource(
        when (state.selectedTab) {
            1 -> R.string.library_section_favorites
            2 -> R.string.library_section_albums
            3 -> R.string.library_section_artists
            else -> R.string.recently_added
        }
    )
}

/** 曲目列表空态：全部 / 收藏 / 二级详情各自的引导不同。 */
@Composable
private fun EmptyStateFor(
    state: LibraryUiState,
    onScanClick: () -> Unit,
    onTabSelected: (Int) -> Unit
) {
    when {
        state.detail != null -> EmptyState(
            title = stringResource(R.string.library_detail_empty_title),
            body = stringResource(R.string.library_detail_empty_body)
        )
        state.selectedTab == 1 -> EmptyState(
            title = stringResource(R.string.library_favorites_empty_title),
            body = stringResource(R.string.library_favorites_empty_body),
            actionLabel = stringResource(R.string.library_view_all_action),
            onAction = { onTabSelected(0) }
        )
        else -> EmptyState(
            title = stringResource(R.string.library_empty_title),
            body = stringResource(R.string.library_empty_body),
            actionLabel = stringResource(R.string.library_empty_action),
            onAction = onScanClick
        )
    }
}

private val PreviewTracks = listOf(
    Track(1L, Uri.EMPTY, "示例曲目一", "拾音", "本机音频", 1L, 204_000L, 0L, "demo1.mp3"),
    Track(2L, Uri.EMPTY, "示例曲目二", "未知艺术家", "本机音频", 2L, 187_000L, 0L, "demo2.mp3")
)

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryScreenPreview() {
    ShiyinTheme {
        LibraryScreen(
            state = LibraryUiState(
                isLoading = false,
                permission = PermissionState.Granted,
                tracks = PreviewTracks
            ),
            onTabSelected = {},
            onOpenGroup = { _, _ -> },
            onCloseGroup = {},
            onToggleFavorite = {},
            onScanClick = {},
            onOpenTool = {},
            onOpenDesignNotes = {},
            onTrackClick = { _, _ -> },
            onDeleteRequest = { _, _ -> },
            onOpenSettings = {},
            onPlayerBarClick = {},
            onPlayerToggle = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun LibraryScreenEmptyPreview() {
    ShiyinTheme {
        LibraryScreen(
            state = LibraryUiState(
                isLoading = false,
                permission = PermissionState.Granted
            ),
            onTabSelected = {},
            onOpenGroup = { _, _ -> },
            onCloseGroup = {},
            onToggleFavorite = {},
            onScanClick = {},
            onOpenTool = {},
            onOpenDesignNotes = {},
            onTrackClick = { _, _ -> },
            onDeleteRequest = { _, _ -> },
            onOpenSettings = {},
            onPlayerBarClick = {},
            onPlayerToggle = {}
        )
    }
}
