package com.monst.transfiranow.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.monst.transfiranow.data.VisitingCard
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.min

object CardExport {
    fun exportPng(context: Context, card: VisitingCard): Uri {
        val bitmap = renderShareBitmap(context, card)
        val file = File(context.cacheDir, "visitas-card-${card.id}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file.toUri(context)
    }

    fun exportPdf(context: Context, card: VisitingCard): Uri {
        val bitmap = renderShareBitmap(context, card)
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = doc.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        doc.finishPage(page)

        val file = File(context.cacheDir, "visitas-card-${card.id}.pdf")
        FileOutputStream(file).use { out ->
            doc.writeTo(out)
        }
        doc.close()
        return file.toUri(context)
    }

    fun exportVcf(context: Context, card: VisitingCard): Uri {
        val vcf = buildVCard(card)
        val file = File(context.cacheDir, "visitas-${card.id}.vcf")
        file.writeText(vcf, StandardCharsets.UTF_8)
        return file.toUri(context)
    }

    fun buildVCard(card: VisitingCard): String {
        val lines = buildList {
            add("BEGIN:VCARD")
            add("VERSION:3.0")
            card.name.trim().takeIf { it.isNotBlank() }?.let { add("FN:${escapeVCard(it)}") }
            card.role.trim().takeIf { it.isNotBlank() }?.let { add("TITLE:${escapeVCard(it)}") }
            card.phone.trim().takeIf { it.isNotBlank() }?.let { add("TEL;TYPE=CELL:${escapeVCard(it)}") }
            card.email.trim().takeIf { it.isNotBlank() }?.let { add("EMAIL:${escapeVCard(it)}") }
            card.website.trim().takeIf { it.isNotBlank() }?.let { add("URL:${escapeVCard(it)}") }
            card.note.trim().takeIf { it.isNotBlank() }?.let { add("NOTE:${escapeVCard(it)}") }

            val socials = buildList {
                card.instagram.trim().takeIf { it.isNotBlank() }?.let { add("Instagram: $it") }
                card.linkedin.trim().takeIf { it.isNotBlank() }?.let { add("LinkedIn: $it") }
            }.joinToString(" • ")

            if (socials.isNotBlank()) {
                add("NOTE:${escapeVCard(socials)}")
            }

            add("END:VCARD")
        }

        return lines.joinToString(separator = "\n", postfix = "\n")
    }

    private fun escapeVCard(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace(";", "\\;")
            .replace(",", "\\,")
    }

    private fun renderShareBitmap(context: Context, card: VisitingCard): Bitmap {
        val width = 1080
        val height = 1350
        val padding = 72f

        val accent = parseColor(card.passColor, fallback = Color.parseColor("#0F766E"))
        val accentDark = adjustBrightness(accent, 0.55f)
        val surface = Color.parseColor("#0B1220")

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                intArrayOf(accent, accentDark, surface),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cardRect = RectF(padding, padding, width - padding, height - padding)
        val radius = 84f

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 0, 0, 0)
        }
        canvas.drawRoundRect(
            RectF(cardRect.left + 10f, cardRect.top + 12f, cardRect.right + 10f, cardRect.bottom + 12f),
            radius,
            radius,
            shadowPaint
        )

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                cardRect.left,
                cardRect.top,
                cardRect.right,
                cardRect.bottom,
                intArrayOf(Color.argb(235, 255, 255, 255), Color.argb(225, 230, 255, 255)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(cardRect, radius, radius, cardPaint)

        val inner = RectF(cardRect.left + 28f, cardRect.top + 28f, cardRect.right - 28f, cardRect.bottom - 28f)
        val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                inner.left,
                inner.top,
                inner.right,
                inner.bottom,
                intArrayOf(accent, adjustBrightness(accent, 1.12f)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(inner, radius * 0.82f, radius * 0.82f, innerPaint)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 78f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(235, 255, 255, 255)
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 255, 255, 255)
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 255, 255, 255)
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.08f
        }

        val avatarSize = 210f
        val avatarLeft = inner.left + 54f
        val avatarTop = inner.top + 62f
        val avatarRect = RectF(avatarLeft, avatarTop, avatarLeft + avatarSize, avatarTop + avatarSize)

        val avatarBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) }
        canvas.drawOval(avatarRect, avatarBg)

        loadBitmapFromUri(context, card.photoUri)?.let { photo ->
            val dst = RectF(avatarRect)
            val src = centerCrop(photo, dst.width().toInt(), dst.height().toInt())
            val clipPath = Path().apply { addOval(dst, Path.Direction.CW) }
            val checkpoint = canvas.save()
            canvas.clipPath(clipPath)
            canvas.drawBitmap(src, null, dst, Paint(Paint.ANTI_ALIAS_FLAG))
            canvas.restoreToCount(checkpoint)
        } ?: run {
            val emoji = card.avatarEmoji.trim()
            val initial = card.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "V"
            val text = emoji.ifBlank { initial }
            val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = if (emoji.isNotBlank()) 110f else 92f
                typeface = Typeface.create(Typeface.DEFAULT, if (emoji.isNotBlank()) Typeface.NORMAL else Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val x = avatarRect.centerX()
            val y = avatarRect.centerY() - (initialPaint.descent() + initialPaint.ascent()) / 2
            canvas.drawText(text, x, y, initialPaint)
        }

        val titleLeft = avatarRect.right + 42f
        val titleTop = avatarRect.top + 62f
        val maxTitleWidth = inner.right - titleLeft - 54f

        val name = card.name.trim().ifBlank { "Sem nome" }
        canvas.drawText(ellipsize(name, titlePaint, maxTitleWidth), titleLeft, titleTop, titlePaint)

        val role = card.role.trim().ifBlank { "Cartão pessoal" }
        canvas.drawText(ellipsize(role, subtitlePaint, maxTitleWidth), titleLeft, titleTop + 64f, subtitlePaint)

        val detailsTop = avatarRect.bottom + 86f
        var y = detailsTop

        fun drawDetail(label: String, value: String) {
            if (value.isBlank()) return
            canvas.drawText(label.uppercase(), inner.left + 54f, y, labelPaint)
            y += 44f
            canvas.drawText(ellipsize(value, linePaint, inner.width() - 108f), inner.left + 54f, y, linePaint)
            y += 74f
        }

        drawDetail("Telefone", card.phone.trim())
        drawDetail("Email", card.email.trim())
        drawDetail("Website", card.website.trim())

        val qrValue = card.qrValue.trim().ifBlank { card.website.trim() }
        if (qrValue.isNotBlank()) {
            val qrSize = 300f
            val qrBox = RectF(inner.right - qrSize - 54f, inner.bottom - qrSize - 54f, inner.right - 54f, inner.bottom - 54f)
            val qrBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(qrBox, 54f, 54f, qrBg)

            val qrBitmap = generateQrBitmap(qrValue, 512)
            val inset = 28f
            val dst = RectF(qrBox.left + inset, qrBox.top + inset, qrBox.right - inset, qrBox.bottom - inset)
            canvas.drawBitmap(qrBitmap, null, dst, Paint(Paint.ANTI_ALIAS_FLAG))
        }

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 255, 255, 255)
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText("VISITAS", inner.left + 54f, inner.bottom - 64f, brandPaint)

        return bitmap
    }

    private fun generateQrBitmap(value: String, size: Int): Bitmap {
        val matrix = MultiFormatWriter().encode(
            value,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(EncodeHintType.MARGIN to 1)
        )
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        }
    }

    private fun parseColor(value: String, fallback: Int): Int {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return fallback
        return runCatching { Color.parseColor(trimmed) }.getOrDefault(fallback)
    }

    private fun adjustBrightness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(255, hsv)
    }

    private fun loadBitmapFromUri(context: Context, raw: String): Bitmap? {
        if (raw.isBlank()) return null
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }

    private fun centerCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val width = source.width.toFloat()
        val height = source.height.toFloat()
        val scale = maxOf(targetWidth / width, targetHeight / height)

        val scaledWidth = width * scale
        val scaledHeight = height * scale

        val left = (scaledWidth - targetWidth) / 2f
        val top = (scaledHeight - targetHeight) / 2f

        val matrix = android.graphics.Matrix().apply {
            postScale(scale, scale)
            postTranslate(-left, -top)
        }

        return Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888).also { out ->
            Canvas(out).drawBitmap(source, matrix, Paint(Paint.ANTI_ALIAS_FLAG))
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val target = maxWidth - paint.measureText(ellipsis)
        if (target <= 0f) return ellipsis

        var end = min(text.length, 64)
        while (end > 0 && paint.measureText(text, 0, end) > target) {
            end--
        }
        return text.take(end).trimEnd() + ellipsis
    }

    private fun File.toUri(context: Context): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            this
        )
    }
}
