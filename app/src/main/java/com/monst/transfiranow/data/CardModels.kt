package com.monst.transfiranow.data

data class VisitingCard(
    val id: String,
    val name: String,
    val role: String,
    val phone: String,
    val email: String,
    val instagram: String,
    val linkedin: String,
    val website: String,
    val note: String,
    val hexColor: String
)

data class CardDraft(
    val id: String? = null,
    val name: String = "",
    val role: String = "",
    val phone: String = "",
    val email: String = "",
    val instagram: String = "",
    val linkedin: String = "",
    val website: String = "",
    val note: String = "",
    val hexColor: String = "#1E3A8A"
)

data class CardsUiState(
    val cards: List<VisitingCard> = emptyList(),
    val draft: CardDraft = CardDraft(),
    val walletIssuerId: String = "",
    val walletClassSuffix: String = "visitas_card",
    val walletBackendUrl: String = "",
    val canUseGoogleWallet: Boolean = false,
    val statusMessage: String = "Crie um cartão, salve no app e depois envie para o Google Wallet.",
    val isSavingToWallet: Boolean = false
)
