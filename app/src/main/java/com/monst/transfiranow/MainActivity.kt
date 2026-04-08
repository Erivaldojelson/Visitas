package com.monst.transfiranow

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.FragmentActivity
import com.monst.transfiranow.ui.VisitasApp
import com.monst.transfiranow.ui.VisitasViewModel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: VisitasViewModel by viewModels()
    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        viewModel.updateDraftPhoto(uri.toString())
    }
    private val qrPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.updateDraftQrFromImage(contentResolver, uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VisitasApp(
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
                }
            )
        }

        handleWidgetActions(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetActions(intent)
    }

    private fun handleWidgetActions(intent: Intent?) {
        if (intent?.action != ACTION_WIDGET_SAVE_TO_WALLET) return

        val cardId = intent.getStringExtra(EXTRA_CARD_ID).orEmpty()
        if (cardId.isBlank()) {
            viewModel.onWalletSaveResult("Nenhum cartão selecionado para salvar no Wallet.")
            return
        }

        lifecycleScope.launch {
            val card = viewModel.getCardById(cardId)
            if (card == null) {
                viewModel.onWalletSaveResult("Cartão não encontrado para salvar no Wallet.")
                return@launch
            }
            viewModel.prepareWalletSaveUrl(card) { url ->
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }.onFailure {
                    viewModel.onWalletSaveResult("Não foi possível abrir o Google Wallet.")
                }
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_SAVE_TO_WALLET = "com.monst.transfiranow.action.WIDGET_SAVE_TO_WALLET"
        const val EXTRA_CARD_ID = "extra_card_id"
    }
}
