package com.shiyin.player.data.model

/**
 * 04 下载的错误分类。用枚举而非裸字符串：文案集中在 strings.xml，
 * ViewModel 不持有 Context、不硬编码中文；detail 只承载 HTTP 码 / IOException 原文等技术信息。
 */
enum class DownloadError {
    INVALID_URL,
    NO_NETWORK,
    REQUEST_FAILED,
    HTTP_ERROR,
    UNSUPPORTED_MEDIA,
    FILE_TOO_LARGE,
    EMPTY_FILE,
    WRITE_FAILED,
    IMPORT_FAILED,
    EMPTY_QUERY,
    SEARCH_FAILED,
    SEARCH_EMPTY,
    PREVIEW_FAILED
}

/**
 * 04 下载页状态：两个模式（搜歌 / 直链）共用一条状态流，下载 Job 全局互斥。
 *
 * [progress] 为 0..99 的确定进度，-1 表示响应未给出 Content-Length。
 * [exportState] 复用 05/06 的统一结果态，用于直链模式；
 * [savedName] 用于搜歌模式逐条下载后的成功提示。
 */
data class DownloadUiState(
    // ---- 直链模式 ----
    val url: String = "",
    val isDownloading: Boolean = false,
    val progress: Int = -1,
    val exportState: ExportState = ExportState.Idle,
    val error: DownloadError? = null,
    val errorDetail: String? = null,

    // ---- 页面模式：0 = 搜歌，1 = 直链 ----
    val mode: Int = 0,

    // ---- 搜歌模式 ----
    val query: String = "",
    val results: List<SongResult> = emptyList(),
    val page: Int = 1,
    val hasSearched: Boolean = false,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val searchError: DownloadError? = null,
    val searchErrorDetail: String? = null,

    // ---- 试听（独立于曲库播放会话）----
    val previewId: String? = null,
    val previewTitle: String = "",
    val isPreviewPlaying: Boolean = false,
    val previewPositionMs: Long = 0L,
    val previewDurationMs: Long = 0L,

    // ---- 搜索结果逐条下载 ----
    val downloadingId: String? = null,
    val downloadProgress: Int = -1,
    val savedName: String? = null
)
