package com.shiyin.player

import com.shiyin.player.data.util.DownloadLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 04 链接下载的纯逻辑边界：URL 校验、文件名解析、媒体类型判定。 */
class DownloadLinkTest {

    @Test
    fun normalize_acceptsHttpAndHttps() {
        assertEquals(
            "https://example.com/song.mp3",
            DownloadLink.normalize("  https://example.com/song.mp3  ")
        )
        assertEquals(
            "http://10.0.2.2:8765/test.wav",
            DownloadLink.normalize("http://10.0.2.2:8765/test.wav")
        )
    }

    @Test
    fun normalize_rejectsNonHttpOrMalformed() {
        assertNull(DownloadLink.normalize("ftp://example.com/a.mp3"))
        assertNull(DownloadLink.normalize("file:///sdcard/a.mp3"))
        assertNull(DownloadLink.normalize("example.com/a.mp3"))
        assertNull(DownloadLink.normalize("http://"))
        assertNull(DownloadLink.normalize(""))
        assertNull(DownloadLink.normalize("   "))
    }

    @Test
    fun fileFromUrl_isDecoded() {
        assertEquals(
            "歌.mp3",
            DownloadLink.fileName("https://x.com/music/%E6%AD%8C.mp3", null, null)
        )
        assertEquals(
            "track.flac",
            DownloadLink.fileName("https://x.com/a/track.flac?token=1#frag", null, null)
        )
    }

    @Test
    fun contentDisposition_takesPrecedenceOverUrl() {
        assertEquals(
            "hello world.mp3",
            DownloadLink.fileName(
                "https://x.com/download?id=1",
                "audio/mpeg",
                "attachment; filename=\"hello world.mp3\""
            )
        )
        assertEquals(
            "中文名.m4a",
            DownloadLink.fileName(
                "https://x.com/download",
                "audio/mp4",
                "attachment; filename*=UTF-8''%E4%B8%AD%E6%96%87%E5%90%8D.m4a"
            )
        )
    }

    @Test
    fun missingExtension_isInferredFromContentType() {
        assertEquals(
            "audio.mp3",
            DownloadLink.fileName("https://x.com/audio", "audio/mpeg; charset=utf-8", null)
        )
        assertEquals(
            "拾音下载.m4a",
            DownloadLink.fileName("https://x.com/", "audio/mp4", null)
        )
    }

    @Test
    fun illegalCharacters_areSanitized() {
        assertEquals(
            "a_b_c.mp3",
            DownloadLink.fileName("https://x.com/a:b*c.mp3", null, null)
        )
    }

    @Test
    fun longName_isClampedButKeepsExtension() {
        val result = DownloadLink.fileName(
            "https://x.com/" + "a".repeat(200) + ".mp3",
            "audio/mpeg",
            null
        )
        assertEquals(120, result.length)
        assertTrue(result.endsWith(".mp3"))
    }

    @Test
    fun mediaMime_mapsKnownExtensions() {
        assertEquals("audio/mpeg", DownloadLink.mediaMime("song.mp3", null))
        assertEquals("audio/mp4", DownloadLink.mediaMime("song.m4a", null))
        assertEquals("video/mp4", DownloadLink.mediaMime("clip.mp4", null))
        assertEquals("audio/wav", DownloadLink.mediaMime("tone.wav", null))
        assertEquals("audio/flac", DownloadLink.mediaMime("lossless.FLAC", null))
    }

    @Test
    fun mediaMime_rejectsHtmlAndUnknown() {
        // HTML 错误页即使带 .mp3 后缀也不能入库。
        assertNull(DownloadLink.mediaMime("song.mp3", "text/html; charset=utf-8"))
        assertNull(DownloadLink.mediaMime("page.html", "text/html"))
        assertNull(DownloadLink.mediaMime("data.json", "application/json"))
        assertNull(DownloadLink.mediaMime("x.bin", null))
    }

    @Test
    fun mediaMime_fallsBackToContentType() {
        assertEquals("audio/mpeg", DownloadLink.mediaMime("blob", "application/octet-stream"))
        assertEquals("audio/aac", DownloadLink.mediaMime("noext", "audio/aac"))
    }

    @Test
    fun canonicalType_stripsParametersAndLowercases() {
        assertEquals("audio/mpeg", DownloadLink.canonicalType("Audio/MPEG; charset=utf-8"))
        assertEquals("", DownloadLink.canonicalType(null))
    }
}
