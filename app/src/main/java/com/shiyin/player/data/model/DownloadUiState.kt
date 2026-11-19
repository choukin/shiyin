package com.shiyin.player.data.model

/**
 * 04 链接下载的错误分类。用枚举而非裸字符串：文案集中在 strings.xml，
 * ViewModel 不持有 Context、不硬编码中文；[detail] 只承载 HTTP 码 / IOException 原文等技术信息。
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
    IMPORT_FAILED
}

/**
 * 04 链接下载页状态。
 * [progress] 为 0..99 的确定进度，-1 表示响应未给出 Content-Length（不确定进度）。
 * [exportState] 复用 05/06 的统一结果态，Success 携带实际入库文件名。
 */
data class DownloadUiState(
    val url: String = "",
    val isDownloading: Boolean = false,
    val progress: Int = -1,
    val exportState: ExportState = ExportState.Idle,
    val error: DownloadError? = null,
    val errorDetail: String? = null
)
