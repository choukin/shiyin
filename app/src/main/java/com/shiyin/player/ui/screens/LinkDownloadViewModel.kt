package com.shiyin.player.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shiyin.player.data.media.DownloadResult
import com.shiyin.player.data.media.LinkDownloadRepository
import com.shiyin.player.data.media.MediaEditRepository
import com.shiyin.player.data.media.MusicSearchRepository
import com.shiyin.player.data.media.SearchOutcome
import com.shiyin.player.data.media.SearchPreviewPlayer
import com.shiyin.player.data.model.DownloadError
import com.shiyin.player.data.model.DownloadUiState
import com.shiyin.player.data.model.ExportState
import com.shiyin.player.data.model.SongResult
import com.shiyin.player.data.util.SearchLink
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 04 下载页：主入口为「搜歌」（搜索 → 试听 → 下载入库），
 * 次要入口保留「直链下载」与 SAF 本地导入。
 *
 * 不持有 Context，错误只用 [DownloadError] 枚举 + 技术 detail 表达，文案全部在 strings.xml。
 * 搜索 Job 与下载 Job 各自互斥；下载 Job 全局唯一，两个模式不会并发写媒体库。
 */
class LinkDownloadViewModel(
    private val downloadRepository: LinkDownloadRepository,
    private val mediaEditRepository: MediaEditRepository,
    private val searchRepository: MusicSearchRepository,
    private val previewPlayer: SearchPreviewPlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadUiState())
    val uiState: StateFlow<DownloadUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null
    private var searchJob: Job? = null

    init {
        previewPlayer.onStateChanged = { playing, positionMs, durationMs ->
            _uiState.update {
                it.copy(
                    isPreviewPlaying = playing,
                    previewPositionMs = positionMs,
                    previewDurationMs = durationMs
                )
            }
        }
        previewPlayer.onError = { detail ->
            _uiState.update {
                it.copy(
                    previewId = null,
                    previewTitle = "",
                    isPreviewPlaying = false,
                    previewPositionMs = 0L,
                    previewDurationMs = 0L,
                    searchError = DownloadError.PREVIEW_FAILED,
                    searchErrorDetail = detail
                )
            }
        }
    }

    // ---------------- 页面模式 ----------------

    fun onModeChange(index: Int) {
        if (index == _uiState.value.mode) return
        // 离开搜歌页立即停试听，避免切到直链后还在后台播放。
        if (index != MODE_SEARCH) stopPreview()
        _uiState.update { it.copy(mode = index) }
    }

    // ---------------- 搜歌 ----------------

    fun onQueryChange(value: String) {
        _uiState.update {
            it.copy(query = value, searchError = null, searchErrorDetail = null)
        }
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) {
            _uiState.update {
                it.copy(searchError = DownloadError.EMPTY_QUERY, searchErrorDetail = null)
            }
            return
        }
        runSearch(query, page = 1, append = false)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isSearching || state.isLoadingMore || !state.hasMore) return
        runSearch(state.query.trim(), page = state.page + 1, append = true)
    }

    private fun runSearch(query: String, page: Int, append: Boolean) {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                query = query,
                page = page,
                hasSearched = true,
                isSearching = !append,
                isLoadingMore = append,
                searchError = null,
                searchErrorDetail = null,
                results = if (append) it.results else emptyList(),
                hasMore = false
            )
        }
        searchJob = viewModelScope.launch {
            when (val outcome = searchRepository.search(query, page)) {
                is SearchOutcome.Success -> _uiState.update { current ->
                    current.copy(
                        isSearching = false,
                        isLoadingMore = false,
                        results = (current.results + outcome.songs).distinctBy { it.id },
                        hasMore = outcome.hasMore
                    )
                }
                SearchOutcome.Empty -> _uiState.update { current ->
                    current.copy(
                        isSearching = false,
                        isLoadingMore = false,
                        hasMore = false,
                        results = if (append) current.results else emptyList(),
                        searchError = if (append) null else DownloadError.SEARCH_EMPTY
                    )
                }
                is SearchOutcome.Failure -> _uiState.update { current ->
                    current.copy(
                        isSearching = false,
                        isLoadingMore = false,
                        hasMore = false,
                        results = if (append) current.results else emptyList(),
                        searchError = DownloadError.SEARCH_FAILED,
                        searchErrorDetail = outcome.detail
                    )
                }
            }
        }
    }

    // ---------------- 试听 ----------------

    fun onPreviewClick(song: SongResult) {
        val current = _uiState.value
        if (current.previewId == song.id) {
            if (current.isPreviewPlaying) previewPlayer.pause() else previewPlayer.play(song.id, song.playUrl)
        } else {
            previewPlayer.play(song.id, song.playUrl)
            _uiState.update {
                it.copy(
                    previewId = song.id,
                    previewTitle = song.title,
                    isPreviewPlaying = true,
                    previewPositionMs = 0L,
                    previewDurationMs = 0L,
                    searchError = null,
                    searchErrorDetail = null
                )
            }
        }
    }

    fun stopPreview() {
        previewPlayer.stop()
        _uiState.update {
            it.copy(
                previewId = null,
                previewTitle = "",
                isPreviewPlaying = false,
                previewPositionMs = 0L,
                previewDurationMs = 0L
            )
        }
    }

    // ---------------- 搜索结果下载 ----------------

    fun downloadSong(song: SongResult) {
        if (isBusy()) return
        _uiState.update {
            it.copy(
                downloadingId = song.id,
                downloadProgress = -1,
                savedName = null,
                searchError = null,
                searchErrorDetail = null
            )
        }
        downloadJob = viewModelScope.launch {
            val result = downloadRepository.download(
                rawUrl = song.playUrl,
                onProgress = { progress ->
                    _uiState.update { it.copy(downloadProgress = progress) }
                },
                displayName = SearchLink.displayName(song.title, song.artist)
            )
            _uiState.update { current ->
                when (result) {
                    // 只用 savedName 在搜歌区提示，不写 exportState，避免切到「直链」Tab 时串出同一条结果。
                    is DownloadResult.Success -> current.copy(
                        downloadingId = null,
                        downloadProgress = 100,
                        savedName = result.displayName
                    )
                    is DownloadResult.Failure -> current.copy(
                        downloadingId = null,
                        downloadProgress = -1,
                        searchError = result.error,
                        searchErrorDetail = result.detail
                    )
                }
            }
        }
    }

    fun dismissSaved() {
        _uiState.update { it.copy(savedName = null) }
    }

    fun consumeSearchError() {
        _uiState.update { it.copy(searchError = null, searchErrorDetail = null) }
    }

    // ---------------- 直链下载（原有能力） ----------------

    fun onUrlChange(value: String) {
        _uiState.update { it.copy(url = value, error = null, errorDetail = null) }
    }

    fun download() {
        if (isBusy()) return
        val url = _uiState.value.url
        _uiState.update {
            it.copy(
                isDownloading = true,
                progress = -1,
                exportState = ExportState.Idle,
                error = null,
                errorDetail = null
            )
        }
        downloadJob = viewModelScope.launch {
            val result = downloadRepository.download(
                rawUrl = url,
                onProgress = { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }
            )
            _uiState.update { current ->
                when (result) {
                    is DownloadResult.Success -> current.copy(
                        isDownloading = false,
                        progress = 100,
                        exportState = ExportState.Success(result.displayName)
                    )
                    is DownloadResult.Failure -> current.copy(
                        isDownloading = false,
                        progress = -1,
                        error = result.error,
                        errorDetail = result.detail
                    )
                }
            }
        }
    }

    fun cancel() {
        downloadJob?.cancel()
        downloadJob = null
        _uiState.update {
            it.copy(
                isDownloading = false,
                downloadingId = null,
                progress = -1,
                downloadProgress = -1
            )
        }
    }

    /** 次要入口：把 SAF 选中的本机音频原样导入音乐库。 */
    fun import(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(exportState = ExportState.Idle, error = null, errorDetail = null)
            }
            mediaEditRepository.importAudio(uri)
                .onSuccess { name ->
                    _uiState.update { it.copy(exportState = ExportState.Success(name)) }
                }
                .onFailure {
                    _uiState.update { it.copy(error = DownloadError.IMPORT_FAILED) }
                }
        }
    }

    fun reset() {
        _uiState.update {
            it.copy(
                exportState = ExportState.Idle,
                error = null,
                errorDetail = null,
                progress = -1,
                savedName = null
            )
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null, errorDetail = null) }
    }

    private fun isBusy(): Boolean {
        val state = _uiState.value
        return state.isDownloading || state.downloadingId != null
    }

    override fun onCleared() {
        downloadJob?.cancel()
        searchJob?.cancel()
        previewPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val MODE_SEARCH = 0
    }
}
