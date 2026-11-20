package com.shiyin.player.data.model

/**
 * 04 下载页「搜歌」的搜索源，对应站点表单里的 type 字段。
 *
 * 2026-11-20 实测：NETEASE 返回 10 条/页且带可用播放直链；
 * KUAKE 返回空列表；WS 返回 code 403「目前还不支持这个网站」。
 * 因此 UI 只暴露 NETEASE，其余保留为枚举便于接口恢复后一行接入。
 */
enum class MusicSource(val param: String) {
    NETEASE("netease"),
    KUAKE("kuake"),
    WS("ws")
}

/**
 * 一条搜索结果。[playUrl] 已由 SearchLink.forceHttps 规范化，
 * 搜索接口原始返回的是 http:// 外链，直连会被 CDN 判 403。
 */
data class SongResult(
    val id: String,
    val title: String,
    val artist: String,
    val playUrl: String,
    val coverUrl: String? = null,
    val source: MusicSource = MusicSource.NETEASE
)
