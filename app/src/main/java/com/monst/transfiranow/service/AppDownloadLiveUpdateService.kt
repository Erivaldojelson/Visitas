package com.monst.transfiranow.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.getSystemService
import com.monst.transfiranow.data.AppDownloadManager
import com.monst.transfiranow.data.TransferRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppDownloadLiveUpdateService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationHelper: NotificationHelper
    private var pollingJob: Job? = null
    private var notificationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        NotificationHelper.createNotificationChannel(this)
        startForeground(NotificationHelper.NOTIFICATION_ID + 1, buildNotification())

        pollingJob = scope.launch {
            while (true) {
                AppDownloadManager.poll(this@AppDownloadLiveUpdateService)
                delay(750)
            }
        }

        notificationJob = scope.launch {
            TransferRepository.uiState.collectLatest {
                val manager = getSystemService<NotificationManager>() ?: return@collectLatest
                val topAppManaged = TransferRepository.topAppManagedActiveEntry()
                if (topAppManaged == null) {
                    stopSelf()
                    return@collectLatest
                }
                manager.notify(NotificationHelper.NOTIFICATION_ID + 1, buildNotification())
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        pollingJob?.cancel()
        notificationJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification() = notificationHelper.buildNotification(
        entry = TransferRepository.topAppManagedActiveEntry(),
        accentColor = TransferRepository.uiState.value.accentColor.toArgb(),
        canPromote = canRequestPromotedOngoing(),
        mode = NotificationHelper.Mode.AppManagedLive
    )

    private fun canRequestPromotedOngoing(): Boolean {
        return if (Build.VERSION.SDK_INT >= 36) {
            runCatching {
                val notificationManager = getSystemService(NotificationManager::class.java)
                NotificationManager::class.java
                    .getMethod("canPostPromotedNotifications")
                    .invoke(notificationManager) as Boolean
            }.getOrDefault(false)
        } else {
            false
        }
    }
}
