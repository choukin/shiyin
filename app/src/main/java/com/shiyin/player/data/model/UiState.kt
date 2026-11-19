package com.shiyin.player.data.model

/** 音频权限的四种状态。永久拒绝态不重复弹窗，改为引导去系统设置。 */
sealed interface PermissionState {
    data object Unknown : PermissionState
    data object Granted : PermissionState
    data object Denied : PermissionState
    data object PermanentlyDenied : PermissionState
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val permission: PermissionState = PermissionState.Unknown,
    val tracks: List<Track> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val adDismissed: Boolean = true,
    val selectedTab: Int = 0,
    val error: String? = null
)

data class ScanUiState(
    val permission: PermissionState = PermissionState.Unknown,
    val isScanning: Boolean = false,
    val foundCount: Int = 0,
    val finished: Boolean = false,
    val error: String? = null
)

data class PlayerUiState(
    val isConnected: Boolean = false,
    val isPlaying: Boolean = false,
    val currentTrack: Track? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList()
)
