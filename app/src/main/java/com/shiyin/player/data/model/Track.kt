package com.shiyin.player.data.model

import android.net.Uri

/**
 * 一首本机音频。曲库不落库：列表直接由 MediaStore 查询得到，
 * 本模型只在内存中承载，不写入 Room / 文件。
 */
data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val dateAddedSec: Long,
    val displayName: String
)
