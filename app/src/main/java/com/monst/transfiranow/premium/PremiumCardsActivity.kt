package com.monst.transfiranow.premium

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import com.monst.transfiranow.ui.AppLockGate
import com.monst.transfiranow.ui.VisitasViewModel
import com.monst.transfiranow.ui.theme.TransfiraNowTheme
import com.monst.transfiranow.util.parseColor

class PremiumCardsActivity : FragmentActivity() {
    private val viewModel: VisitasViewModel by viewModels()

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        viewModel.updateDraftPhoto(uri.toString())
    }

    private val qrPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.updateDraftQrFromImage(contentResolver, uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val darkTheme = when (uiState.themeMode) {
                com.monst.transfiranow.data.AppThemeMode.LIGHT -> false
                com.monst.transfiranow.data.AppThemeMode.DARK -> true
                else -> systemDark
            }
            TransfiraNowTheme(
                dynamicColor = uiState.dynamicColorEnabled,
                accentColor = parseColor(uiState.draft.passColor),
                darkTheme = darkTheme,
                pureBlack = uiState.pureBlackThemeEnabled
            ) {
                AppLockGate(enabled = uiState.appLockEnabled) {
                    PremiumNavHost(
                        viewModel = viewModel,
                        onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                        onPickQrCode = { qrPicker.launch(arrayOf("image/*")) },
                        onClose = { finish() }
                    )
                }
            }
        }

        window.apply {
            WindowCompat.setDecorFitsSystemWindows(this, false)
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }
}
