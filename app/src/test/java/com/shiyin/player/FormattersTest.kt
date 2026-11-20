package com.shiyin.player

import com.shiyin.player.data.util.Formatters
import org.junit.Assert.assertEquals
import org.junit.Test

/** 纯 JVM 单测：不依赖 Android 框架 / Compose Dp，可直接用 JUnit4 运行。 */
class FormattersTest {

    @Test
    fun duration_zeroIsDoubleZero() {
        assertEquals("00:00", Formatters.formatDuration(0L))
    }

    @Test
    fun duration_minutesAndSeconds() {
        assertEquals("03:24", Formatters.formatDuration(204_000L))
    }

    @Test
    fun duration_hoursAreIncluded() {
        assertEquals("1:01:05", Formatters.formatDuration(3_665_000L))
    }

    @Test
    fun duration_negativeIsClamped() {
        assertEquals("00:00", Formatters.formatDuration(-1L))
    }

    @Test
    fun dateAdded_invalidReturnsPlaceholder() {
        assertEquals("--", Formatters.formatDateAdded(0L))
    }

    @Test
    fun fileSize_bytes() {
        assertEquals("512 B", Formatters.formatFileSize(512L))
    }

    @Test
    fun fileSize_kilobytes() {
        assertEquals("1.5 KB", Formatters.formatFileSize(1536L))
    }

    @Test
    fun fileSize_megabytes() {
        assertEquals("2.0 MB", Formatters.formatFileSize(2L * 1024 * 1024))
    }

    @Test
    fun fileSize_zeroAndNegative() {
        assertEquals("0 B", Formatters.formatFileSize(0L))
        assertEquals("0 B", Formatters.formatFileSize(-100L))
    }
}
