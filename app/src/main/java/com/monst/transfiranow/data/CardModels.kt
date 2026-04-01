package com.monst.transfiranow.data

enum class AppLanguage(val code: String) {
    PT_BR("pt-BR"),
    PT_PT("pt-PT"),
    PT_AO("pt-AO"),
    EN("en"),
    ZH("zh");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: PT_BR
    }
}

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
    val photoUri: String,
    val walletPhotoUrl: String,
    val passColor: String,
    val updatedAt: Long
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
    val photoUri: String = "",
    val walletPhotoUrl: String = "",
    val passColor: String = "#1E3A8A"
)

data class CardsUiState(
    val cards: List<VisitingCard> = emptyList(),
    val draft: CardDraft = CardDraft(),
    val walletIssuerId: String = "",
    val walletClassSuffix: String = "visitas_card",
    val walletBackendUrl: String = "",
    val appLanguage: AppLanguage = AppLanguage.PT_BR,
    val canUseGoogleWallet: Boolean = false,
    val statusMessage: String = "Crie um passe, salve no app e depois envie para o Google Wallet.",
    val isSavingToWallet: Boolean = false
)
