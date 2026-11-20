package com.shiyin.player.data.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.shiyin.player.data.model.DownloadError
import com.shiyin.player.data.util.DownloadLink
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.io.DEFAULT_BUFFER_SIZE
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** 下载结果：错误用枚举归类，[detail] 只放 HTTP 码 / IOException 原文等技术信息。 */
sealed interface DownloadResult {
    data class Success(val displayName: String) : DownloadResult
    data class Failure(val error: DownloadError, val detail: String? = null) : DownloadResult
}

/**
 * 04 链接下载：对用户显式粘贴的 http(s) 音视频**直链**发起 GET，
 * 流式写入缓存文件 → 校验类型与大小 → 经 [MediaEditRepository.publishFile] 写入音乐库。
 *
 * 边界：
 * - 不解析第三方平台页面、不嗅探页面里的媒体地址、不上传任何内容。
 * - 只信任 Content-Type / 扩展名判定为音频或视频的响应，HTML 错误页一律拒绝入库。
 * - 全程在 Dispatchers.IO，循环内 [ensureActive] 支持取消；失败会删除缓存与
 *   MediaStore 中的 pending 记录，不留下半成品。
 */
class LinkDownloadRepository(
    private val context: Context,
    private val mediaEditRepository: MediaEditRepository
) {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * [displayName] 由搜歌模式传入「歌名 - 歌手.mp3」；为空时按响应头 / URL 推导，
     * 直链模式的行为保持不变。
     */
    suspend fun download(
        rawUrl: String,
        onProgress: (Int) -> Unit,
        displayName: String? = null
    ): DownloadResult =
        withContext(Dispatchers.IO) {
            val url = DownloadLink.normalize(rawUrl)
                ?: return@withContext DownloadResult.Failure(DownloadError.INVALID_URL)
            if (!hasNetwork()) {
                return@withContext DownloadResult.Failure(DownloadError.NO_NETWORK)
            }

            val temp = File(context.cacheDir, "shiyin_dl_${System.currentTimeMillis()}.tmp")
            var call: Call? = null
            return@withContext try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()
                call = client.newCall(request)
                val response = call.execute()
                try {
                    consume(response, url, temp, onProgress, displayName)
                } finally {
                    response.close()
                }
            } catch (cancellation: CancellationException) {
                // 取消必须原样抛出，否则会破坏协程的结构化取消。
                throw cancellation
            } catch (tooLarge: TooLargeException) {
                DownloadResult.Failure(DownloadError.FILE_TOO_LARGE)
            } catch (io: IOException) {
                DownloadResult.Failure(DownloadError.REQUEST_FAILED, io.message)
            } catch (error: Throwable) {
                DownloadResult.Failure(DownloadError.WRITE_FAILED, error.message)
            } finally {
                call?.cancel()
                temp.delete()
            }
        }

    /** 消费响应体：校验 → 流式落盘（带进度）→ 发布到媒体库。 */
    private suspend fun consume(
        response: Response,
        url: String,
        temp: File,
        onProgress: (Int) -> Unit,
        requestedName: String?
    ): DownloadResult {
        if (!response.isSuccessful) {
            return DownloadResult.Failure(
                DownloadError.HTTP_ERROR,
                "HTTP ${response.code}"
            )
        }

        val body = response.body
        val contentType = body.contentType()?.toString()
            ?: response.header("Content-Type")
        val declaredLength = body.contentLength()
        if (declaredLength > DownloadLink.MAX_BYTES) {
            return DownloadResult.Failure(DownloadError.FILE_TOO_LARGE)
        }

        val displayName = requestedName?.takeIf { it.isNotBlank() }
            ?: DownloadLink.fileName(
                url,
                contentType,
                response.header("Content-Disposition")
            )
        val mime = DownloadLink.mediaMime(displayName, contentType)
            ?: return DownloadResult.Failure(DownloadError.UNSUPPORTED_MEDIA)

        body.byteStream().use { input ->
            temp.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    output.write(buffer, 0, read)
                    total += read
                    if (total > DownloadLink.MAX_BYTES) throw TooLargeException()
                    onProgress(
                        if (declaredLength > 0) {
                            (total * 100 / declaredLength).toInt().coerceIn(0, 99)
                        } else {
                            -1
                        }
                    )
                }
                output.flush()
            }
        }

        if (temp.length() == 0L) {
            return DownloadResult.Failure(DownloadError.EMPTY_FILE)
        }
        val published = mediaEditRepository.publishFile(temp, displayName, mime)
        onProgress(100)
        return DownloadResult.Success(published)
    }

    private fun hasNetwork(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private class TooLargeException : Exception()

    private companion object {
        const val USER_AGENT = "Shiyin/0.1.0 (Android; direct-link download)"
    }
}
