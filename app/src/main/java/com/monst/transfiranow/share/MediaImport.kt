package com.monst.transfiranow.share

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object MediaImport {
    fun copyPhotoToPrivateStorage(context: Context, source: Uri): String? {
        return runCatching {
            val resolver = context.contentResolver
            val extension = extensionForMime(resolver.getType(source))
            val dir = File(context.filesDir, "visitas-photos").apply { mkdirs() }
            val file = File(dir, "photo-${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension")

            resolver.openInputStream(source)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            } ?: return null

            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    internal fun extensionForMime(mimeType: String?): String {
        return when (mimeType?.lowercase()?.substringBefore(";")) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            "image/gif" -> "gif"
            "image/jpeg", "image/jpg" -> "jpg"
            else -> "jpg"
        }
    }
}
