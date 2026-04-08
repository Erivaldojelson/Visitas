package com.monst.transfiranow.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.monst.transfiranow.MainActivity
import com.monst.transfiranow.R

object AppNotifications {
    private const val CHANNEL_UPDATES = "visitas_updates"
    private const val CHANNEL_EVENT = "visitas_event"

    private const val NOTIFICATION_ID_CARD_STATUS = 2410

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_UPDATES,
            "Atualizações",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Avisos do Transfira Now (criação de cartões e Live Updates)."
        }
        manager.createNotificationChannel(channel)

        val eventChannel = NotificationChannel(
            CHANNEL_EVENT,
            "Modo Evento",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Atividades em andamento (Now Bar/Live notifications)."
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
        }
        manager.createNotificationChannel(eventChannel)
    }

    fun canPostNotifications(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canPostPromotedNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 36) return false
        return NotificationManagerCompat.from(context).canPostPromotedNotifications()
    }

    fun postCardGenerationLiveUpdate(
        context: Context,
        cardName: String,
        criticalText: String = "Gerando",
        requestPromoted: Boolean = false,
        pillColor: Int? = null
    ) {
        ensureChannels(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("Gerando cartão")
            .setContentText(cardName.ifBlank { "Aguarde…" })
            .apply { pillColor?.let { setColor(it).setColorized(true) } }
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(0, 0, true)
            .setContentIntent(mainPendingIntent(context))

        if (Build.VERSION.SDK_INT >= 36 && requestPromoted) {
            builder
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(criticalText)
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CARD_STATUS, builder.build())
    }

    fun postCardGenerationCompleted(context: Context, cardName: String, requestPromoted: Boolean = false) {
        ensureChannels(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_UPDATES)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle("Cartão gerado")
            .setContentText(cardName.ifBlank { "Seu passe já está salvo." })
            .setOngoing(false)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(mainPendingIntent(context))

        if (Build.VERSION.SDK_INT >= 36 && requestPromoted) {
            builder.setRequestPromotedOngoing(false)
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_CARD_STATUS, builder.build())
    }

    fun cancelCardStatus(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_CARD_STATUS)
    }

    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startIntentSafely(context, intent)
    }

    fun openAppNotificationPromotionSettings(context: Context) {
        val promotionIntent = Intent("android.settings.APP_NOTIFICATION_PROMOTION_SETTINGS")
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (promotionIntent.resolveActivity(context.packageManager) != null) {
            startIntentSafely(context, promotionIntent)
        } else {
            openAppNotificationSettings(context)
        }
    }

    fun startEventMode(
        context: Context,
        title: String = "Modo Evento Ativo",
        text: String = "Seu cartão está pronto para compartilhar",
        durationMs: Long = 59L * 60L * 1000L,
        cardId: String? = null
    ) {
        ensureChannels(context)
        val intent = Intent(context, EventModeService::class.java).apply {
            putExtra(EventModeService.EXTRA_TITLE, title)
            putExtra(EventModeService.EXTRA_TEXT, text)
            putExtra(EventModeService.EXTRA_DURATION_MS, durationMs)
            cardId?.trim()?.takeIf { it.isNotBlank() }?.let { putExtra(EventModeService.EXTRA_CARD_ID, it) }
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopEventMode(context: Context) {
        val intent = Intent(context, EventModeService::class.java).setAction(EventModeService.ACTION_STOP)
        runCatching { context.startService(intent) }
    }

    private fun startIntentSafely(context: Context, intent: Intent) {
        runCatching { context.startActivity(intent) }
            .onFailure { openAppDetails(context) }
    }

    private fun openAppDetails(context: Context) {
        val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(details) }
    }

    private fun mainPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, 0, intent, flags)
    }
}
