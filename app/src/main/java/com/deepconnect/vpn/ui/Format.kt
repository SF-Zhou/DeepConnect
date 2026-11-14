package com.deepconnect.vpn.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LogTimestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)

/** Formats a duration as hh:mm:ss (e.g. 3723000 -> "01:02:03"). */
internal fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

/** Formats a byte count using binary units, e.g. 1_258_291 -> "1.2 MB". */
internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    if (bytes < 1024L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    val unit = units[unitIndex]
    return if (value >= 100.0) {
        String.format(Locale.US, "%.0f %s", value, unit)
    } else {
        String.format(Locale.US, "%.1f %s", value, unit)
    }
}

/** Formats a log timestamp as a local wall-clock time, e.g. "14:05:09". */
internal fun formatLogTimestamp(timestamp: Long): String =
    LogTimestampFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

/** Two-letter avatar initials derived from a profile display name. */
internal fun profileInitials(name: String): String {
    val parts = name.trim().split(' ', '-', '_', '.').filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0].take(1).uppercase(Locale.getDefault())
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase(Locale.getDefault())
    }
}
