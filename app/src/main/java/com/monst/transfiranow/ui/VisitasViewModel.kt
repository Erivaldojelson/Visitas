package com.monst.transfiranow.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.monst.transfiranow.data.CardDraft
import com.monst.transfiranow.data.CardStore
import com.monst.transfiranow.data.CardsUiState
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.wallet.WalletJwtClient
import com.monst.transfiranow.wallet.WalletPassBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VisitasViewModel(application: Application) : AndroidViewModel(application) {
    private val store = CardStore(application)
    private val walletJwtClient = WalletJwtClient()
    private val _uiState = MutableStateFlow(CardsUiState())
    val uiState: StateFlow<CardsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.uiStateFlow.collect { persisted ->
                _uiState.update {
                    it.copy(
                        cards = persisted.cards,
                        walletIssuerId = persisted.walletIssuerId,
                        walletClassSuffix = persisted.walletClassSuffix,
                        walletBackendUrl = persisted.walletBackendUrl
                    )
                }
            }
        }
    }

    fun updateDraft(transform: (CardDraft) -> CardDraft) {
        _uiState.update { it.copy(draft = transform(it.draft)) }
    }

    fun editCard(card: VisitingCard) {
        _uiState.update {
            it.copy(
                draft = CardDraft(
                    id = card.id,
                    name = card.name,
                    role = card.role,
                    phone = card.phone,
                    email = card.email,
                    instagram = card.instagram,
                    linkedin = card.linkedin,
                    website = card.website,
                    note = card.note,
                    hexColor = card.hexColor
                ),
                statusMessage = "Editando ${card.name}."
            )
        }
    }

    fun clearDraft() {
        _uiState.update { it.copy(draft = CardDraft()) }
    }

    fun updateWalletSettings(issuerId: String? = null, classSuffix: String? = null, backendUrl: String? = null) {
        _uiState.update {
            it.copy(
                walletIssuerId = issuerId ?: it.walletIssuerId,
                walletClassSuffix = classSuffix ?: it.walletClassSuffix,
                walletBackendUrl = backendUrl ?: it.walletBackendUrl
            )
        }
    }

    fun setWalletAvailability(available: Boolean) {
        _uiState.update { it.copy(canUseGoogleWallet = available) }
    }

    fun saveDraft() {
        val draft = _uiState.value.draft
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Preencha pelo menos o nome do cartão.") }
            return
        }
        viewModelScope.launch {
            store.saveCard(draft)
            _uiState.update {
                it.copy(
                    draft = CardDraft(hexColor = it.draft.hexColor),
                    statusMessage = "Cartão salvo no app."
                )
            }
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            store.deleteCard(id)
            _uiState.update { it.copy(statusMessage = "Cartão removido.") }
        }
    }

    fun persistWalletSettings() {
        val state = _uiState.value
        viewModelScope.launch {
            store.persistSettings(state.walletIssuerId, state.walletClassSuffix, state.walletBackendUrl)
            _uiState.update { it.copy(statusMessage = "Configuração do Google Wallet salva.") }
        }
    }

    fun prepareWalletJwt(card: VisitingCard, onReady: (String) -> Unit) {
        val state = _uiState.value
        if (state.walletIssuerId.isBlank() || state.walletBackendUrl.isBlank()) {
            _uiState.update {
                it.copy(statusMessage = "Configure o issuerId e a URL do backend que assina o JWT do Google Wallet.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingToWallet = true, statusMessage = "Gerando passe para o Google Wallet...") }
            val payload = WalletPassBuilder.buildUnsignedPayload(
                issuerId = state.walletIssuerId,
                classSuffix = state.walletClassSuffix.ifBlank { "visitas_card" },
                card = card
            )
            val result = walletJwtClient.fetchSignedJwt(state.walletBackendUrl, payload)
            result.onSuccess { jwt ->
                _uiState.update { it.copy(isSavingToWallet = false, statusMessage = "JWT assinado recebido. Abrindo Google Wallet...") }
                onReady(jwt)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingToWallet = false,
                        statusMessage = error.message ?: "Falha ao criar o JWT do Google Wallet."
                    )
                }
            }
        }
    }

    fun onWalletSaveResult(message: String) {
        _uiState.update { it.copy(statusMessage = message, isSavingToWallet = false) }
    }
}
