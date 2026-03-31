package com.monst.transfiranow.data

import android.app.Notification
import android.content.Context
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

object TransferRepository {
    private val entries = ConcurrentHashMap<String, TransferEntry>()
    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    fun updateAccentColor(color: Color) {
        _uiState.update { it.copy(accentColor = color) }
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        _uiState.update { it.copy(dynamicColorEnabled = enabled) }
    }

    fun setDownloadUrl(value: String) {
        _uiState.update { it.copy(downloadUrl = value) }
    }

    fun setHelperMessage(value: String) {
        _uiState.update { it.copy(helperMessage = value) }
    }

    fun refreshPermissionState(context: Context) {
        _uiState.update {
            it.copy(notificationsAccessGranted = hasNotificationAccess(context))
        }
    }

    fun hasNotificationAccess(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        return enabled.contains(context.packageName)
    }

    fun upsert(entry: TransferEntry) {
        entries[entry.id] = entry
        syncState()
    }

    fun remove(id: String) {
        entries.remove(id)
        syncState()
    }

    fun topActiveEntry(): TransferEntry? {
        return entries.values
            .filter { it.state == TransferState.Active || it.state == TransferState.Waiting }
            .sortedWith(
                compareByDescending<TransferEntry> { it.origin == TransferOrigin.AppManaged }
                    .thenByDescending { it.updatedAt }
            )
            .firstOrNull()
    }

    fun maybeCaptureNotification(
        id: String,
        appName: String,
        notification: Notification
    ) {
        val extras = notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val lowerText = "$title $text".lowercase(Locale.ROOT)
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        val looksLikeDownload = lowerText.contains("download") ||
            lowerText.contains("baix") ||
            progress >= 0

        if (!looksLikeDownload) return

        val normalizedProgress = when {
            indeterminate -> null
            max > 0 && progress >= 0 -> ((progress / max.toFloat()) * 100f).roundToInt()
            progress in 0..100 -> progress
            else -> null
        }

        upsert(
            TransferEntry(
                id = id,
                sourceApp = appName,
                title = title.ifBlank { text.ifBlank { "Transferência em andamento" } },
                progress = normalizedProgress,
                state = if (indeterminate) TransferState.Waiting else TransferState.Active,
                origin = TransferOrigin.ExternalNotification,
                detail = "Detectado pelas notificações de outro app."
            )
        )
    }

    private fun syncState() {
        _uiState.update { current ->
            current.copy(entries = entries.values.sortedByDescending { it.updatedAt })
        }
    }
}
