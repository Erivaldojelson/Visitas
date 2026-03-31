package com.monst.transfiranow

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.monst.transfiranow.data.AppDownloadManager
import com.monst.transfiranow.data.TransferRepository
import com.monst.transfiranow.service.TransferMonitorService
import com.monst.transfiranow.ui.TransfiraNowApp

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            startMonitorService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        startMonitorService()

        setContent {
            TransfiraNowApp(
                onEnsureMonitor = { startMonitorService() },
                onStartDownload = { url ->
                    val result = AppDownloadManager.enqueue(this, url)
                    result.onSuccess {
                        startMonitorService()
                        TransferRepository.setDownloadUrl("")
                    }.onFailure { error ->
                        TransferRepository.setHelperMessage(
                            error.message ?: "Não foi possível iniciar o download."
                        )
                    }
                }
            )
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun startMonitorService() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, TransferMonitorService::class.java)
        )
    }
}
