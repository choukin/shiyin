package com.shiyin.player

import com.shiyin.player.data.model.TrackMetadata
import com.shiyin.player.data.util.TrackMetadataCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMetadataCodecTest {

    @Test
    fun roundTrip_multipleEntries() {
        val items = listOf(
            TrackMetadata(7L, "夜航西飞", "一段\n简介｜含分隔符", "cover_7.jpg"),
            TrackMetadata(3L, "Yesterday", "", "")
        )
        val decoded = TrackMetadataCodec.decode(TrackMetadataCodec.encode(items))
        assertEquals(items.associateBy { it.trackId }, decoded)
    }

    @Test
    fun encode_blankEntriesSkipped() {
        val raw = TrackMetadataCodec.encode(listOf(TrackMetadata(1L)))
        assertTrue(raw.isEmpty())
    }

    @Test
    fun decode_emptyAndMalformed() {
        assertTrue(TrackMetadataCodec.decode("").isEmpty())
        assertTrue(TrackMetadataCodec.decode("bad-line\n\u0001\u0001\u0001").isEmpty())
    }

    @Test
    fun decode_threeFieldLegacyLine_hasEmptyArtwork() {
        val enc = java.util.Base64.getEncoder()
        val name = enc.encodeToString("旧记录".toByteArray(Charsets.UTF_8))
        val desc = enc.encodeToString("".toByteArray(Charsets.UTF_8))
        val decoded = TrackMetadataCodec.decode("42\u0001$name\u0001$desc\n")
        assertEquals(TrackMetadata(42L, "旧记录", "", ""), decoded[42L])
    }
}
