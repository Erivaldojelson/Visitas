package com.monst.transfiranow.cards.ui

import com.monst.transfiranow.cards.model.CreateCardResponse

data class CreateCardUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val createdCard: CreateCardResponse? = null
)

