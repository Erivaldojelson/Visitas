package com.monst.transfiranow.service

import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.monst.transfiranow.data.TransferRepository

class DownloadNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        TransferRepository.refreshPermissionState(this)
        activeNotifications?.forEach { handleNotification(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handleNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        TransferRepository.remove("${sbn.packageName}:${sbn.id}")
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return
        val appName = runCatching {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(sbn.packageName)

        TransferRepository.maybeCaptureNotification(
            id = "${sbn.packageName}:${sbn.id}",
            appName = appName,
            notification = sbn.notification
        )
    }
}
