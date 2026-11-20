package com.shiyin.player.data.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 纯 JVM 工具：不依赖 Android 框架，可直接跑本地单元测试。 */
object Formatters {
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs.coerceAtLeast(0L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDateAdded(dateAddedSec: Long): String {
        if (dateAddedSec <= 0L) return "--"
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateAddedSec * 1000L))
        }.getOrDefault("--")
    }

    /** 人类可读的文件大小：B / KB / MB / GB，保留 1 位小数。 */
    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = sizeBytes.toDouble()
        var unit = 0
        while (value >= 1024.0 && unit < units.lastIndex) {
            value /= 1024.0
            unit++
        }
        return if (unit == 0) {
            String.format(Locale.US, "%d B", value.toLong())
        } else {
            String.format(Locale.US, "%.1f %s", value, units[unit])
        }
    }
}
