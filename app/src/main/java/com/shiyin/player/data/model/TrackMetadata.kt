package com.shiyin.player.data.model

/**
 * 曲目信息的应用内覆盖，按 MediaStore 的 _ID 关联。
 * 只影响拾音里的显示，不改写手机里的音频文件；空字符串代表「沿用原始值」。
 *
 * @param artworkFile 应用私有目录 covers/ 下的封面文件名，空串表示未设置自定义封面。
 */
data class TrackMetadata(
    val trackId: Long,
    val title: String = "",
    val description: String = "",
    val artworkFile: String = ""
) {
    val isBlank: Boolean
        get() = title.isBlank() && description.isBlank() && artworkFile.isBlank()
}
