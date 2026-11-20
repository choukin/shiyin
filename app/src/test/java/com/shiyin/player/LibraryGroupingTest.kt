package com.shiyin.player

import com.shiyin.player.data.model.LibraryGroupKind
import com.shiyin.player.data.model.LibraryUiState
import com.shiyin.player.data.util.GroupSource
import com.shiyin.player.data.util.LibraryGrouping
import com.shiyin.player.data.util.LibraryViewMode
import com.shiyin.player.data.util.deriveLibraryView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 分组与派生逻辑是纯 JVM 的，直接单测，不依赖 Robolectric / android.net.Uri。 */
class LibraryGroupingTest {

    @Test
    fun normalizeFoldsUnknownMetadata() {
        assertEquals("", LibraryGrouping.normalize(""))
        assertEquals("", LibraryGrouping.normalize("   "))
        assertEquals("", LibraryGrouping.normalize("<unknown>"))
        assertEquals("", LibraryGrouping.normalize("<UNKNOWN>"))
        assertEquals("", LibraryGrouping.normalize("(unknown)"))
        assertEquals("周杰伦", LibraryGrouping.normalize("  周杰伦  "))
    }

    @Test
    fun groupByAlbumCountsTracksAndSkipsInvalidAlbumId() {
        val sources = listOf(
            GroupSource(1, "A", "专辑甲", 0),
            GroupSource(2, "A", "专辑甲", 12),
            GroupSource(3, "B", "专辑乙", 0)
        )
        val groups = LibraryGrouping.group(sources, LibraryGroupKind.ALBUM)
        assertEquals(2, groups.size)
        // 中文按拼音排序，不假设具体先后，按 key 定位。
        val first = groups.first { it.key == "专辑甲" }
        assertEquals(2, first.trackCount)
        assertEquals(listOf(1L, 2L), first.trackIds)
        assertEquals(12L, first.albumId)
        assertEquals(1L, first.representativeId)
    }

    @Test
    fun groupByArtistSortsCaseInsensitiveWithUnknownLast() {
        val sources = listOf(
            GroupSource(1, "<unknown>", "x", 0),
            GroupSource(2, "beta", "x", 0),
            GroupSource(3, "Alpha", "x", 0)
        )
        val groups = LibraryGrouping.group(sources, LibraryGroupKind.ARTIST)
        assertEquals(listOf("Alpha", "beta", ""), groups.map { it.key })
    }

    @Test
    fun parseFavoriteIdsIgnoresInvalidEntries() {
        assertEquals(
            setOf(1L, 3L),
            LibraryGrouping.parseFavoriteIds(setOf("1", "3", "oops", ""))
        )
    }

    @Test
    fun defaultStateDerivesAllTracksView() {
        val view = deriveLibraryView(LibraryUiState())
        assertEquals(LibraryViewMode.TRACKS, view.mode)
        assertTrue(view.tracks.isEmpty())
        assertTrue(view.groups.isEmpty())
        assertFalse(view.isDetail)
    }

    @Test
    fun emptyLibraryStillYieldsEmptyGroupViews() {
        val albums = deriveLibraryView(LibraryUiState(selectedTab = 2))
        assertEquals(LibraryViewMode.GROUPS, albums.mode)
        assertEquals(LibraryGroupKind.ALBUM, albums.kind)
        assertTrue(albums.groups.isEmpty())

        val artists = deriveLibraryView(LibraryUiState(selectedTab = 3))
        assertEquals(LibraryViewMode.GROUPS, artists.mode)
        assertEquals(LibraryGroupKind.ARTIST, artists.kind)
        assertTrue(artists.groups.isEmpty())
    }

    @Test
    fun detailAlwaysDerivesTracksView() {
        val view = deriveLibraryView(
            LibraryUiState(
                selectedTab = 2,
                detail = com.shiyin.player.data.model.LibraryGroupDetail(
                    LibraryGroupKind.ALBUM,
                    "专辑甲"
                )
            )
        )
        assertEquals(LibraryViewMode.TRACKS, view.mode)
        assertTrue(view.isDetail)
        assertTrue(view.tracks.isEmpty())
    }
}
