package com.monst.transfiranow.notifications

import android.app.Service
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.app.TaskStackBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.monst.transfiranow.MainActivity
import com.monst.transfiranow.R
import com.monst.transfiranow.data.CardStore
import com.monst.transfiranow.data.DEFAULT_NOW_BAR_COLOR
import com.monst.transfiranow.data.VisitingCard
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
    private var presentationJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        timeoutJob?.cancel()
        presentationJob?.cancel()
        scope.launch {
            runCatching { CardStore(applicationContext).persistEventModeEnabled(false) }
        }
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

        scope.launch {
            runCatching { CardStore(applicationContext).persistEventModeEnabled(true) }
        }

        val endAt = System.currentTimeMillis() + durationMs
        val notification = buildNotification(
            title = title,
            text = text,
            endAt = endAt,
            durationMs = durationMs,
            card = null,
            qrBitmap = null,
            pillColor = DEFAULT_NOW_BAR_COLOR
        )

        val serviceType = if (Build.VERSION.SDK_INT >= 34) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)

        presentationJob?.cancel()
        presentationJob = scope.launch {
            val store = CardStore(applicationContext)
            val pillColor = withContext(Dispatchers.IO) { store.getNowBarColor() }

            val card = cardId?.takeIf { it.isNotBlank() }?.let { id ->
                withContext(Dispatchers.IO) { store.getCardById(id) }
            }

            val qrValue = card?.qrValue?.trim().orEmpty().ifBlank { card?.website?.trim().orEmpty() }
            val qrBitmap = qrValue.takeIf { it.isNotBlank() }?.let { value ->
                runCatching {
                    withContext(Dispatchers.Default) {
                        generateQrBitmap(value, size = 512)
                    }
                }.getOrNull()
            }

            val updatedTitle = card?.name?.trim().orEmpty().ifBlank { title }
            val updatedText = card?.let(::presentationLine).orEmpty().ifBlank { text }
            val updated = buildNotification(
                title = updatedTitle,
                text = updatedText,
                endAt = endAt,
                durationMs = durationMs,
                card = card,
                qrBitmap = qrBitmap,
                pillColor = pillColor
            )
            NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, updated)
        }

        timeoutJob?.cancel()
        timeoutJob = scope.launch {
            delay(durationMs)
            stopEventMode()
        }
    }

    private fun stopEventMode() {
        timeoutJob?.cancel()
        scope.launch {
            runCatching { CardStore(applicationContext).persistEventModeEnabled(false) }
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, text: String, endAt: Long, durationMs: Long, card: VisitingCard?, qrBitmap: Bitmap?, pillColor: Int): android.app.Notification {
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
            .setSmallIcon(R.drawable.ic_stat_person)
            .setContentTitle(title)
            .setContentText(text)
            .setColor(pillColor)
            .apply {
                if (Build.VERSION.SDK_INT < 36) {
                    setColorized(true)
                }
            }
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endAt)
            .setTimeoutAfter(durationMs)
            .setContentIntent(mainPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(R.drawable.ic_stat_download, "Encerrar", stopPendingIntent)

        val phone = card?.phone?.trim().orEmpty()
        if (phone.isNotBlank()) {
            builder.setSubText(phone)
        }

        val role = card?.role?.trim().orEmpty()
        val email = card?.email?.trim().orEmpty()
        val website = card?.website?.trim().orEmpty()
        val linkedin = card?.linkedin?.trim().orEmpty()
        val instagram = card?.instagram?.trim().orEmpty()
            .takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("@")) it else "@$it" }
            .orEmpty()

        val summary = listOfNotNull(
            role.takeIf { it.isNotBlank() },
            phone.takeIf { it.isNotBlank() },
            instagram.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        if (qrBitmap != null) {
            val largeIcon = if (qrBitmap.width > 256) {
                Bitmap.createScaledBitmap(qrBitmap, 256, 256, true)
            } else {
                qrBitmap
            }
            builder.setLargeIcon(largeIcon)
        }

        if (card != null) {
            val expandedLines = buildList {
                role.takeIf { it.isNotBlank() }?.let { add("Cargo: $it") }
                phone.takeIf { it.isNotBlank() }?.let { add("Telefone: $it") }
                email.takeIf { it.isNotBlank() }?.let { add("Email: $it") }
                instagram.takeIf { it.isNotBlank() }?.let { add("Instagram: $it") }
                linkedin.takeIf { it.isNotBlank() }?.let { add("LinkedIn: $it") }
                website.takeIf { it.isNotBlank() }?.let { add("URL: $it") }
            }
            val style = NotificationCompat.InboxStyle()
            style.setBigContentTitle(title)
            expandedLines.take(6).forEach { line -> style.addLine(line) }
            style.setSummaryText(summary.ifBlank { "Toque para compartilhar seu cartão" })
            builder.setStyle(style)
        } else if (summary.isNotBlank()) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
                    .setSummaryText(summary)
            )
        }

        if (Build.VERSION.SDK_INT >= 36) {
            val chip = card?.name
                ?.trim()
                ?.split(" ")
                ?.firstOrNull()
                ?.take(10)
                ?.takeIf { it.isNotBlank() }
                ?: "Cartão"
            builder
                .setRequestPromotedOngoing(true)
                .setShortCriticalText(chip)
        }

        return builder.build()
    }

    private fun presentationLine(card: VisitingCard): String {
        val role = card.role.trim()
        val phone = card.phone.trim()
        val instagram = card.instagram.trim()
            .takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("@")) it else "@$it" }
            .orEmpty()
        return when {
            role.isNotBlank() && phone.isNotBlank() && instagram.isNotBlank() -> "$role • $phone • $instagram"
            role.isNotBlank() && phone.isNotBlank() -> "$role • $phone"
            role.isNotBlank() && instagram.isNotBlank() -> "$role • $instagram"
            phone.isNotBlank() && instagram.isNotBlank() -> "$phone • $instagram"
            role.isNotBlank() -> role
            phone.isNotBlank() -> phone
            instagram.isNotBlank() -> instagram
            else -> ""
        }
    }

    private fun generateQrBitmap(value: String, size: Int): Bitmap {
        val matrix = MultiFormatWriter().encode(
            value,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(EncodeHintType.MARGIN to 1)
        )
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
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
