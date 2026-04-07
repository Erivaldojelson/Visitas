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
import com.google.android.gms.pay.Pay
import com.google.android.gms.pay.PayClient
import com.google.android.gms.pay.PayClient.SavePassesResult
import com.monst.transfiranow.ui.VisitasViewModel
import com.monst.transfiranow.ui.theme.TransfiraNowTheme
import com.monst.transfiranow.util.parseColor

class PremiumCardsActivity : FragmentActivity() {
    private lateinit var walletClient: PayClient
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
        walletClient = Pay.getClient(this)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            TransfiraNowTheme(dynamicColor = true, accentColor = parseColor(uiState.draft.passColor)) {
                AppLockGate(enabled = uiState.appLockEnabled) {
                    PremiumNavHost(
                        viewModel = viewModel,
                        onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                        onPickQrCode = { qrPicker.launch(arrayOf("image/*")) },
                        onSaveToWallet = { card ->
                            viewModel.prepareWalletJwt(card) { jwt ->
                                walletClient.savePassesJwt(jwt, this, ADD_TO_WALLET_REQUEST_CODE)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != ADD_TO_WALLET_REQUEST_CODE) return

        when (resultCode) {
            RESULT_OK -> viewModel.onWalletSaveResult("Cartão salvo no Google Wallet com sucesso.")
            RESULT_CANCELED -> viewModel.onWalletSaveResult("Operação cancelada no Google Wallet.")
            SavePassesResult.SAVE_ERROR -> {
                val rawMessage = data?.getStringExtra(PayClient.EXTRA_API_ERROR_MESSAGE).orEmpty()
                val extras = data?.extras
                val debugExtras = extras?.keySet()?.sorted()?.joinToString(prefix = " extras=[", postfix = "]")
                val message = rawMessage.ifBlank { "Erro ao salvar no Google Wallet." } + (debugExtras ?: "")
                viewModel.onWalletSaveResult(message)
            }
            else -> viewModel.onWalletSaveResult("Falha inesperada ao abrir o Google Wallet.")
        }
    }

    companion object {
        private const val ADD_TO_WALLET_REQUEST_CODE = 2402
    }
}
