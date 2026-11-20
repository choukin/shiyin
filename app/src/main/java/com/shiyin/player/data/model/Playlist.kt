package com.shiyin.player.data.model

/**
 * 歌单：用户自建的曲目集合，仅存 Track.id 列表（Track 本体仍来自 MediaStore 实时查询）。
 * 这样删除源文件后歌单不会残留无效路径，重新扫描即可自动对齐。
 */
data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<Long>,
    val createdAt: Long
)
