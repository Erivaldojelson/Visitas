package com.monst.transfiranow.premium

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

@Composable
fun rememberQrBitmap(qrValue: String, size: Int): ImageBitmap? {
    return remember(qrValue, size) {
        decodeBase64Png(qrValue)?.asImageBitmap() ?: generateQrPng(qrValue, size)?.asImageBitmap()
    }
}

private fun decodeBase64Png(value: String): Bitmap? {
    val clean = value.substringAfter("base64,", value).trim()
    if (clean.isBlank()) return null
    val bytes = runCatching { Base64.decode(clean, Base64.DEFAULT) }.getOrNull() ?: return null
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

private fun generateQrPng(value: String, size: Int): Bitmap? = runCatching {
    val matrix = MultiFormatWriter().encode(
        value,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(EncodeHintType.MARGIN to 1)
    )
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
}.getOrNull()

