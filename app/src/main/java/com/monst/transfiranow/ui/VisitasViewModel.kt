package com.monst.transfiranow.ui

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.monst.transfiranow.data.AppLanguage
import com.monst.transfiranow.data.CardDraft
import com.monst.transfiranow.data.CardStore
import com.monst.transfiranow.data.CardsUiState
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.wallet.WalletJwtClient
import com.monst.transfiranow.wallet.WalletPassBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.RGBLuminanceSource
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
                        walletBackendUrl = persisted.walletBackendUrl,
                        appLanguage = persisted.appLanguage,
                        statusMessage = if (it.statusMessage == CardsUiState().statusMessage) {
                            message(persisted.appLanguage, "create_intro")
                        } else {
                            it.statusMessage
                        }
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
                    photoUri = card.photoUri,
                    walletPhotoUrl = card.walletPhotoUrl,
                    qrValue = card.qrValue,
                    passColor = card.passColor
                ),
                statusMessage = message(it.appLanguage, "editing", card.name)
            )
        }
    }

    fun clearDraft() {
        _uiState.update { it.copy(draft = CardDraft(), statusMessage = message(it.appLanguage, "draft_cleared")) }
    }

    fun updateDraftPhoto(photoUri: String) {
        _uiState.update {
            it.copy(
                draft = it.draft.copy(photoUri = photoUri),
                statusMessage = message(it.appLanguage, "photo_added")
            )
        }
    }

    fun updateDraftQrValue(qrValue: String) {
        _uiState.update {
            it.copy(
                draft = it.draft.copy(qrValue = qrValue),
                statusMessage = message(it.appLanguage, "qr_added")
            )
        }
    }

    fun clearDraftQr() {
        _uiState.update {
            it.copy(
                draft = it.draft.copy(qrValue = ""),
                statusMessage = message(it.appLanguage, "qr_cleared")
            )
        }
    }

    fun updateDraftQrFromImage(contentResolver: ContentResolver, uri: Uri) {
        val bitmap = runCatching {
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()

        if (bitmap == null) {
            Toast.makeText(getApplication(), "Não foi possível ler a imagem do QR Code.", Toast.LENGTH_LONG).show()
            return
        }

        val decoded = decodeQrFromBitmap(bitmap)
        if (decoded == null) {
            Toast.makeText(getApplication(), "A imagem selecionada não parece ser um QR Code válido.", Toast.LENGTH_LONG).show()
            return
        }

        updateDraftQrValue(decoded)
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

    fun updateLanguage(language: AppLanguage) {
        _uiState.update {
            it.copy(
                appLanguage = language,
                statusMessage = message(language, "language_changed")
            )
        }
        viewModelScope.launch {
            store.persistLanguage(language)
        }
    }

    fun setWalletAvailability(available: Boolean) {
        _uiState.update { it.copy(canUseGoogleWallet = available) }
    }

    fun saveDraft() {
        val state = _uiState.value
        val draft = state.draft
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(statusMessage = message(it.appLanguage, "need_name")) }
            return
        }
        viewModelScope.launch {
            store.saveCard(draft)
            _uiState.update {
                it.copy(
                    draft = CardDraft(passColor = it.draft.passColor),
                    statusMessage = message(it.appLanguage, "pass_saved")
                )
            }
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            store.deleteCard(id)
            _uiState.update { it.copy(statusMessage = message(it.appLanguage, "pass_deleted")) }
        }
    }

    fun persistWalletSettings() {
        val state = _uiState.value
        viewModelScope.launch {
            store.persistSettings(state.walletIssuerId, state.walletClassSuffix, state.walletBackendUrl)
            _uiState.update { it.copy(statusMessage = message(it.appLanguage, "wallet_saved")) }
        }
    }

    fun prepareWalletJwt(card: VisitingCard, onReady: (String) -> Unit) {
        val state = _uiState.value
        if (state.walletIssuerId.isBlank() || state.walletBackendUrl.isBlank()) {
            _uiState.update {
                it.copy(statusMessage = message(it.appLanguage, "wallet_missing"))
            }
            return
        }

        viewModelScope.launch {
            val issuerId = state.walletIssuerId.trim()
            val backendEndpoint = normalizeWalletBackendEndpoint(state.walletBackendUrl)
            _uiState.update {
                it.copy(
                    isSavingToWallet = true,
                    statusMessage = message(it.appLanguage, "wallet_generating")
                )
            }
            val payload = WalletPassBuilder.buildUnsignedPayload(
                issuerId = issuerId,
                classSuffix = state.walletClassSuffix.ifBlank { "visitas_card" },
                card = card
            )
            val result = walletJwtClient.fetchSignedJwt(backendEndpoint, payload)
            result.onSuccess { jwt ->
                _uiState.update {
                    it.copy(
                        isSavingToWallet = false,
                        statusMessage = message(it.appLanguage, "wallet_opening")
                    )
                }
                onReady(jwt)
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSavingToWallet = false,
                        statusMessage = error.message ?: message(it.appLanguage, "wallet_failed")
                    )
                }
            }
        }
    }

    private fun normalizeWalletBackendEndpoint(raw: String): String {
        val trimmed = raw.trim().removeSuffix("/")
        if (trimmed.isBlank()) return trimmed
        if (trimmed.contains("/wallet/sign")) return trimmed
        return if (trimmed.matches(Regex("^https?://[^/]+$"))) "$trimmed/wallet/sign" else trimmed
    }

    fun onWalletSaveResult(message: String) {
        _uiState.update { it.copy(statusMessage = message, isSavingToWallet = false) }
    }

    private fun message(language: AppLanguage, key: String, arg: String = ""): String {
        return when (language) {
            AppLanguage.PT_BR -> when (key) {
                "create_intro" -> "Crie um passe, salve no app e depois envie para o Google Wallet."
                "editing" -> "Editando $arg."
                "draft_cleared" -> "Campo de criação limpo."
                "photo_added" -> "Foto adicionada ao passe."
                "qr_added" -> "QR Code adicionado ao passe."
                "qr_cleared" -> "QR Code removido do passe."
                "language_changed" -> "Idioma atualizado."
                "need_name" -> "Preencha pelo menos o nome do passe."
                "pass_saved" -> "Passe salvo no app."
                "pass_deleted" -> "Passe removido."
                "wallet_saved" -> "Configuração do Google Wallet salva."
                "wallet_missing" -> "Configure o issuer ID e a URL do backend que assina o JWT do Google Wallet."
                "wallet_generating" -> "Gerando passe para o Google Wallet..."
                "wallet_opening" -> "JWT assinado recebido. Abrindo Google Wallet..."
                else -> "Falha ao criar o JWT do Google Wallet."
            }
            AppLanguage.PT_PT, AppLanguage.PT_AO -> when (key) {
                "create_intro" -> "Crie um passe, guarde na aplicação e depois envie para o Google Wallet."
                "editing" -> "A editar $arg."
                "draft_cleared" -> "Campo de criação limpo."
                "photo_added" -> "Fotografia adicionada ao passe."
                "qr_added" -> "QR Code adicionado ao passe."
                "qr_cleared" -> "QR Code removido do passe."
                "language_changed" -> "Idioma atualizado."
                "need_name" -> "Preencha pelo menos o nome do passe."
                "pass_saved" -> "Passe guardado na aplicação."
                "pass_deleted" -> "Passe removido."
                "wallet_saved" -> "Configuração do Google Wallet guardada."
                "wallet_missing" -> "Configure o issuer ID e o URL do backend que assina o JWT do Google Wallet."
                "wallet_generating" -> "A gerar passe para o Google Wallet..."
                "wallet_opening" -> "JWT assinado recebido. A abrir o Google Wallet..."
                else -> "Falha ao criar o JWT do Google Wallet."
            }
            AppLanguage.EN -> when (key) {
                "create_intro" -> "Create a pass, save it in the app, then send it to Google Wallet."
                "editing" -> "Editing $arg."
                "draft_cleared" -> "Create form cleared."
                "photo_added" -> "Photo added to the pass."
                "qr_added" -> "QR Code added to the pass."
                "qr_cleared" -> "QR Code removed from the pass."
                "language_changed" -> "Language updated."
                "need_name" -> "Fill in at least the pass name."
                "pass_saved" -> "Pass saved in the app."
                "pass_deleted" -> "Pass removed."
                "wallet_saved" -> "Google Wallet settings saved."
                "wallet_missing" -> "Set the issuer ID and backend URL that signs the Google Wallet JWT."
                "wallet_generating" -> "Generating pass for Google Wallet..."
                "wallet_opening" -> "Signed JWT received. Opening Google Wallet..."
                else -> "Failed to create the Google Wallet JWT."
            }
            AppLanguage.ZH -> when (key) {
                "create_intro" -> "创建通行证，保存到应用中，然后发送到 Google Wallet。"
                "editing" -> "正在编辑 $arg。"
                "draft_cleared" -> "创建表单已清空。"
                "photo_added" -> "照片已添加到通行证。"
                "qr_added" -> "二维码已添加到通行证。"
                "qr_cleared" -> "二维码已从通行证移除。"
                "language_changed" -> "语言已更新。"
                "need_name" -> "请至少填写通行证名称。"
                "pass_saved" -> "通行证已保存在应用中。"
                "pass_deleted" -> "通行证已删除。"
                "wallet_saved" -> "Google Wallet 设置已保存。"
                "wallet_missing" -> "请配置 issuer ID 和用于签名 Google Wallet JWT 的后端地址。"
                "wallet_generating" -> "正在为 Google Wallet 生成通行证..."
                "wallet_opening" -> "已收到签名 JWT，正在打开 Google Wallet..."
                else -> "创建 Google Wallet JWT 失败。"
            }
        }
    }
}

private fun decodeQrFromBitmap(bitmap: Bitmap): String? {
    val input = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
    val width = input.width
    val height = input.height
    val pixels = IntArray(width * height)
    input.getPixels(pixels, 0, width, 0, 0, width, height)

    val source = RGBLuminanceSource(width, height, pixels)
    val binary = BinaryBitmap(HybridBinarizer(source))

    return runCatching {
        val result = MultiFormatReader().decode(binary)
        if (result.barcodeFormat != BarcodeFormat.QR_CODE) return@runCatching null
        result.text
    }.getOrNull()
}
