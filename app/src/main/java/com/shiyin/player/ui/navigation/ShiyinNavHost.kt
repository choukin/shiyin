package com.shiyin.player.ui.navigation

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
import com.shiyin.player.data.model.PermissionState
import com.shiyin.player.data.permission.Permissions
import com.shiyin.player.di.AppContainer
import com.shiyin.player.ui.screens.DesignNotesScreen
import com.shiyin.player.ui.screens.ExtractAudioScreen
import com.shiyin.player.ui.screens.LibraryScreen
import com.shiyin.player.ui.screens.LibraryViewModel
import com.shiyin.player.ui.screens.LinkDownloadScreen
import com.shiyin.player.ui.screens.MusicTrimScreen
import com.shiyin.player.ui.screens.PlayerScreen
import com.shiyin.player.ui.screens.PlayerViewModel
import com.shiyin.player.ui.screens.ScanScreen
import com.shiyin.player.ui.screens.ScanViewModel
import com.shiyin.player.ui.screens.SplashScreen

/**
 * 零广告路由白名单（docs/AD_SPEC.md）：广告组件只允许出现在 02 曲库页，
 * 导航层用 assertAdFree 统一约束，避免逐屏判断时漏掉编辑类页面。
 */
internal val AD_FREE_ROUTES = setOf(
    Routes.SPLASH,
    Routes.LINK_DOWNLOAD,
    Routes.TRIM,
    Routes.EXTRACT,
    Routes.PLAYER,
    Routes.DESIGN_NOTES
)

private fun assertAdFree(route: String) {
    check(route in AD_FREE_ROUTES) { "$route 必须是零广告页面" }
}

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
                onScanClick = { navController.navigate(Routes.SCAN) },
                onOpenTool = { route -> navController.navigate(route) },
                onOpenDesignNotes = { navController.navigate(Routes.DESIGN_NOTES) },
                onTrackClick = { track ->
                    val index = state.tracks.indexOf(track).coerceAtLeast(0)
                    container.playerController.setQueue(state.tracks, index)
                    navController.navigate(Routes.PLAYER)
                },
                onDismissAd = vm::dismissAd,
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
                resolveArtwork = { track ->
                    container.albumArtResolver.resolve(track.albumId, track.uri)
                }
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

        composable(Routes.LINK_DOWNLOAD) {
            assertAdFree(Routes.LINK_DOWNLOAD)
            LinkDownloadScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TRIM) {
            assertAdFree(Routes.TRIM)
            MusicTrimScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.EXTRACT) {
            assertAdFree(Routes.EXTRACT)
            ExtractAudioScreen(onBack = { navController.popBackStack() })
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.DESIGN_NOTES) {
            DesignNotesScreen(onBack = { navController.popBackStack() })
        }
    }
}
