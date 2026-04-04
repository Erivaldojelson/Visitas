package com.monst.transfiranow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.os.bundleOf
import com.monst.transfiranow.MainActivity
import com.monst.transfiranow.R

class CardNowbarService : Service() {

    private var isForegroundStarted = false

    private var userName: String = ""
    private var userRole: String = ""
    private var viewsCount: Int = 0
    private var qrBitmap: Bitmap? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                userName = intent.getStringExtra(EXTRA_USER_NAME).orEmpty()
                userRole = intent.getStringExtra(EXTRA_USER_ROLE).orEmpty()
                viewsCount = intent.getIntExtra(EXTRA_VIEWS_COUNT, 0)
                ensureChannel()
                startForegroundInternal()
            }

            ACTION_UPDATE -> {
                userName = intent.getStringExtra(EXTRA_USER_NAME) ?: userName
                userRole = intent.getStringExtra(EXTRA_USER_ROLE) ?: userRole
                viewsCount = intent.getIntExtra(EXTRA_VIEWS_COUNT, viewsCount)
                updateNotification()
            }

            ACTION_SET_QR_BITMAP -> {
                qrBitmap = CardNowbarState.qrBitmap
                updateNotification()
            }

            ACTION_STOP -> {
                stopForegroundInternal()
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopForegroundInternal()
        super.onDestroy()
    }

    private fun startForegroundInternal() {
        startForeground(NOTIFICATION_ID, buildNotification())
        isForegroundStarted = true
    }

    private fun stopForegroundInternal() {
        if (!isForegroundStarted) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        isForegroundStarted = false
    }

    private fun updateNotification() {
        ensureChannel()
        val notification = buildNotification()
        if (!isForegroundStarted) {
            startForegroundInternal()
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(): Notification {
        val remoteViews = RemoteViews(packageName, R.layout.layout_card_nowbar).apply {
            setTextViewText(R.id.tvName, userName.ifBlank { getString(R.string.nowbar_default_name) })
            setTextViewText(R.id.tvRole, userRole.ifBlank { getString(R.string.nowbar_default_role) })
            setTextViewText(R.id.tvViews, getString(R.string.nowbar_views_format, viewsCount))

            val bitmap = qrBitmap ?: CardNowbarState.qrBitmap
            if (bitmap != null) {
                setImageViewBitmap(R.id.ivQr, bitmap)
            } else {
                setImageViewResource(R.id.ivQr, R.drawable.ic_qr_placeholder)
            }

            setOnClickPendingIntent(
                R.id.btnShare,
                NowbarIntents.sharePendingIntent(this@CardNowbarService, userName, userRole)
            )
        }

        val contentIntent = NowbarIntents.openAppPendingIntent(this)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentIntent)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setExtras(buildSamsungOngoingExtras(userName, userRole))

        return builder.build()
    }

    private fun buildSamsungOngoingExtras(primary: String, secondary: String): Bundle = bundleOf(
        // Obs: a Now Bar / Live Notifications da Samsung (One UI) depende também de whitelist do package name.
        "android.ongoingActivityNoti.style" to 1,
        "android.ongoingActivityNoti.primaryInfo" to primary,
        "android.ongoingActivityNoti.secondaryInfo" to secondary,
        "android.ongoingActivityNoti.chipExpandedText" to primary,
        "android.ongoingActivityNoti.nowbarPrimaryInfo" to primary,
        "android.ongoingActivityNoti.nowbarSecondaryInfo" to secondary
    )

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.nowbar_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.nowbar_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "card_nowbar_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_START = "com.monst.transfiranow.action.NOWBAR_START"
        const val ACTION_UPDATE = "com.monst.transfiranow.action.NOWBAR_UPDATE"
        const val ACTION_SET_QR_BITMAP = "com.monst.transfiranow.action.NOWBAR_SET_QR_BITMAP"
        const val ACTION_STOP = "com.monst.transfiranow.action.NOWBAR_STOP"

        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_USER_ROLE = "extra_user_role"
        const val EXTRA_VIEWS_COUNT = "extra_views_count"

        fun start(context: Context, userName: String, userRole: String, viewsCount: Int = 0) {
            val intent = Intent(context, CardNowbarService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_USER_NAME, userName)
                putExtra(EXTRA_USER_ROLE, userRole)
                putExtra(EXTRA_VIEWS_COUNT, viewsCount)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Diferencial: atualiza texto (ex.: contador de visualizações) sem recriar o Service.
         */
        fun update(context: Context, userName: String? = null, userRole: String? = null, viewsCount: Int? = null) {
            val intent = Intent(context, CardNowbarService::class.java).apply {
                action = ACTION_UPDATE
                userName?.let { putExtra(EXTRA_USER_NAME, it) }
                userRole?.let { putExtra(EXTRA_USER_ROLE, it) }
                viewsCount?.let { putExtra(EXTRA_VIEWS_COUNT, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Atualiza o QR Code via estado compartilhado (RemoteViews aceita Bitmap).
         */
        fun updateQrBitmap(context: Context, bitmap: Bitmap?) {
            CardNowbarState.qrBitmap = bitmap
            val intent = Intent(context, CardNowbarService::class.java).apply {
                action = ACTION_SET_QR_BITMAP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, CardNowbarService::class.java).apply { action = ACTION_STOP }
            )
        }
    }
}

private object NowbarIntents {
    fun openAppPendingIntent(context: Context) =
        androidx.core.app.TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(Intent(context, MainActivity::class.java))
            getPendingIntent(
                100,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
        }

    fun sharePendingIntent(context: Context, userName: String, userRole: String): android.app.PendingIntent {
        val intent = Intent(context, ShareCardReceiver::class.java).apply {
            action = ShareCardReceiver.ACTION_SHARE
            putExtra(ShareCardReceiver.EXTRA_USER_NAME, userName)
            putExtra(ShareCardReceiver.EXTRA_USER_ROLE, userRole)
        }

        return android.app.PendingIntent.getBroadcast(
            context,
            101,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }
}

object CardNowbarState {
    @Volatile
    var qrBitmap: Bitmap? = null
}
