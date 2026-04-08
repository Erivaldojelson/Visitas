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
import com.monst.transfiranow.share.CardsBackup
import com.monst.transfiranow.util.VCardParser
import com.monst.transfiranow.widget.MyCardWidgetProvider
import com.monst.transfiranow.BuildConfig
import com.monst.transfiranow.wallet.WalletSaveUrlClient
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.RGBLuminanceSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.json.JSONObject
import java.util.UUID

class VisitasViewModel(application: Application) : AndroidViewModel(application) {
    private val store = CardStore(application)
    private val walletSaveUrlClient = WalletSaveUrlClient()
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
                        appLockEnabled = persisted.appLockEnabled,
                        notificationsEnabled = persisted.notificationsEnabled,
                        liveUpdatesEnabled = persisted.liveUpdatesEnabled,
                        eventModeEnabled = persisted.eventModeEnabled,
                        nowBarColor = persisted.nowBarColor,
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

    suspend fun getCardById(id: String): VisitingCard? {
        if (id.isBlank()) return null
        return store.uiStateFlow.first().cards.firstOrNull { it.id == id }
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
                    avatarEmoji = card.avatarEmoji,
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

        handleScannedContent(decoded)
    }

    fun scanQrFromBitmap(bitmap: Bitmap) {
        val decoded = decodeQrFromBitmap(bitmap)
        if (decoded == null) {
            Toast.makeText(getApplication(), "Não foi possível ler um QR Code com essa imagem.", Toast.LENGTH_LONG).show()
            return
        }
        handleScannedContent(decoded)
    }

    fun importVCardFromUri(uri: Uri) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }.orEmpty()
                }.getOrDefault("")
            }

            if (content.isBlank()) {
                _uiState.update { it.copy(statusMessage = "Não foi possível ler o arquivo vCard.") }
                return@launch
            }

            val parsed = VCardParser.parse(content)
            if (parsed == null || parsed.fullName.isBlank()) {
                _uiState.update { it.copy(statusMessage = "Esse arquivo não parece ser um vCard válido.") }
                return@launch
            }

            _uiState.update {
                it.copy(
                    draft = it.draft.copy(
                        name = parsed.fullName.ifBlank { it.draft.name },
                        role = parsed.title.ifBlank { it.draft.role },
                        phone = parsed.phone.ifBlank { it.draft.phone },
                        email = parsed.email.ifBlank { it.draft.email },
                        website = parsed.url.ifBlank { it.draft.website },
                        note = listOf(it.draft.note, parsed.note).filter { v -> v.isNotBlank() }.distinct().joinToString("\n\n")
                    ),
                    statusMessage = "vCard importado. Revise e toque em “Criar passe”."
                )
            }
        }
    }

    fun importBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().readText()
                    }.orEmpty()
                }.getOrDefault("")
            }

            if (content.isBlank()) {
                _uiState.update { it.copy(statusMessage = "Não foi possível ler o arquivo de backup.") }
                return@launch
            }

            val imported = CardsBackup.parseCards(content)
            if (imported.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "Nenhum cartão encontrado nesse backup.") }
                return@launch
            }

              store.mergeCards(imported)
              MyCardWidgetProvider.requestUpdate(getApplication())
              _uiState.update { it.copy(statusMessage = "Backup importado: ${imported.size} cartões mesclados.") }
          }
      }

    private fun handleScannedContent(content: String) {
        val trimmed = content.trim()
        if (trimmed.isBlank()) return

        VCardParser.parse(trimmed)?.let { parsed ->
            if (parsed.fullName.isNotBlank()) {
                _uiState.update {
                    it.copy(
                        draft = it.draft.copy(
                            name = parsed.fullName.ifBlank { it.draft.name },
                            role = parsed.title.ifBlank { it.draft.role },
                            phone = parsed.phone.ifBlank { it.draft.phone },
                            email = parsed.email.ifBlank { it.draft.email },
                            website = parsed.url.ifBlank { it.draft.website },
                            note = listOf(it.draft.note, parsed.note).filter { v -> v.isNotBlank() }.distinct().joinToString("\n\n"),
                            qrValue = it.draft.qrValue.ifBlank { parsed.url.ifBlank { it.draft.qrValue } }
                        ),
                        statusMessage = "QR com vCard lido. Revise e toque em “Criar passe”."
                    )
                }
                return
            }
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            _uiState.update {
                it.copy(
                    draft = it.draft.copy(
                        website = it.draft.website.ifBlank { trimmed },
                        qrValue = trimmed
                    ),
                    statusMessage = "Link do QR adicionado ao cartão."
                )
            }
            return
        }

        updateDraftQrValue(trimmed)
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

    fun setAppLockEnabled(enabled: Boolean) {
        _uiState.update { it.copy(appLockEnabled = enabled) }
        viewModelScope.launch {
            store.persistAppLockEnabled(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                notificationsEnabled = enabled,
                liveUpdatesEnabled = if (enabled) it.liveUpdatesEnabled else false
            )
        }
        viewModelScope.launch {
            store.persistNotificationsEnabled(enabled)
            if (!enabled) {
                store.persistLiveUpdatesEnabled(false)
            }
        }
    }

    fun setLiveUpdatesEnabled(enabled: Boolean) {
        _uiState.update { it.copy(liveUpdatesEnabled = enabled) }
        viewModelScope.launch {
            store.persistLiveUpdatesEnabled(enabled)
        }
    }

    fun setNowBarColor(color: Int) {
        _uiState.update { it.copy(nowBarColor = color) }
        viewModelScope.launch {
            store.persistNowBarColor(color)
        }
    }

    fun setWalletAvailability(available: Boolean) {
        _uiState.update { it.copy(canUseGoogleWallet = available) }
    }

    data class SaveDraftResult(val job: Job, val cardId: String)

    fun saveDraft(): SaveDraftResult? {
        val state = _uiState.value
        val draft = state.draft
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(statusMessage = message(it.appLanguage, "need_name")) }
            return null
        }

        val cardId = draft.id ?: UUID.randomUUID().toString()
        val draftToSave = draft.copy(id = cardId)

        val job = viewModelScope.launch {
            store.saveCard(draftToSave)
            MyCardWidgetProvider.requestUpdate(getApplication())
            _uiState.update {
                it.copy(
                    draft = CardDraft(passColor = it.draft.passColor),
                    statusMessage = message(it.appLanguage, "pass_saved")
                )
            }
        }

        return SaveDraftResult(job = job, cardId = cardId)
    }

    fun deleteCard(id: String) {
        viewModelScope.launch {
            store.deleteCard(id)
            MyCardWidgetProvider.requestUpdate(getApplication())
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

    fun prepareWalletSaveUrl(card: VisitingCard, onReady: (String) -> Unit) {
        val state = _uiState.value
        val backendEndpoint = normalizeWalletBackendEndpoint(
            state.walletBackendUrl.ifBlank { BuildConfig.CARDS_API_BASE_URL }
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSavingToWallet = true,
                    statusMessage = message(it.appLanguage, "wallet_generating")
                )
            }

            val payload = JSONObject()
                .put("cardId", card.id)
                .put("name", card.name)
                .put("role", card.role)
                .put("phone", card.phone)
                .put("email", card.email)
                .put("instagram", card.instagram)
                .put("linkedin", card.linkedin)
                .put("website", card.website)
                .put("note", card.note)
                .put("passColor", card.passColor)
                .put("qrValue", card.qrValue)
                .put("walletPhotoUrl", card.walletPhotoUrl)

            val result = walletSaveUrlClient.fetchSaveUrl(backendEndpoint, payload)
            result.onSuccess { url ->
                _uiState.update {
                    it.copy(
                        isSavingToWallet = false,
                        statusMessage = message(it.appLanguage, "wallet_opening")
                    )
                }
                onReady(url)
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
        if (trimmed.contains("/wallet/save-url")) return trimmed
        return if (trimmed.matches(Regex("^https?://[^/]+$"))) "$trimmed/wallet/save-url" else trimmed
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
                "wallet_missing" -> "Configure o backend do Google Wallet para gerar o link."
                "wallet_generating" -> "Gerando link do Google Wallet..."
                "wallet_opening" -> "Link gerado. Abrindo Google Wallet..."
                else -> "Falha ao gerar o link do Google Wallet."
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
                "wallet_missing" -> "Configure o backend do Google Wallet para gerar o link."
                "wallet_generating" -> "A gerar link do Google Wallet..."
                "wallet_opening" -> "Link gerado. A abrir o Google Wallet..."
                else -> "Falha ao gerar o link do Google Wallet."
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
                "wallet_missing" -> "Set up the backend to generate the Google Wallet link."
                "wallet_generating" -> "Generating Google Wallet link..."
                "wallet_opening" -> "Link received. Opening Google Wallet..."
                else -> "Failed to generate the Google Wallet link."
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
                "wallet_missing" -> "请配置后端以生成 Google Wallet 链接。"
                "wallet_generating" -> "正在生成 Google Wallet 链接..."
                "wallet_opening" -> "已收到链接，正在打开 Google Wallet..."
                else -> "生成 Google Wallet 链接失败。"
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
