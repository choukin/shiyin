package com.shiyin.player.data.util

/**
 * 裁剪区间的纯 JVM 计算（不依赖 Android 框架，便于单测）。
 *
 * 所有边界收敛都集中在这里，UI 层只负责把滑杆结果传进来，
 * 避免把 start/end 的钳制逻辑散落在 Compose 回调里。
 */
data class ClipRange(val startMs: Long, val endMs: Long) {

    val lengthMs: Long get() = endMs - startMs

    /**
     * 把区间钳制到 [0, durationMs]，并保证至少 [minLengthMs] 长。
     * durationMs <= 0 时返回空区间，调用方据此禁用导出。
     */
    fun clamp(durationMs: Long, minLengthMs: Long = MIN_LENGTH_MS): ClipRange {
        if (durationMs <= 0L) return ClipRange(0L, 0L)
        val minLength = minLengthMs.coerceIn(1L, durationMs)
        val latestStart = durationMs - minLength
        val start = startMs.coerceIn(0L, latestStart)
        val end = endMs.coerceIn(start + minLength, durationMs)
        return ClipRange(start, end)
    }

    fun isValid(durationMs: Long, minLengthMs: Long = MIN_LENGTH_MS): Boolean =
        durationMs > 0L &&
            startMs >= 0L &&
            endMs <= durationMs &&
            lengthMs >= minLengthMs.coerceAtMost(durationMs)

    companion object {
        /** 最短导出片段，避免产出 0 字节音频。 */
        const val MIN_LENGTH_MS = 1_000L

        fun full(durationMs: Long): ClipRange = ClipRange(0L, durationMs.coerceAtLeast(0L))
    }
}
