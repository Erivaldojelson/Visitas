package com.monst.transfiranow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.monst.transfiranow.MainActivity
import com.monst.transfiranow.R
import com.monst.transfiranow.data.TransferRepository
import androidx.compose.ui.graphics.toArgb
import com.monst.transfiranow.data.TransferEntry
import com.monst.transfiranow.data.formatBytes
import com.monst.transfiranow.data.isIndeterminate
import com.monst.transfiranow.data.progressPercent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransferMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        TransferRepository.refreshPermissionState(this)
        monitorJob = serviceScope.launch {
            TransferRepository.uiState.collectLatest {
                val manager = getSystemService<NotificationManager>() ?: return@collectLatest
                manager.notify(NOTIFICATION_ID, buildNotification())
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        monitorJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Transferências",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mostra downloads ativos do Transfira Now"
            setShowBadge(false)
        }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val uiState = TransferRepository.uiState.value
        val top = TransferRepository.topActiveEntry()
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val colorInt = uiState.accentColor.toArgb()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setColor(colorInt)
            .setColorized(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo_foreground))

        if (top == null) {
            return builder
                .setContentTitle("Transfira Now ativo")
                .setContentText("Aguardando downloads detectados nas notificações")
                .setPublicVersion(
                    NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_download)
                        .setContentTitle("Transfira-now")
                        .setContentText("Aguardando transferências")
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .build()
                )
                .build()
        }

        val title = top.title
        val content = "${top.sourceApp} · ${top.progress?.let { "$it%" } ?: "Em andamento"}"
        val compactView = buildCompactRemoteViews(top)
        val expandedView = buildExpandedRemoteViews(top)
        val publicVersion = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(title)
            .setContentText(content)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(100, top.progressPercent, top.isIndeterminate)
            .build()

        if (Build.VERSION.SDK_INT >= 36) {
            buildRuntimeProgressStyle(
                title = title,
                content = content,
                colorInt = colorInt,
                progress = top.progressPercent,
                contentIntent = contentIntent,
                compactView = compactView,
                expandedView = expandedView,
                publicVersion = publicVersion
            )?.let { return it }
        }

        return builder
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setPublicVersion(publicVersion)
            .setProgress(100, top.progressPercent, top.isIndeterminate)
            .build()
    }

    private fun buildCompactRemoteViews(entry: TransferEntry): RemoteViews {
        return RemoteViews(packageName, R.layout.notification_transfer_compact).apply {
            setTextViewText(R.id.transfer_title, entry.title)
            setTextViewText(R.id.transfer_subtitle, entry.sourceApp)
            setTextViewText(R.id.transfer_percent, if (entry.isIndeterminate) "..." else "${entry.progressPercent}%")
            setProgressBar(R.id.transfer_progress, 100, entry.progressPercent, entry.isIndeterminate)
            setImageViewResource(R.id.transfer_icon, R.drawable.logo_foreground)
        }
    }

    private fun buildExpandedRemoteViews(entry: TransferEntry): RemoteViews {
        val meta = buildString {
            append(entry.sourceApp)
            entry.downloadedBytes?.let { bytes -> append(" · ").append(bytes.formatBytes()) }
            entry.totalBytes?.let { bytes -> append(" / ").append(bytes.formatBytes()) }
        }
        return RemoteViews(packageName, R.layout.notification_transfer_expanded).apply {
            setTextViewText(R.id.transfer_title, entry.title)
            setTextViewText(R.id.transfer_subtitle, "Download em segundo plano")
            setTextViewText(R.id.transfer_meta, meta)
            setTextViewText(R.id.transfer_percent, if (entry.isIndeterminate) "..." else "${entry.progressPercent}%")
            setProgressBar(R.id.transfer_progress, 100, entry.progressPercent, entry.isIndeterminate)
            setImageViewResource(R.id.transfer_icon, R.drawable.logo_foreground)
        }
    }

    private fun buildRuntimeProgressStyle(
        title: String,
        content: String,
        colorInt: Int,
        progress: Int,
        contentIntent: PendingIntent,
        compactView: RemoteViews,
        expandedView: RemoteViews,
        publicVersion: Notification
    ): Notification? {
        return runCatching {
            val styleClass = Class.forName("android.app.Notification\$ProgressStyle")
            val style = styleClass.getDeclaredConstructor().newInstance()
            styleClass.getMethod("setStyledByProgress", Boolean::class.javaPrimitiveType)
                .invoke(style, true)
            styleClass.getMethod("setProgress", Int::class.javaPrimitiveType)
                .invoke(style, progress)

            val builder = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_download)
                .setContentTitle(title)
                .setContentText(content)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColorized(true)
                .setColor(colorInt)
                .setCustomContentView(compactView)
                .setCustomBigContentView(expandedView)
                .setPublicVersion(publicVersion)
            Notification.Builder::class.java
                .getMethod("setStyle", Notification.Style::class.java)
                .invoke(builder, style)
            builder.build()
        }.getOrNull()
    }

    companion object {
        private const val CHANNEL_ID = "transfer-monitor"
        private const val NOTIFICATION_ID = 101
    }
}
