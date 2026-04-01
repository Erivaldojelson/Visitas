package com.monst.transfiranow.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "visitas_store")

class CardStore(private val context: Context) {
    private val cardsKey = stringPreferencesKey("cards_json")
    private val issuerKey = stringPreferencesKey("wallet_issuer_id")
    private val classSuffixKey = stringPreferencesKey("wallet_class_suffix")
    private val backendUrlKey = stringPreferencesKey("wallet_backend_url")

    val uiStateFlow: Flow<CardsUiState> = context.dataStore.data.map { preferences ->
        CardsUiState(
            cards = parseCards(preferences[cardsKey].orEmpty()),
            walletIssuerId = preferences[issuerKey].orEmpty(),
            walletClassSuffix = preferences[classSuffixKey] ?: "visitas_card",
            walletBackendUrl = preferences[backendUrlKey].orEmpty()
        )
    }

    suspend fun saveCard(draft: CardDraft) {
        val existing = loadCardsMutable()
        val id = draft.id ?: UUID.randomUUID().toString()
        val card = VisitingCard(
            id = id,
            name = draft.name.trim(),
            role = draft.role.trim(),
            phone = draft.phone.trim(),
            email = draft.email.trim(),
            instagram = draft.instagram.trim(),
            linkedin = draft.linkedin.trim(),
            website = draft.website.trim(),
            note = draft.note.trim(),
            hexColor = draft.hexColor
        )
        val updated = existing.filterNot { it.id == id } + card
        persistCards(updated.sortedBy { it.name.lowercase() })
    }

    suspend fun deleteCard(id: String) {
        val updated = loadCardsMutable().filterNot { it.id == id }
        persistCards(updated)
    }

    suspend fun persistSettings(issuerId: String, classSuffix: String, backendUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[issuerKey] = issuerId.trim()
            preferences[classSuffixKey] = classSuffix.trim().ifBlank { "visitas_card" }
            preferences[backendUrlKey] = backendUrl.trim()
        }
    }

    private suspend fun loadCardsMutable(): MutableList<VisitingCard> {
        return context.dataStore.data
            .map { parseCards(it[cardsKey].orEmpty()) }
            .first()
            .toMutableList()
    }

    private suspend fun persistCards(cards: List<VisitingCard>) {
        context.dataStore.edit { preferences ->
            val json = JSONArray()
            cards.forEach { card ->
                json.put(
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
                        .put("hexColor", card.hexColor)
                )
            }
            preferences[cardsKey] = json.toString()
        }
    }

    private fun parseCards(raw: String): List<VisitingCard> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        VisitingCard(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            role = item.optString("role"),
                            phone = item.optString("phone"),
                            email = item.optString("email"),
                            instagram = item.optString("instagram"),
                            linkedin = item.optString("linkedin"),
                            website = item.optString("website"),
                            note = item.optString("note"),
                            hexColor = item.optString("hexColor", "#1E3A8A")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
