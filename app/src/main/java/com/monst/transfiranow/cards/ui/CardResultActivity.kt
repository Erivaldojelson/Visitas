package com.monst.transfiranow.cards.ui

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import coil.load
import com.monst.transfiranow.R
import com.monst.transfiranow.databinding.ActivityCardResultBinding
import com.monst.transfiranow.service.CardNowbarService

class CardResultActivity : ComponentActivity() {
    private lateinit var binding: ActivityCardResultBinding

    private var pendingNowbarName: String? = null
    private var pendingNowbarQr: Bitmap? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) return@registerForActivityResult
            val name = pendingNowbarName ?: return@registerForActivityResult
            startNowbar(name, pendingNowbarQr)
            pendingNowbarName = null
            pendingNowbarQr = null
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setTitle(R.string.card_result_title)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val photo = intent.getStringExtra(EXTRA_PHOTO_URL).orEmpty()
        val qrBase64 = intent.getStringExtra(EXTRA_QR_BASE64).orEmpty()

        binding.nameText.text = name
        binding.photoImage.load(photo) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_foreground)
            error(R.drawable.ic_launcher_foreground)
        }
        val qrBitmap = decodeBase64ToBitmap(qrBase64)
        binding.qrImage.setImageBitmap(qrBitmap)

        maybeStartNowbar(name, qrBitmap)
    }

    private fun decodeBase64ToBitmap(value: String): Bitmap? {
        val clean = value.substringAfter("base64,", value).trim()
        if (clean.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    private fun maybeStartNowbar(name: String, qrBitmap: Bitmap?) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pendingNowbarName = name
            pendingNowbarQr = qrBitmap
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return

        startNowbar(name, qrBitmap)
    }

    private fun startNowbar(name: String, qrBitmap: Bitmap?) {
        val viewsCount = incrementNowbarViewsCount()
        CardNowbarService.update(
            context = this,
            userName = name,
            userRole = getString(R.string.nowbar_default_role),
            viewsCount = viewsCount
        )
        CardNowbarService.updateQrBitmap(this, qrBitmap)
    }

    private fun incrementNowbarViewsCount(): Int {
        val prefs = getSharedPreferences("nowbar_prefs", MODE_PRIVATE)
        val next = prefs.getInt("views_count", 0) + 1
        prefs.edit().putInt("views_count", next).apply()
        return next
    }

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_PHOTO_URL = "extra_photo_url"
        const val EXTRA_QR_BASE64 = "extra_qr_base64"
    }
}
