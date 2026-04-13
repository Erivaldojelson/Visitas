package com.monst.transfiranow

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.pay.Pay
import com.google.android.gms.pay.PayApiAvailabilityStatus
import com.google.android.gms.pay.PayClient
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.share.CardExchange
import com.monst.transfiranow.share.MediaImport
import com.monst.transfiranow.ui.VisitasApp
import com.monst.transfiranow.ui.VisitasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private val viewModel: VisitasViewModel by viewModels()
    private lateinit var walletClient: PayClient
    private val photoPicker = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri ?: return@registerForActivityResult
        importPickedPhoto(uri)
    }

    private val qrPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        viewModel.updateDraftQrFromImage(contentResolver, uri)
    }

    companion object {
        private const val SAVE_TO_WALLET_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        walletClient = Pay.getClient(this)
        refreshWalletAvailability()

        setContent {
            VisitasApp(
                viewModel = viewModel,
                onPickPhoto = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onPickQrCode = { qrPicker.launch(arrayOf("image/*")) },
                onSaveToWallet = ::saveCardToGoogleWallet
            )
        }

        handleIncomingCardIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshWalletAvailability()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingCardIntent(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != SAVE_TO_WALLET_REQUEST_CODE) return

        val message = when (resultCode) {
            RESULT_OK -> "Cartão salvo no Google Wallet."
            RESULT_CANCELED -> "Salvamento no Google Wallet cancelado."
            else -> "Não foi possível concluir o salvamento no Google Wallet."
        }
        viewModel.onWalletSaveResult(message)
    }

    private fun importPickedPhoto(uri: Uri) {
        lifecycleScope.launch {
            val localPhotoUri = withContext(Dispatchers.IO) {
                MediaImport.copyPhotoToPrivateStorage(this@MainActivity, uri)
            }

            if (localPhotoUri == null) {
                Toast.makeText(this@MainActivity, "Não foi possível importar a foto.", Toast.LENGTH_LONG).show()
                return@launch
            }

            viewModel.updateDraftPhoto(localPhotoUri)
        }
    }

    private fun handleIncomingCardIntent(intent: Intent?) {
        intent ?: return
        val isExactCard = intent.type == CardExchange.MIME_TYPE ||
            intent.data?.lastPathSegment?.endsWith(".visitas-card", ignoreCase = true) == true
        if (!isExactCard) return

        val uri = when (intent.action) {
            Intent.ACTION_SEND -> intent.streamUri()
            Intent.ACTION_VIEW -> intent.data
            else -> null
        } ?: return

        viewModel.importExactCardFromUri(uri)
    }

    private fun refreshWalletAvailability() {
        walletClient.getPayApiAvailabilityStatus(PayClient.RequestType.SAVE_PASSES)
            .addOnSuccessListener { status ->
                viewModel.setWalletAvailability(status == PayApiAvailabilityStatus.AVAILABLE)
            }
            .addOnFailureListener {
                viewModel.setWalletAvailability(false)
            }
    }

    private fun saveCardToGoogleWallet(card: VisitingCard) {
        viewModel.prepareWalletSavePass(card) { issuedPass ->
            val openedNativeFlow = runCatching {
                if (!viewModel.uiState.value.canUseGoogleWallet) {
                    return@runCatching false
                }
                walletClient.savePassesJwt(issuedPass.jwt, this, SAVE_TO_WALLET_REQUEST_CODE)
                true
            }.getOrElse { false }

            if (!openedNativeFlow) {
                runCatching {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(issuedPass.url)))
                    viewModel.onWalletSaveResult("Abrindo o link do Google Wallet para concluir o salvamento.")
                }.onFailure { error ->
                    viewModel.onWalletSaveResult(
                        error.message ?: "Não foi possível abrir o fluxo do Google Wallet."
                    )
                }
            }
        }
    }

    private fun Intent.streamUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
    }
}
