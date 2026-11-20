package com.shiyin.player

import com.shiyin.player.data.model.MusicSource
import com.shiyin.player.data.util.SearchLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 04 下载页搜歌链路的纯 JVM 逻辑单测。
 * 真正的网络请求与 org.json 解析属 Android 层，不在此覆盖。
 */
class SearchLinkTest {

    @Test
    fun `forceHttps upgrades http playback links`() {
        assertEquals(
            "https://music.163.com/song/media/outer/url?id=123.mp3",
            SearchLink.forceHttps("http://music.163.com/song/media/outer/url?id=123.mp3")
        )
    }

    @Test
    fun `forceHttps keeps https and leaves other values untouched`() {
        val https = "https://example.com/a.mp3"
        assertEquals(https, SearchLink.forceHttps(https))
        assertEquals("data:text/plain,", SearchLink.forceHttps("data:text/plain,"))
        assertEquals("", SearchLink.forceHttps("   "))
    }

    @Test
    fun `isPlayable accepts only http and https`() {
        assertTrue(SearchLink.isPlayable("https://a/b.mp3"))
        assertTrue(SearchLink.isPlayable("http://a/b.mp3"))
        assertFalse(SearchLink.isPlayable("data:text/plain,"))
        assertFalse(SearchLink.isPlayable(""))
    }

    @Test
    fun `formFields match the site contract`() {
        val fields = SearchLink.formFields("  周杰伦 ", 2, MusicSource.NETEASE)
        assertEquals("周杰伦", fields["input"])
        assertEquals("name", fields["filter"])
        assertEquals("2", fields["page"])
        assertEquals("netease", fields["type"])
    }

    @Test
    fun `formFields clamp page to at least one`() {
        assertEquals("1", SearchLink.formFields("x", 0, MusicSource.NETEASE)["page"])
        assertEquals("1", SearchLink.formFields("x", -5, MusicSource.KUAKE)["page"])
        assertEquals("ws", SearchLink.formFields("x", 1, MusicSource.WS)["type"])
    }

    @Test
    fun `displayName combines title and artist with mp3 extension`() {
        assertEquals("屋顶 - 周杰伦.mp3", SearchLink.displayName("屋顶", "周杰伦"))
    }

    @Test
    fun `displayName handles missing or duplicated artist`() {
        assertEquals("你好.mp3", SearchLink.displayName("你好", ""))
        assertEquals("同名.mp3", SearchLink.displayName("同名", "同名"))
        assertEquals("拾音下载.mp3", SearchLink.displayName("", ""))
    }

    @Test
    fun `displayName replaces illegal filename characters`() {
        assertEquals("A_B - C_D.mp3", SearchLink.displayName("A/B", "C:D"))
    }

    @Test
    fun `displayName is clamped and always keeps mp3 extension`() {
        val name = SearchLink.displayName("歌".repeat(300), "歌手")
        assertTrue(name.endsWith(".mp3"))
        assertTrue(name.length <= 120)
    }
}
