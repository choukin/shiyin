package com.shiyin.player

import com.shiyin.player.data.model.Playlist
import com.shiyin.player.data.util.PlaylistCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistCodecTest {

    @Test
    fun roundTrip_singlePlaylist() {
        val list = listOf(
            Playlist("id-1", "我喜欢的歌", listOf(10L, 20L, 30L), 12345L)
        )
        val decoded = PlaylistCodec.decode(PlaylistCodec.encode(list))
        assertEquals(list, decoded)
    }

    @Test
    fun roundTrip_multipleAndSpecialChars() {
        val list = listOf(
            Playlist("a", "流行/摇滚|测试", listOf(1L, 2L), 1L),
            Playlist("b", "空歌单", emptyList(), 2L)
        )
        assertEquals(list, PlaylistCodec.decode(PlaylistCodec.encode(list)))
    }

    @Test
    fun decode_emptyReturnsEmpty() {
        assertEquals(emptyList<Playlist>(), PlaylistCodec.decode(""))
    }

    @Test
    fun decode_malformedLinesSkipped() {
        val raw = "bad\u0001line\nid\u0001" +
            java.util.Base64.getEncoder().encodeToString("OK".toByteArray()) +
            "\u0001\u0001" + "50,60\n"
        val decoded = PlaylistCodec.decode(raw)
        assertEquals(1, decoded.size)
        assertEquals("OK", decoded[0].name)
        assertEquals(listOf(50L, 60L), decoded[0].trackIds)
    }
}
