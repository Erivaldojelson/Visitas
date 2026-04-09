package com.monst.transfiranow

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.monst.transfiranow.ui.VisitasApp
import com.monst.transfiranow.ui.VisitasViewModel

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
                onPickQrCode = { qrPicker.launch(arrayOf("image/*")) }
            )
        }
    }
}
