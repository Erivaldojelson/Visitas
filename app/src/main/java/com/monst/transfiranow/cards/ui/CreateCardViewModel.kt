package com.monst.transfiranow.cards.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.monst.transfiranow.cards.data.CardsRepository
import com.monst.transfiranow.cards.model.CreateCardRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateCardViewModel(private val repository: CardsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreateCardUiState())
    val uiState: StateFlow<CreateCardUiState> = _uiState

    fun createCard(
        name: String,
        photo: String,
        instagram: String?,
        whatsapp: String?,
        website: String?
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null, createdCard = null) }
        viewModelScope.launch {
            runCatching {
                repository.createCard(
                    CreateCardRequest(
                        name = name.trim(),
                        photo = photo.trim(),
                        instagram = instagram?.trim().orEmpty().ifBlank { null },
                        whatsapp = whatsapp?.trim().orEmpty().ifBlank { null },
                        website = website?.trim().orEmpty().ifBlank { null }
                    )
                )
            }.onSuccess { created ->
                _uiState.update { it.copy(isLoading = false, createdCard = created) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, errorMessage = err.message ?: "Erro ao criar cartão.") }
            }
        }
    }

    class Factory(private val repository: CardsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CreateCardViewModel::class.java)) {
                return CreateCardViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

