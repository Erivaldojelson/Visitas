package com.monst.transfiranow.data

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

enum class TransferState {
    Active,
    Completed,
    Failed,
    Waiting
}

data class TransferEntry(
    val id: String,
    val sourceApp: String,
    val title: String,
    val progress: Int?,
    val state: TransferState,
    val downloadedBytes: Long? = null,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Long? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

val TransferEntry.progressPercent: Int
    get() = progress?.coerceIn(0, 100) ?: 0

val TransferEntry.isIndeterminate: Boolean
    get() = progress == null

data class AccentOption(
    val name: String,
    val color: Color
)

data class TransferUiState(
    val entries: List<TransferEntry> = emptyList(),
    val accentColor: Color = Color(0xFF5D56C4),
    val dynamicColorEnabled: Boolean = true,
    val notificationsAccessGranted: Boolean = false
) {
    val activeEntries: List<TransferEntry>
        get() = entries.filter { it.state == TransferState.Active || it.state == TransferState.Waiting }

    val totalSpeedBytesPerSecond: Long
        get() = activeEntries.sumOf { it.speedBytesPerSecond ?: 0L }
}

fun Long.formatSpeed(): String {
    if (this <= 0L) return "0 B/s"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        this >= gb -> "${((this / gb) * 10).roundToInt() / 10.0} GB/s"
        this >= mb -> "${((this / mb) * 10).roundToInt() / 10.0} MB/s"
        this >= kb -> "${((this / kb) * 10).roundToInt() / 10.0} KB/s"
        else -> "$this B/s"
    }
}

fun Long.formatBytes(): String {
    if (this <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        this >= gb -> "${((this / gb) * 10).roundToInt() / 10.0} GB"
        this >= mb -> "${((this / mb) * 10).roundToInt() / 10.0} MB"
        this >= kb -> "${((this / kb) * 10).roundToInt() / 10.0} KB"
        else -> "$this B"
    }
}
