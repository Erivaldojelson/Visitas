package com.monst.transfiranow.premium

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.monst.transfiranow.share.MediaImport
import com.monst.transfiranow.ui.AppLockGate
import com.monst.transfiranow.ui.VisitasViewModel
import com.monst.transfiranow.ui.theme.TransfiraNowTheme
import com.monst.transfiranow.util.parseColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PremiumCardsActivity : FragmentActivity() {
    private val viewModel: VisitasViewModel by viewModels()

    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        importPickedPhoto(uri)
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
                        onPickPhoto = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
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

    private fun importPickedPhoto(uri: Uri) {
        lifecycleScope.launch {
            val localPhotoUri = withContext(Dispatchers.IO) {
                MediaImport.copyPhotoToPrivateStorage(this@PremiumCardsActivity, uri)
            }

            if (localPhotoUri == null) {
                Toast.makeText(this@PremiumCardsActivity, "Não foi possível importar a foto.", Toast.LENGTH_LONG).show()
                return@launch
            }

            viewModel.updateDraftPhoto(localPhotoUri)
        }
    }
}
