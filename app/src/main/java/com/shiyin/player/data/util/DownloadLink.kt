package com.shiyin.player.data.util

import java.net.URI
import java.net.URLDecoder
import java.util.Locale

/**
 * 04 链接下载的纯逻辑：URL 校验、入库文件名解析、媒体 MIME 判定。
 *
 * 刻意不依赖任何 Android / OkHttp 类型，便于 JVM 单测覆盖边界。
 * 只接受用户显式粘贴的 http(s) 直链；**不解析第三方平台页面、不做链接嗅探**，
 * 非音频/视频内容（例如 HTML 错误页）由 [mediaMime] 返回 null 直接拒绝入库。
 */
object DownloadLink {

    /** 单文件上限，避免误贴超大地址把缓存与音乐库写满。 */
    const val MAX_BYTES = 300L * 1024 * 1024

    private const val MAX_NAME_LENGTH = 120
    private const val DEFAULT_EXTENSION = "mp3"
    private const val FALLBACK_NAME = "拾音下载"

    private val EXTENSION_TO_MIME = mapOf(
        "mp3" to "audio/mpeg",
        "m4a" to "audio/mp4",
        "mp4" to "video/mp4",
        "m4v" to "video/mp4",
        "aac" to "audio/aac",
        "wav" to "audio/wav",
        "flac" to "audio/flac",
        "ogg" to "audio/ogg",
        "oga" to "audio/ogg",
        "opus" to "audio/ogg",
        "wma" to "audio/x-ms-wma",
        "aiff" to "audio/x-aiff",
        "mkv" to "video/x-matroska",
        "webm" to "video/webm",
        "3gp" to "video/3gpp",
        "mov" to "video/quicktime"
    )

    private val MIME_TO_EXTENSION = mapOf(
        "audio/mpeg" to "mp3",
        "audio/mp4" to "m4a",
        "audio/aac" to "aac",
        "audio/wav" to "wav",
        "audio/x-wav" to "wav",
        "audio/flac" to "flac",
        "audio/ogg" to "ogg",
        "video/mp4" to "mp4",
        "video/webm" to "webm"
    )

    private val ILLEGAL_CHARS = Regex("[/\\\\:*?\"<>|\\p{Cntrl}]")

    /** 仅接受 http/https 且 host 非空；合法时返回去除首尾空白的原串，否则 null。 */
    fun normalize(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        val uri = runCatching { URI(value) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank()) return null
        return value
    }

    /** 去掉 `; charset=...` 等参数并小写，未知返回空串。 */
    fun canonicalType(contentType: String?): String =
        contentType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase(Locale.ROOT)
            .orEmpty()

    /**
     * 最终入库文件名，优先级：Content-Disposition > URL path > 兜底名。
     * 会做 URL 解码、非法字符替换；无扩展名时按 Content-Type 补齐，超长时保留扩展名截断。
     */
    fun fileName(url: String, contentType: String?, disposition: String?): String {
        val fromDisposition = disposition?.let(::parseDisposition)
        val fromUrl = runCatching {
            URI(url).path.orEmpty().substringAfterLast('/')
        }.getOrDefault("")
        val raw = decode(fromDisposition ?: fromUrl)
            .trim()
            .substringBefore('?')
            .substringBefore('#')
        val cleaned = sanitize(raw)
        val type = canonicalType(contentType)
        val withExtension = when {
            cleaned.isEmpty() -> "$FALLBACK_NAME.${MIME_TO_EXTENSION[type] ?: DEFAULT_EXTENSION}"
            extensionOf(cleaned) == null ->
                "$cleaned.${MIME_TO_EXTENSION[type] ?: DEFAULT_EXTENSION}"
            else -> cleaned
        }
        return clamp(withExtension)
    }

    /** 可入库的 MIME；HTML/JSON 等错误页、以及无法判定为音视频的内容返回 null。 */
    fun mediaMime(fileName: String, contentType: String?): String? {
        val ext = extensionOf(fileName)?.lowercase(Locale.ROOT)
        val type = canonicalType(contentType)
        if (type.startsWith("text/") || type == "application/json") return null
        EXTENSION_TO_MIME[ext]?.let { return it }
        return when {
            type.startsWith("audio/") || type.startsWith("video/") -> type
            type == "application/octet-stream" || type == "binary/octet-stream" -> "audio/mpeg"
            else -> null
        }
    }

    fun extensionOf(name: String): String? {
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.length - 1) return null
        return name.substring(dot + 1)
    }

    private fun parseDisposition(header: String): String? {
        val encoded = Regex(
            "filename\\*\\s*=\\s*UTF-8''([^;]+)",
            RegexOption.IGNORE_CASE
        ).find(header)?.groupValues?.get(1)
        if (!encoded.isNullOrBlank()) return decode(encoded)
        return Regex(
            "filename\\s*=\\s*\"?([^\";]+)\"?",
            RegexOption.IGNORE_CASE
        ).find(header)?.groupValues?.get(1)
    }

    private fun decode(value: String): String = runCatching {
        URLDecoder.decode(value, "UTF-8")
    }.getOrDefault(value)

    private fun sanitize(value: String): String =
        ILLEGAL_CHARS.replace(value, "_").trim().trim('.')

    private fun clamp(name: String): String {
        if (name.length <= MAX_NAME_LENGTH) return name
        val suffix = extensionOf(name)?.let { ".$it" }.orEmpty()
        return name.take((MAX_NAME_LENGTH - suffix.length).coerceAtLeast(1)) + suffix
    }
}
