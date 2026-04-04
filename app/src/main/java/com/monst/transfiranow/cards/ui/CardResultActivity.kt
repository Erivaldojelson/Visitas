package com.monst.transfiranow.cards.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import coil.load
import com.monst.transfiranow.R
import com.monst.transfiranow.databinding.ActivityCardResultBinding

class CardResultActivity : ComponentActivity() {
    private lateinit var binding: ActivityCardResultBinding

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
        binding.qrImage.setImageBitmap(decodeBase64ToBitmap(qrBase64))
    }

    private fun decodeBase64ToBitmap(value: String): Bitmap? {
        val clean = value.substringAfter("base64,", value).trim()
        if (clean.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    companion object {
        const val EXTRA_NAME = "extra_name"
        const val EXTRA_PHOTO_URL = "extra_photo_url"
        const val EXTRA_QR_BASE64 = "extra_qr_base64"
    }
}

