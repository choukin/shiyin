package com.shiyin.player

import com.shiyin.player.data.model.TrackMetadata
import com.shiyin.player.data.util.TrackMetadataApplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackMetadataApplierTest {

    @Test
    fun hiddenTrack_isExcluded() {
        val result = TrackMetadataApplier.resolve(
            trackId = 9L,
            originalTitle = "原始名称",
            metadata = emptyMap(),
            hiddenIds = setOf("9")
        )
        assertNull(result)
    }

    @Test
    fun noOverride_keepsOriginalTitle() {
        val result = TrackMetadataApplier.resolve(9L, "原始名称", emptyMap(), emptySet())
        assertEquals("原始名称", result?.title)
        assertEquals("", result?.description)
        assertEquals("", result?.artworkFile)
    }

    @Test
    fun override_appliesNameDescriptionAndArtwork() {
        val metadata = mapOf(
            9L to TrackMetadata(9L, "新名字", "一段简介", "cover_9.jpg")
        )
        val result = TrackMetadataApplier.resolve(9L, "原始名称", metadata, emptySet())
        assertEquals("新名字", result?.title)
        assertEquals("一段简介", result?.description)
        assertEquals("cover_9.jpg", result?.artworkFile)
    }

    @Test
    fun blankName_fallsBackToOriginalTitle_butKeepsOtherFields() {
        val metadata = mapOf(9L to TrackMetadata(9L, "   ", "保留简介", "cover_9.jpg"))
        val result = TrackMetadataApplier.resolve(9L, "原始名称", metadata, emptySet())
        assertEquals("原始名称", result?.title)
        assertEquals("保留简介", result?.description)
        assertEquals("cover_9.jpg", result?.artworkFile)
    }
}
