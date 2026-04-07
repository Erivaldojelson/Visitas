package com.monst.transfiranow.share

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.monst.transfiranow.data.VisitingCard
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID

object CardsBackup {
    fun exportJson(context: Context, cards: List<VisitingCard>): Uri {
        val payload = JSONObject()
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("cards", JSONArray().apply {
                cards.forEach { card ->
                    put(
                        JSONObject()
                            .put("id", card.id)
                            .put("name", card.name)
                            .put("role", card.role)
                            .put("phone", card.phone)
                            .put("email", card.email)
                            .put("instagram", card.instagram)
                            .put("linkedin", card.linkedin)
                            .put("website", card.website)
                            .put("note", card.note)
                            .put("photoUri", card.photoUri)
                            .put("walletPhotoUrl", card.walletPhotoUrl)
                            .put("qrValue", card.qrValue)
                            .put("passColor", card.passColor)
                            .put("updatedAt", card.updatedAt)
                    )
                }
            })

        val file = File(context.cacheDir, "visitas-backup.json")
        file.writeText(payload.toString(2), StandardCharsets.UTF_8)
        return file.toUri(context)
    }

    fun parseCards(raw: String): List<VisitingCard> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return emptyList()

        val array = runCatching {
            if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("cards") ?: JSONArray()
            }
        }.getOrNull() ?: return emptyList()

        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val id = item.optString("id").ifBlank { UUID.randomUUID().toString() }
                add(
                    VisitingCard(
                        id = id,
                        name = item.optString("name"),
                        role = item.optString("role"),
                        phone = item.optString("phone"),
                        email = item.optString("email"),
                        instagram = item.optString("instagram"),
                        linkedin = item.optString("linkedin"),
                        website = item.optString("website"),
                        note = item.optString("note"),
                        photoUri = item.optString("photoUri"),
                        walletPhotoUrl = item.optString("walletPhotoUrl"),
                        qrValue = item.optString("qrValue"),
                        passColor = item.optString("passColor", item.optString("hexColor", "#1E3A8A")),
                        updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
        }
    }

    private fun File.toUri(context: Context): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            this
        )
    }
}

