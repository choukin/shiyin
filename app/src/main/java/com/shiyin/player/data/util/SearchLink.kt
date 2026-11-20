package com.shiyin.player.data.util

import com.shiyin.player.data.model.MusicSource
import java.util.Locale

/**
 * 04 下载页搜索链路的纯 JVM 逻辑：请求参数、播放直链规范化、入库文件名。
 *
 * 刻意不依赖 Android / OkHttp / org.json，便于 JVM 单测覆盖。
 *
 * 关键约束（2026-11-20 实测）：搜索接口返回的播放地址是
 * `http://music.163.com/song/media/outer/url?id=xxx.mp3`，直接请求会 302 到
 * 126.net CDN 后拿到 403 Forbidden；把协议换成 https 后返回 200 audio/mpeg，
 * 且支持 Range（206）。故所有播放/下载 URL 都必须经 [forceHttps]。
 */
object SearchLink {

    const val SEARCH_URL = "https://www.myfreemp3.com.cn/"
    const val USER_AGENT =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"

    private const val MAX_NAME_LENGTH = 120
    private const val FALLBACK_NAME = "拾音下载"
    private val ILLEGAL_CHARS = Regex("[/\\\\:*?\"<>|\\p{Cntrl}]")

    /** 搜索表单字段；字段名与站点 main22.js 中的 `&filter=name&page=` 一致。 */
    fun formFields(query: String, page: Int, source: MusicSource): Map<String, String> = mapOf(
        "input" to query.trim(),
        "filter" to "name",
        "page" to page.coerceAtLeast(1).toString(),
        "type" to source.param
    )

    /**
     * http → https。只改协议前缀，不触碰路径与查询串（id 参数决定曲目）。
     * 非 http(s) 或本就 https 的原样返回，避免误改 data: 之类的字段。
     */
    fun forceHttps(url: String): String {
        val value = url.trim()
        return when {
            value.startsWith("http://", ignoreCase = true) -> "https://" + value.substring(7)
            else -> value
        }
    }

    /** 是否可作为播放/下载地址：必须是规范化后的 http(s) 链接。 */
    fun isPlayable(url: String): Boolean {
        val value = url.trim().lowercase(Locale.ROOT)
        return value.startsWith("https://") || value.startsWith("http://")
    }

    /**
     * 下载入库显示名：「歌名 - 歌手.mp3」。
     * 清洗文件名非法字符、去掉首尾句点、限制长度；歌名为空时回落固定名。
     */
    fun displayName(title: String, artist: String): String {
        val safeTitle = clean(title)
        val safeArtist = clean(artist)
        val base = when {
            safeTitle.isEmpty() -> FALLBACK_NAME
            safeArtist.isEmpty() || safeArtist == safeTitle -> safeTitle
            else -> "$safeTitle - $safeArtist"
        }
        val clipped = base.take(MAX_NAME_LENGTH - 4).trim().trim('.')
        return (clipped.ifEmpty { FALLBACK_NAME }) + ".mp3"
    }

    private fun clean(value: String): String =
        ILLEGAL_CHARS.replace(value.trim(), "_").trim().trim('.')
}
