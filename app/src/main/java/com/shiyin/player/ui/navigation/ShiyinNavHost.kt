package com.shiyin.player.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shiyin.player.data.media.ClipPreviewPlayer
import com.shiyin.player.data.media.MediaDeleteRepository.DeleteMode
import com.shiyin.player.data.media.SearchPreviewPlayer
import com.shiyin.player.data.model.ExtractMode
import com.shiyin.player.data.model.PermissionState
import com.shiyin.player.data.permission.Permissions
import com.shiyin.player.di.AppContainer
import com.shiyin.player.ui.screens.DesignNotesScreen
import com.shiyin.player.ui.screens.EditorViewModel
import com.shiyin.player.ui.screens.ExtractAudioScreen
import com.shiyin.player.ui.screens.LibraryScreen
import com.shiyin.player.ui.screens.LinkDownloadViewModel
import com.shiyin.player.ui.screens.LibraryViewModel
import com.shiyin.player.ui.screens.LinkDownloadScreen
import com.shiyin.player.ui.screens.MusicTrimScreen
import com.shiyin.player.ui.screens.PlaylistScreen
import com.shiyin.player.ui.screens.PlaylistViewModel
import com.shiyin.player.ui.screens.PlayerScreen
import com.shiyin.player.ui.screens.PlayerViewModel
import com.shiyin.player.ui.screens.ScanScreen
import com.shiyin.player.ui.screens.ScanViewModel
import com.shiyin.player.ui.screens.SplashScreen

private inline fun <reified VM : ViewModel> viewModelFactory(
    crossinline create: () -> VM
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}

/** 8 条路由的导航图。所有屏幕都通过 AppContainer 取数据与播放控制，无全局单例。 */
@Composable
fun ShiyinNavHost(
    container: AppContainer,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val playerState by container.playerController.state.collectAsStateWithLifecycle()
    val openSettings = { context.startActivity(Permissions.appSettingsIntent(context)) }
    val backToLibrary = {
        navController.navigate(Routes.LIBRARY) {
            popUpTo(Routes.LIBRARY) { inclusive = true }
        }
        Unit
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.LIBRARY) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LIBRARY) {
            val vm: LibraryViewModel = viewModel(
                factory = viewModelFactory<LibraryViewModel> {
                    LibraryViewModel(container.libraryRepository)
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            // 系统删除确认弹窗（MediaStore.createTrashRequest / createDeleteRequest）。
            val deleteLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult()
            ) { result ->
                // 用户在系统弹窗点「删除」后 resultCode= RESULT_OK，此时刷新曲库。
                if (result.resultCode == android.app.Activity.RESULT_OK) vm.refresh()
            }
            LaunchedEffect(Unit) {
                vm.onPermissionStateChanged(
                    if (Permissions.isGranted(context)) {
                        PermissionState.Granted
                    } else {
                        PermissionState.Unknown
                    }
                )
            }
            LibraryScreen(
                state = state,
                onTabSelected = vm::onTabSelected,
                onOpenGroup = vm::openGroup,
                onCloseGroup = vm::closeGroup,
                onToggleFavorite = vm::toggleFavorite,
                onScanClick = { navController.navigate(Routes.SCAN) },
                onOpenTool = { route -> navController.navigate(route) },
                onOpenDesignNotes = { navController.navigate(Routes.DESIGN_NOTES) },
                // 队列用「当前视图可见的列表」，收藏 / 分组二级页播放时不会串进全量曲库。
                onTrackClick = { track, queue ->
                    val index = queue.indexOf(track).coerceAtLeast(0)
                    container.playerController.setQueue(queue, index)
                    navController.navigate(Routes.PLAYER)
                },
                onDeleteRequest = { track, mode ->
                    val request = container.mediaDeleteRepository.requestDelete(listOf(track.uri), mode)
                    if (request == null) {
                        // API<30 已直接删除，这里只刷新。
                        vm.refresh()
                    } else {
                        runCatching { deleteLauncher.launch(request) }
                            .onFailure {
                                // 部分 ROM 不支持回收站请求，回退到永久删除。
                                if (mode == DeleteMode.Trash) {
                                    val fallback = container.mediaDeleteRepository.requestDelete(
                                        listOf(track.uri), DeleteMode.Permanent
                                    )
                                    fallback?.let { deleteLauncher.launch(it) }
                                }
                            }
                    }
                },
                // 编辑（名称 / 简介 / 海报）与「仅从列表移除」都写 DataStore，不改手机文件。
                onSaveEdit = vm::saveEdit,
                onRemoveFromList = { track -> vm.removeFromLibrary(track.id) },
                onRestoreHidden = vm::restoreHidden,
                canTrash = container.mediaDeleteRepository.canTrash(),
                onOpenSettings = openSettings,
                onPlayerBarClick = { navController.navigate(Routes.PLAYER) },
                onPlayerToggle = {
                    if (playerState.isPlaying) {
                        container.playerController.pause()
                    } else {
                        container.playerController.play()
                    }
                },
                isPlaying = playerState.isPlaying,
                currentTrack = playerState.currentTrack,
                resolveArtwork = container.trackArtworkResolver::resolve,
                loadArtwork = container.trackArtworkResolver::load
            )
        }

        composable(Routes.SCAN) {
            val vm: ScanViewModel = viewModel(
                factory = viewModelFactory<ScanViewModel> {
                    ScanViewModel(container.libraryRepository)
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                vm.onPermissionStateChanged(
                    if (Permissions.isGranted(context)) {
                        PermissionState.Granted
                    } else {
                        PermissionState.Unknown
                    }
                )
            }
            ScanScreen(
                state = state,
                onPermissionResult = vm::onPermissionResult,
                onStartScan = vm::scan,
                onOpenSettings = openSettings,
                onBackToLibrary = backToLibrary
            )
        }

        // 04 下载：主入口搜歌（POST 搜索 → 独立 ExoPlayer 试听 → 下载入库），
        // 次要入口保留直链下载与 SAF 本地导入。
        composable(Routes.LINK_DOWNLOAD) {
            val vm: LinkDownloadViewModel = viewModel(
                factory = viewModelFactory<LinkDownloadViewModel> {
                    LinkDownloadViewModel(
                        downloadRepository = container.linkDownloadRepository,
                        mediaEditRepository = container.mediaEditRepository,
                        searchRepository = container.musicSearchRepository,
                        previewPlayer = SearchPreviewPlayer(context.applicationContext)
                    )
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            LinkDownloadScreen(
                state = state,
                onModeChange = vm::onModeChange,
                onQueryChange = vm::onQueryChange,
                onSearch = vm::search,
                onLoadMore = vm::loadMore,
                onPreviewClick = vm::onPreviewClick,
                onStopPreview = vm::stopPreview,
                onDownloadSong = vm::downloadSong,
                onDismissSaved = vm::dismissSaved,
                onConsumeSearchError = vm::consumeSearchError,
                onUrlChange = vm::onUrlChange,
                onDownload = vm::download,
                onCancel = vm::cancel,
                onImport = vm::import,
                onReset = vm::reset,
                onConsumeError = vm::consumeError,
                onBack = { navController.popBackStack() }
            )
        }

        // 05 / 06 共用 EditorViewModel，仅 removeVideo 与默认模式不同。
        composable(Routes.TRIM) {
            val vm: EditorViewModel = viewModel(
                factory = viewModelFactory<EditorViewModel> {
                    EditorViewModel(
                        repository = container.mediaEditRepository,
                        waveformExtractor = container.waveformExtractor,
                        previewPlayer = ClipPreviewPlayer(context.applicationContext),
                        removeVideo = false,
                        defaultMode = ExtractMode.RANGE
                    )
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            MusicTrimScreen(
                state = state,
                onSourceSelected = vm::onSourceSelected,
                onRangeChange = vm::onRangeChange,
                onExportFileNameChange = vm::onExportFileNameChange,
                onExportFormatChange = vm::onExportFormatChange,
                onExport = vm::export,
                onResetExport = vm::resetExportState,
                onConsumeError = vm::consumeError,
                onTogglePreview = vm::togglePreview,
                onStopPreview = vm::stopPreview,
                onAttachVideo = vm::attachVideoView,
                onDetachVideo = vm::detachVideoView,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.EXTRACT) {
            val vm: EditorViewModel = viewModel(
                factory = viewModelFactory<EditorViewModel> {
                    EditorViewModel(
                        repository = container.mediaEditRepository,
                        waveformExtractor = container.waveformExtractor,
                        previewPlayer = ClipPreviewPlayer(context.applicationContext),
                        removeVideo = true,
                        defaultMode = ExtractMode.WHOLE
                    )
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            ExtractAudioScreen(
                state = state,
                onSourceSelected = vm::onSourceSelected,
                onRangeChange = vm::onRangeChange,
                onModeChange = vm::onModeChange,
                onExportFileNameChange = vm::onExportFileNameChange,
                onExportFormatChange = vm::onExportFormatChange,
                onExport = vm::export,
                onResetExport = vm::resetExportState,
                onConsumeError = vm::consumeError,
                onTogglePreview = vm::togglePreview,
                onStopPreview = vm::stopPreview,
                onAttachVideo = vm::attachVideoView,
                onDetachVideo = vm::detachVideoView,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PLAYER) {
            val vm: PlayerViewModel = viewModel(
                factory = viewModelFactory<PlayerViewModel> {
                    PlayerViewModel(container.playerController)
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            PlayerScreen(
                state = state,
                onToggle = vm::togglePlayPause,
                onPrevious = vm::previous,
                onNext = vm::next,
                onSeek = vm::seekTo,
                onBack = { navController.popBackStack() },
                resolveArtwork = container.trackArtworkResolver::resolve
            )
        }

        composable(Routes.PLAYLIST) {
            val vm: PlaylistViewModel = viewModel(
                factory = viewModelFactory<PlaylistViewModel> {
                    PlaylistViewModel(
                        container.playlistRepository,
                        container.libraryRepository,
                        container.playerController
                    )
                }
            )
            val state by vm.uiState.collectAsStateWithLifecycle()
            PlaylistScreen(
                state = state,
                onCreate = vm::create,
                onShowCreateDialog = vm::showCreateDialog,
                onSelect = vm::select,
                onDelete = vm::delete,
                onAddTracks = vm::addTracks,
                onRemoveTrack = vm::removeTrack,
                onPlay = { playlist ->
                    vm.play(playlist)
                    navController.navigate(Routes.PLAYER)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DESIGN_NOTES) {
            DesignNotesScreen(onBack = { navController.popBackStack() })
        }
    }
}
