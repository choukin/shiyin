package com.shiyin.player.ui.screens

import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.shiyin.player.R
import com.shiyin.player.data.model.LibraryUiState
import com.shiyin.player.data.model.PermissionState
import com.shiyin.player.data.model.Track
import com.shiyin.player.ui.components.AdBannerPlaceholder
import com.shiyin.player.ui.components.CapsuleTabBar
import com.shiyin.player.ui.components.EmptyState
import com.shiyin.player.ui.components.Glyph
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
 * 02 曲库首页。
 * 广告条严格位于「快捷工具」与「最近添加」之间；关闭态由 VM 持久化到 DataStore。
 * 无权限时整页替换为权限说明态。
 */
@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onTabSelected: (Int) -> Unit,
    onScanClick: () -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenDesignNotes: () -> Unit,
    onTrackClick: (Track) -> Unit,
    onDismissAd: () -> Unit,
    onOpenSettings: () -> Unit,
    onPlayerBarClick: () -> Unit,
    onPlayerToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    currentTrack: Track? = null,
    resolveArtwork: suspend (Track) -> ImageBitmap? = { null }
) {
    val tabs = listOf(
        stringResource(R.string.library_tab_all),
        stringResource(R.string.library_tab_favorites),
        stringResource(R.string.library_tab_albums),
        stringResource(R.string.library_tab_artists)
    )
    val tools = listOf(
        QuickTool(stringResource(R.string.scan_local), Routes.SCAN, Glyph.Scan),
        QuickTool(stringResource(R.string.link_download), Routes.LINK_DOWNLOAD, Glyph.Link),
        QuickTool(stringResource(R.string.music_trim), Routes.TRIM, Glyph.Trim),
        QuickTool(stringResource(R.string.extract_audio), Routes.EXTRACT, Glyph.Extract)
    )

    ShiyinScaffold(
        title = stringResource(R.string.library_title),
        modifier = modifier,
        actions = {
            IconButton(onClick = onOpenDesignNotes) {
                ShiyinGlyph(
                    glyph = Glyph.Info,
                    modifier = Modifier.size(ProvisionalDimens.IconSizeMedium)
                )
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
            item { CapsuleTabBar(tabs = tabs, selectedIndex = state.selectedTab, onSelected = onTabSelected) }

            item {
                Column {
                    SectionHeader(title = stringResource(R.string.quick_tools))
                    Spacer(modifier = Modifier.height(ProvisionalDimens.ItemVerticalSpacing))
                    QuickToolsRow(tools = tools, onToolClick = onOpenTool)
                }
            }

            // 广告位：严格在「快捷工具」之后、「最近添加」之前
            if (!state.adDismissed) {
                item { AdBannerPlaceholder(onDismiss = onDismissAd) }
            }

            item { SectionHeader(title = stringResource(R.string.recently_added)) }

            if (state.tracks.isEmpty() && !state.isLoading) {
                item {
                    EmptyState(
                        title = stringResource(R.string.library_empty_title),
                        body = stringResource(R.string.library_empty_body),
                        actionLabel = stringResource(R.string.library_empty_action),
                        onAction = onScanClick
                    )
                }
            } else {
                items(state.tracks, key = { it.id }) { track ->
                    val artwork by produceState<ImageBitmap?>(
                        initialValue = null,
                        key1 = track.id
                    ) {
                        value = resolveArtwork(track)
                    }
                    TrackListItem(
                        track = track,
                        artwork = artwork,
                        onClick = { onTrackClick(track) }
                    )
                }
            }

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
                adDismissed = false,
                tracks = PreviewTracks
            ),
            onTabSelected = {},
            onScanClick = {},
            onOpenTool = {},
            onOpenDesignNotes = {},
            onTrackClick = {},
            onDismissAd = {},
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
                permission = PermissionState.Granted,
                adDismissed = true
            ),
            onTabSelected = {},
            onScanClick = {},
            onOpenTool = {},
            onOpenDesignNotes = {},
            onTrackClick = {},
            onDismissAd = {},
            onOpenSettings = {},
            onPlayerBarClick = {},
            onPlayerToggle = {}
        )
    }
}
