package com.shiyin.player.data.media

import com.shiyin.player.data.model.MusicSource
import com.shiyin.player.data.model.SongResult
import com.shiyin.player.data.util.SearchLink
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/** 搜索结果：错误只带技术信息，文案统一由 ViewModel / strings.xml 决定。 */
sealed interface SearchOutcome {
    data class Success(val songs: List<SongResult>, val hasMore: Boolean) : SearchOutcome
    data object Empty : SearchOutcome
    data class Failure(val detail: String) : SearchOutcome
}

/**
 * 04 下载页「搜歌」的数据源。
 *
 * 请求方式（2026-11-20 抓站 + curl 实测）：
 * `POST https://www.myfreemp3.com.cn/`，form `input / filter=name / page / type`，
 * 需带 Origin 与 X-Requested-With，否则站点会返回整页 HTML 而不是 JSON。
 * 成功响应 `{code:200,error:"",data:{list:[...],more,total}}`；
 * 空关键词 / 无匹配 / 触发风控时**返回 HTML**，故解析前先判首字符，不能无条件当 JSON。
 *
 * 解析只用 Android 内置的 org.json，不新增第三方 JSON 库。
 * 结果里的播放地址统一经 [SearchLink.forceHttps] 规范化，否则 CDN 会 403。
 */
class MusicSearchRepository {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun search(
        query: String,
        page: Int,
        source: MusicSource = MusicSource.NETEASE
    ): SearchOutcome = withContext(Dispatchers.IO) {
        val fields = SearchLink.formFields(query, page, source)
        if (fields.getValue("input").isEmpty()) return@withContext SearchOutcome.Empty

        val form = FormBody.Builder().apply {
            fields.forEach { (key, value) -> add(key, value) }
        }.build()
        val request = Request.Builder()
            .url(SearchLink.SEARCH_URL)
            .header("User-Agent", SearchLink.USER_AGENT)
            .header("Origin", "https://www.myfreemp3.com.cn")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Accept", "*/*")
            .post(form)
            .build()

        return@withContext try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use SearchOutcome.Failure("HTTP ${response.code}")
                }
                parse(response.body.string(), source)
            }
        } catch (io: IOException) {
            SearchOutcome.Failure(io.message.orEmpty())
        } catch (error: Throwable) {
            SearchOutcome.Failure(error.message.orEmpty())
        }
    }

    /** 纯解析逻辑，独立出来便于定位「返回 HTML / code 非 200 / data 非对象」三类异常。 */
    internal fun parse(raw: String, source: MusicSource): SearchOutcome {
        val body = raw.trim()
        // 站点在空结果、风控、参数异常时直接吐 HTML 首页。
        if (!body.startsWith("{")) return SearchOutcome.Empty
        return try {
            val root = JSONObject(body)
            val code = root.optInt("code", 0)
            if (code != 200) {
                val message = root.optString("error").ifBlank { "code=$code" }
                return SearchOutcome.Failure(message)
            }
            val data = root.optJSONObject("data") ?: return SearchOutcome.Empty
            val array = data.optJSONArray("list") ?: return SearchOutcome.Empty
            val songs = ArrayList<SongResult>(array.length())
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val rawUrl = firstNonEmpty(
                    item.optString("url"),
                    item.optString("url_320"),
                    item.optString("url_128")
                )
                val playUrl = SearchLink.forceHttps(rawUrl)
                if (!SearchLink.isPlayable(playUrl)) continue
                val songId = firstNonEmpty(
                    item.optString("songid"),
                    item.optString("id"),
                    playUrl
                )
                val title = item.optString("title").ifBlank { songId }
                val artist = item.optString("author").ifBlank { "未知歌手" }
                songs += SongResult(
                    id = "${source.param}:$songId",
                    title = title,
                    artist = artist,
                    playUrl = playUrl,
                    coverUrl = item.optString("pic").takeIf { it.isNotBlank() },
                    source = source
                )
            }
            if (songs.isEmpty()) {
                SearchOutcome.Empty
            } else {
                // 站点每页固定 10 条；满页才允许继续翻页。
                SearchOutcome.Success(songs, hasMore = songs.size >= PAGE_SIZE)
            }
        } catch (error: Throwable) {
            SearchOutcome.Failure(error.message.orEmpty())
        }
    }

    private fun firstNonEmpty(vararg values: String): String =
        values.firstOrNull { it.isNotBlank() }.orEmpty().trim()

    private companion object {
        const val PAGE_SIZE = 10
    }
}
