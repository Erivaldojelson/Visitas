package com.monst.transfiranow.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.monst.transfiranow.data.VisitingCard
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object CardExchange {
    const val MIME_TYPE = "application/vnd.com.monst.transfiranow.card"

    private const val CARD_ENTRY = "card.json"
    private const val SCHEMA = "visitas-card"

    fun exportPackage(context: Context, card: VisitingCard): Uri {
        val dir = File(context.cacheDir, "visitas-card-share").apply { mkdirs() }
        val file = File(dir, "visitas-${safeFilePart(card.name.ifBlank { card.id })}.visitas-card")
        val photo = copyPhotoToTempFile(context, card.photoUri)
        val photoEntry = photo?.let { "photo.${it.extension}" }

        ZipOutputStream(FileOutputStream(file)).use { zip ->
            val payload = JSONObject()
                .put("schema", SCHEMA)
                .put("version", 1)
                .put("exportedAt", System.currentTimeMillis())
                .put("photoEntry", photoEntry)
                .put("card", card.toJson())

            zip.putNextEntry(ZipEntry(CARD_ENTRY))
            zip.write(payload.toString(2).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()

            if (photo != null && photoEntry != null) {
                zip.putNextEntry(ZipEntry(photoEntry))
                photo.file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }

        photo?.file?.delete()
        return file.toUri(context)
    }

    fun importPackage(context: Context, uri: Uri): VisitingCard? {
        val tempPackage = copyUriToTempFile(context, uri, "visitas-incoming", "visitas-card") ?: return null
        return try {
            ZipFile(tempPackage).use { zip ->
                val payloadEntry = zip.getEntry(CARD_ENTRY) ?: return null
                val payloadText = zip.getInputStream(payloadEntry).use { input ->
                    input.reader(StandardCharsets.UTF_8).readText()
                }
                val payload = JSONObject(payloadText)
                if (payload.optString("schema") != SCHEMA) return null

                val cardJson = payload.optJSONObject("card") ?: return null
                val photoEntryName = payload.optString("photoEntry").takeIf { it.isNotBlank() && it != "null" }
                val importedPhotoUri = photoEntryName
                    ?.let { zip.getEntry(it) }
                    ?.let { entry -> copyZipEntryToImportedPhoto(context, zip, entry) }

                cardJson.toCard(photoUri = importedPhotoUri.orEmpty())
            }
        } finally {
            tempPackage.delete()
        }
    }

    private data class TempPhoto(val file: File, val extension: String)

    private fun copyPhotoToTempFile(context: Context, rawUri: String): TempPhoto? {
        if (rawUri.isBlank()) return null
        val uri = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return null
        val extension = MediaImport.extensionForMime(context.contentResolver.getType(uri))
        val file = copyUriToTempFile(context, uri, "visitas-share-photo", extension) ?: return null
        return TempPhoto(file, extension)
    }

    private fun copyUriToTempFile(context: Context, uri: Uri, prefix: String, extension: String): File? {
        return runCatching {
            val dir = File(context.cacheDir, "visitas-exchange").apply { mkdirs() }
            val file = File(dir, "$prefix-${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            } ?: return null
            file
        }.getOrNull()
    }

    private fun copyZipEntryToImportedPhoto(context: Context, zip: ZipFile, entry: ZipEntry): String? {
        return runCatching {
            val extension = entry.name.substringAfterLast('.', "jpg").ifBlank { "jpg" }
            val dir = File(context.filesDir, "visitas-imported-photos").apply { mkdirs() }
            val file = File(dir, "imported-card-photo-${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension")
            zip.getInputStream(entry).use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            Uri.fromFile(file).toString()
        }.getOrNull()
    }

    private fun VisitingCard.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("name", name)
            .put("role", role)
            .put("phone", phone)
            .put("email", email)
            .put("instagram", instagram)
            .put("linkedin", linkedin)
            .put("website", website)
            .put("note", note)
            .put("photoUri", photoUri)
            .put("avatarEmoji", avatarEmoji)
            .put("walletPhotoUrl", walletPhotoUrl)
            .put("qrValue", qrValue)
            .put("passColor", passColor)
            .put("updatedAt", updatedAt)
    }

    private fun JSONObject.toCard(photoUri: String): VisitingCard {
        return VisitingCard(
            id = optString("id").ifBlank { UUID.randomUUID().toString() },
            name = optString("name"),
            role = optString("role"),
            phone = optString("phone"),
            email = optString("email"),
            instagram = optString("instagram"),
            linkedin = optString("linkedin"),
            website = optString("website"),
            note = optString("note"),
            photoUri = photoUri,
            avatarEmoji = optString("avatarEmoji", optString("emoji")),
            walletPhotoUrl = optString("walletPhotoUrl"),
            qrValue = optString("qrValue"),
            passColor = optString("passColor", optString("hexColor", "#1E3A8A")),
            updatedAt = optLong("updatedAt", System.currentTimeMillis())
        )
    }

    private fun safeFilePart(value: String): String {
        return value
            .lowercase()
            .replace(Regex("[^a-z0-9._-]+"), "-")
            .trim('-')
            .ifBlank { "cartao" }
            .take(48)
    }

    private fun File.toUri(context: Context): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            this
        )
    }
}
