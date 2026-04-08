package com.monst.transfiranow.premium

import android.content.Intent
import android.net.Uri
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
            TransfiraNowTheme(dynamicColor = true, accentColor = parseColor(uiState.draft.passColor)) {
                AppLockGate(enabled = uiState.appLockEnabled) {
                    PremiumNavHost(
                        viewModel = viewModel,
                        onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                        onPickQrCode = { qrPicker.launch(arrayOf("image/*")) },
                        onSaveToWallet = { card ->
                            viewModel.prepareWalletSaveUrl(card) { url ->
                                runCatching {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }.onFailure {
                                    viewModel.onWalletSaveResult("Não foi possível abrir o Google Wallet.")
                                }
                            }
                        },
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
