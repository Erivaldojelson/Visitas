package com.monst.transfiranow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.monst.transfiranow.MainActivity
import com.monst.transfiranow.R
import com.monst.transfiranow.data.TransferEntry
import com.monst.transfiranow.data.TransferOrigin
import com.monst.transfiranow.data.TransferState
import com.monst.transfiranow.data.formatBytes
import com.monst.transfiranow.data.formatSpeed
import com.monst.transfiranow.data.isIndeterminate
import com.monst.transfiranow.data.progressPercent
import kotlin.math.roundToInt

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "transfer-monitor"
        const val CHANNEL_GROUP_ID = "transfer-monitor-group"
        const val NOTIFICATION_ID = 101

        fun createNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val group = NotificationChannelGroup(
                CHANNEL_GROUP_ID,
                context.getString(R.string.notification_channel_name)
            )
            notificationManager.createNotificationChannelGroup(group)

            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
                setGroup(CHANNEL_GROUP_ID)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    private val size =
        (context.resources.displayMetrics.density * 24).roundToInt().coerceAtLeast(48)
    private val bitmap = createBitmap(size, size)
    private val canvas = Canvas(bitmap)

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.58f
    }

    private val unitPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.34f
    }

    fun buildNotification(
        entry: TransferEntry?,
        accentColor: Int,
        canPromote: Boolean
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setColor(accentColor)
            .setSubText(context.getString(R.string.app_name))

        if (entry == null) {
            return builder
                .setContentTitle(context.getString(R.string.notification_idle_title))
                .setContentText(context.getString(R.string.notification_idle_text))
                .build()
        }

        val contentText = buildContentText(entry)
        val metaText = buildMetaText(entry)
        val preferLiveUpdate = entry.origin == TransferOrigin.AppManaged &&
            (entry.state == TransferState.Active || entry.state == TransferState.Waiting)

        if (preferLiveUpdate && android.os.Build.VERSION.SDK_INT >= 36) {
            buildRuntimeLiveUpdate(
                entry = entry,
                accentColor = accentColor,
                pendingIntent = pendingIntent,
                contentText = contentText,
                canPromote = canPromote
            )?.let { return it }
        }

        if (entry.state == TransferState.Active || entry.state == TransferState.Waiting) {
            builder.setSmallIcon(buildProgressIcon(entry))
        }

        return builder
            .setContentTitle(entry.title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(metaText))
            .setProgress(100, entry.progressPercent, entry.isIndeterminate)
            .build()
    }

    private fun buildRuntimeLiveUpdate(
        entry: TransferEntry,
        accentColor: Int,
        pendingIntent: PendingIntent,
        contentText: String,
        canPromote: Boolean
    ): Notification? {
        return runCatching {
            val styleClass = Class.forName("android.app.Notification\$ProgressStyle")
            val style = styleClass.getDeclaredConstructor().newInstance()
            styleClass.getMethod("setStyledByProgress", Boolean::class.javaPrimitiveType)
                .invoke(style, true)
            styleClass.getMethod("setProgress", Int::class.javaPrimitiveType)
                .invoke(style, entry.progressPercent)

            val builder = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_download)
                .setContentTitle(entry.title)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(accentColor)

            if (canPromote) {
                Notification.Builder::class.java
                    .getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    .invoke(builder, true)
            }

            runCatching {
                Notification.Builder::class.java
                    .getMethod("setShortCriticalText", String::class.java)
                    .invoke(builder, shortCriticalText(entry))
            }

            Notification.Builder::class.java
                .getMethod("setStyle", Notification.Style::class.java)
                .invoke(builder, style)
            builder.build()
        }.getOrNull()
    }

    private fun buildProgressIcon(entry: TransferEntry): IconCompat {
        val valueStr = if (entry.isIndeterminate) "..." else entry.progressPercent.toString()
        val unitStr = if (entry.isIndeterminate) "" else "%"

        bitmap.eraseColor(Color.TRANSPARENT)
        val cx = size / 2f
        val cyValue = size * 0.54f
        val cyUnit = size * 0.92f

        canvas.drawText(valueStr, cx, cyValue, textPaint)
        canvas.drawText(unitStr, cx, cyUnit, unitPaint)

        return IconCompat.createWithBitmap(bitmap)
    }

    private fun buildContentText(entry: TransferEntry): String {
        val source = when (entry.origin) {
            TransferOrigin.AppManaged -> context.getString(R.string.notification_source_app_managed)
            TransferOrigin.ExternalNotification -> entry.sourceApp
        }
        val progress = if (entry.isIndeterminate) {
            context.getString(R.string.notification_progress_pending)
        } else {
            "${entry.progressPercent}%"
        }
        return "$source · $progress"
    }

    private fun buildMetaText(entry: TransferEntry): String {
        return buildString {
            append(
                if (entry.origin == TransferOrigin.AppManaged) {
                    context.getString(R.string.notification_detail_app_managed)
                } else {
                    context.getString(R.string.notification_detail_external)
                }
            )
            append(" · ")
            append(entry.sourceApp)
            entry.downloadedBytes?.let { append(" · ").append(it.formatBytes()) }
            entry.totalBytes?.let { append(" / ").append(it.formatBytes()) }
            entry.speedBytesPerSecond?.let { append(" · ").append(it.formatSpeed()) }
            entry.detail?.takeIf { it.isNotBlank() }?.let { append("\n").append(it) }
        }
    }

    private fun shortCriticalText(entry: TransferEntry): String {
        return if (entry.isIndeterminate) {
            context.getString(R.string.notification_progress_pending)
        } else {
            "${entry.progressPercent}%"
        }
    }
}
