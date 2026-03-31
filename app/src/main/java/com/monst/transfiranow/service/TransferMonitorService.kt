package com.monst.transfiranow.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.content.getSystemService
import com.monst.transfiranow.data.AppDownloadManager
import com.monst.transfiranow.data.TransferRepository
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransferMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationHelper: NotificationHelper
    private var monitorJob: Job? = null
    private var pollingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        NotificationHelper.createNotificationChannel(this)
        startForeground(NOTIFICATION_ID, buildNotification())
        TransferRepository.refreshPermissionState(this)
        pollingJob = serviceScope.launch {
            while (true) {
                AppDownloadManager.poll(this@TransferMonitorService)
                delay(1_000)
            }
        }
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
        pollingJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val uiState = TransferRepository.uiState.value
        val top = TransferRepository.topActiveEntry()
        val colorInt = uiState.accentColor.toArgb()
        return notificationHelper.buildNotification(
            entry = top,
            accentColor = colorInt,
            canPromote = canRequestPromotedOngoing()
        )
    }

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

    companion object {
        private const val NOTIFICATION_ID = NotificationHelper.NOTIFICATION_ID
    }
}
