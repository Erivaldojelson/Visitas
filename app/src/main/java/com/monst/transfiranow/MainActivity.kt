package com.monst.transfiranow

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.pay.Pay
import com.google.android.gms.pay.PayApiAvailabilityStatus
import com.google.android.gms.pay.PayClient
import com.monst.transfiranow.ui.VisitasApp
import com.monst.transfiranow.ui.VisitasViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.RGBLuminanceSource

class MainActivity : ComponentActivity() {
    private lateinit var walletClient: PayClient
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
        val bitmap = runCatching {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()

        if (bitmap == null) {
            Toast.makeText(this, "Não foi possível ler a imagem do QR Code.", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }

        val decoded = decodeQrFromBitmap(bitmap)
        if (decoded == null) {
            Toast.makeText(this, "A imagem selecionada não parece ser um QR Code válido.", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }

        viewModel.updateDraftQrValue(decoded)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        walletClient = Pay.getClient(this)
        checkWalletAvailability()

        setContent {
            VisitasApp(
                viewModel = viewModel,
                onPickPhoto = { photoPicker.launch(arrayOf("image/*")) },
                onPickQrCode = { qrPicker.launch(arrayOf("image/*")) },
                onSaveToWallet = { card ->
                    viewModel.prepareWalletJwt(card) { jwt ->
                        walletClient.savePassesJwt(jwt, this, ADD_TO_WALLET_REQUEST_CODE)
                    }
                }
            )
        }
    }

    private fun checkWalletAvailability() {
        walletClient
            .getPayApiAvailabilityStatus(PayClient.RequestType.SAVE_PASSES)
            .addOnSuccessListener { status ->
                viewModel.setWalletAvailability(status == PayApiAvailabilityStatus.AVAILABLE)
            }
            .addOnFailureListener {
                viewModel.setWalletAvailability(false)
                viewModel.onWalletSaveResult("Google Wallet não está disponível neste dispositivo.")
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != ADD_TO_WALLET_REQUEST_CODE) return

        when (resultCode) {
            RESULT_OK -> viewModel.onWalletSaveResult("Cartão salvo no Google Wallet com sucesso.")
            RESULT_CANCELED -> viewModel.onWalletSaveResult("Operação cancelada no Google Wallet.")
            PayClient.SavePassesResult.SAVE_ERROR -> {
                val message = data?.getStringExtra(PayClient.EXTRA_API_ERROR_MESSAGE)
                viewModel.onWalletSaveResult(message ?: "Erro ao salvar no Google Wallet.")
            }
            else -> viewModel.onWalletSaveResult("Falha inesperada ao abrir o Google Wallet.")
        }
    }

    companion object {
        private const val ADD_TO_WALLET_REQUEST_CODE = 2401
    }
}

private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
    val input = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
    val width = input.width
    val height = input.height
    val pixels = IntArray(width * height)
    input.getPixels(pixels, 0, width, 0, 0, width, height)

    val source = RGBLuminanceSource(width, height, pixels)
    val binary = BinaryBitmap(HybridBinarizer(source))

    return runCatching {
        val result = MultiFormatReader().decode(binary)
        if (result.barcodeFormat != BarcodeFormat.QR_CODE) return@runCatching null
        result.text
    }.getOrNull()
}
