package com.shiyin.player

import com.shiyin.player.data.util.ClipRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 二期裁剪边界的纯 JVM 单测；不依赖 Android 框架。 */
class ClipRangeTest {

    @Test
    fun full_coversWholeDuration() {
        val range = ClipRange.full(10_000L)
        assertEquals(0L, range.startMs)
        assertEquals(10_000L, range.endMs)
        assertEquals(10_000L, range.lengthMs)
        assertTrue(range.isValid(10_000L))
    }

    @Test
    fun clamp_negativeStartIsClampedToZero() {
        val range = ClipRange(-500L, 3_000L).clamp(10_000L)
        assertEquals(0L, range.startMs)
        assertEquals(3_000L, range.endMs)
    }

    @Test
    fun clamp_endBeyondDurationIsCapped() {
        val range = ClipRange(2_000L, 50_000L).clamp(10_000L)
        assertEquals(2_000L, range.startMs)
        assertEquals(10_000L, range.endMs)
    }

    @Test
    fun clamp_enforcesMinimumLength() {
        val range = ClipRange(2_000L, 2_500L).clamp(10_000L)
        assertEquals(2_000L, range.startMs)
        assertEquals(2_000L + ClipRange.MIN_LENGTH_MS, range.endMs)
    }

    @Test
    fun clamp_minimumLengthNearEndMovesEndNotStartBeyondDuration() {
        val range = ClipRange(8_000L, 8_100L).clamp(10_000L)
        assertEquals(8_000L, range.startMs)
        assertEquals(9_000L, range.endMs)
        assertTrue(range.isValid(10_000L))
    }

    @Test
    fun clamp_reversedRangeIsRepaired() {
        val range = ClipRange(5_000L, 1_000L).clamp(10_000L)
        assertEquals(5_000L, range.startMs)
        assertEquals(6_000L, range.endMs)
        assertTrue(range.isValid(10_000L))
    }

    @Test
    fun clamp_zeroDurationReturnsEmpty() {
        val range = ClipRange(100L, 500L).clamp(0L)
        assertEquals(0L, range.startMs)
        assertEquals(0L, range.endMs)
        assertFalse(range.isValid(0L))
    }

    @Test
    fun clamp_durationShorterThanMinimumUsesWholeDuration() {
        val range = ClipRange(0L, 500L).clamp(500L)
        assertEquals(0L, range.startMs)
        assertEquals(500L, range.endMs)
        assertTrue(range.isValid(500L))
    }

    @Test
    fun isValid_rejectsEmptyNegativeAndOverflowRanges() {
        assertFalse(ClipRange(0L, 0L).isValid(10_000L))
        assertFalse(ClipRange(-1L, 5_000L).isValid(10_000L))
        assertFalse(ClipRange(0L, 20_000L).isValid(10_000L))
        assertTrue(ClipRange(1_000L, 2_000L).isValid(10_000L))
    }
}
