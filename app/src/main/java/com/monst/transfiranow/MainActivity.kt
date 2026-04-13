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
import com.monst.transfiranow.share.CardExchange
import com.monst.transfiranow.share.MediaImport
import com.monst.transfiranow.ui.VisitasApp
import com.monst.transfiranow.ui.VisitasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
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
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VisitasApp(
                viewModel = viewModel,
                onPickPhoto = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onPickQrCode = { qrPicker.launch(arrayOf("image/*")) }
            )
        }

        handleIncomingCardIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingCardIntent(intent)
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

    private fun Intent.streamUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= 33) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
    }
}
