package com.monst.transfiranow.notifications

import android.app.Service
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.TaskStackBuilder
import com.monst.transfiranow.MainActivity
import com.monst.transfiranow.R
import com.monst.transfiranow.data.CardStore
import com.monst.transfiranow.share.CardExport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventModeService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timeoutJob: Job? = null
    private var thumbnailJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timeoutJob?.cancel()
        thumbnailJob?.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEventMode()
                return START_NOT_STICKY
            }
            else -> {
                val durationMs = intent?.getLongExtra(EXTRA_DURATION_MS, DEFAULT_DURATION_MS) ?: DEFAULT_DURATION_MS
                val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Modo Evento Ativo"
                val text = intent?.getStringExtra(EXTRA_TEXT) ?: "Seu cartão está pronto para compartilhar"
                val cardId = intent?.getStringExtra(EXTRA_CARD_ID).orEmpty().trim()
                startEventMode(title = title, text = text, durationMs = durationMs, cardId = cardId.takeIf { it.isNotBlank() })
                return START_STICKY
            }
        }
    }

    private fun startEventMode(title: String, text: String, durationMs: Long, cardId: String?) {
        AppNotifications.ensureChannels(this)

        val endAt = System.currentTimeMillis() + durationMs
        val notification = buildNotification(title = title, text = text, endAt = endAt, durationMs = durationMs, thumbnail = null)

        val serviceType = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)

        thumbnailJob?.cancel()
        if (!cardId.isNullOrBlank()) {
            thumbnailJob = scope.launch {
                val card = withContext(Dispatchers.IO) {
                    CardStore(applicationContext).getCardById(cardId)
                } ?: return@launch

                val bitmap = runCatching {
                    withContext(Dispatchers.Default) {
                        CardExport.renderNotificationThumbnail(applicationContext, card)
                    }
                }.getOrNull()

                if (bitmap != null) {
                    val updated = buildNotification(title = title, text = text, endAt = endAt, durationMs = durationMs, thumbnail = bitmap)
                    NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, updated)
                }
            }
        }

        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(durationMs)
            stopEventMode()
        }
    }

    private fun stopEventMode() {
        timeoutJob?.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, text: String, endAt: Long, durationMs: Long, thumbnail: Bitmap?): android.app.Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(mainIntent)
            .getPendingIntent(0, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopIntent = Intent(this, EventModeService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endAt)
            .setTimeoutAfter(durationMs)
            .setContentIntent(mainPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(R.drawable.ic_stat_download, "Encerrar", stopPendingIntent)

        if (thumbnail != null) {
            builder
                .setLargeIcon(thumbnail)
                .setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(thumbnail)
                        .bigLargeIcon(null as Bitmap?)
                )
        }

        if (Build.VERSION.SDK_INT >= 36) {
            builder
                .setRequestPromotedOngoing(true)
                .setShortCriticalText("Evento")
        }

        return builder.build()
    }

    companion object {
        const val ACTION_STOP = "com.monst.transfiranow.action.STOP_EVENT_MODE"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_CARD_ID = "extra_card_id"

        private const val DEFAULT_DURATION_MS = 59L * 60L * 1000L

        private const val NOTIFICATION_ID = 2411
        private const val CHANNEL_ID = "visitas_event"
    }
}
