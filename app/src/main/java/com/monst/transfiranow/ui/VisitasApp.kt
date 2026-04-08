package com.monst.transfiranow.ui

import android.Manifest
import android.graphics.Bitmap
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.monst.transfiranow.BuildConfig
import com.monst.transfiranow.data.AppLanguage
import com.monst.transfiranow.data.CardDraft
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.notifications.AppNotifications
import com.monst.transfiranow.premium.PremiumCardsActivity
import com.monst.transfiranow.share.CardExport
import com.monst.transfiranow.share.CardsBackup
import com.monst.transfiranow.ui.theme.TransfiraNowTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private enum class AppTab { HOME, CREATE, SAVED, SETTINGS }
private enum class OverlayScreen { MAIN, DETAILS_SAVED, DETAILS_DRAFT }

private fun adaptiveSidePadding(maxWidth: Dp, maxContentWidth: Dp, minPadding: Dp = 16.dp): Dp {
    if (maxWidth <= maxContentWidth) return minPadding
    val extra = (maxWidth - maxContentWidth) / 2f
    return if (extra > minPadding) extra else minPadding
}

class PassDetailsActivity : ComponentActivity() {
    private val viewModel: VisitasViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SAVED
        val cardId = intent.getStringExtra(EXTRA_CARD_ID).orEmpty()
        val draftFromIntent = if (mode == MODE_DRAFT) {
            CardDraft(
                name = intent.getStringExtra(EXTRA_DRAFT_NAME).orEmpty(),
                role = intent.getStringExtra(EXTRA_DRAFT_ROLE).orEmpty(),
                phone = intent.getStringExtra(EXTRA_DRAFT_PHONE).orEmpty(),
                email = intent.getStringExtra(EXTRA_DRAFT_EMAIL).orEmpty(),
                instagram = intent.getStringExtra(EXTRA_DRAFT_INSTAGRAM).orEmpty(),
                linkedin = intent.getStringExtra(EXTRA_DRAFT_LINKEDIN).orEmpty(),
                website = intent.getStringExtra(EXTRA_DRAFT_WEBSITE).orEmpty(),
                note = intent.getStringExtra(EXTRA_DRAFT_NOTE).orEmpty(),
                photoUri = intent.getStringExtra(EXTRA_DRAFT_PHOTO_URI).orEmpty(),
                avatarEmoji = intent.getStringExtra(EXTRA_DRAFT_AVATAR_EMOJI).orEmpty(),
                qrValue = intent.getStringExtra(EXTRA_DRAFT_QR_VALUE).orEmpty(),
                passColor = intent.getStringExtra(EXTRA_DRAFT_PASS_COLOR) ?: CardDraft().passColor
            )
        } else {
            null
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val t: (String) -> String = { key -> tr(uiState.appLanguage, key) }

            val currentCard = remember(uiState.cards, cardId) { uiState.cards.firstOrNull { it.id == cardId } }
            val accent = when (mode) {
                MODE_DRAFT -> parseColor(draftFromIntent?.passColor ?: uiState.draft.passColor)
                else -> parseColor(currentCard?.passColor ?: uiState.draft.passColor)
            }

            TransfiraNowTheme(dynamicColor = true, accentColor = accent) {
                AppLockGate(enabled = uiState.appLockEnabled) {
                    when (mode) {
                        MODE_DRAFT -> {
                            val draft = draftFromIntent ?: uiState.draft
                            PassDetailsScreen(
                                accentColor = parseColor(draft.passColor),
                                photoUri = draft.photoUri,
                                avatarEmoji = draft.avatarEmoji,
                                badgeText = t("pass"),
                                title = draft.name.ifBlank { "Seu nome" },
                                subtitle = draft.role.ifBlank { "Cargo ou descrição" },
                                tertiary = draft.website,
                                lines = listOf(
                                    t("phone") to draft.phone.ifBlank { "+55 00 00000-0000" },
                                    t("email") to draft.email.ifBlank { "email@exemplo.com" },
                                    t("instagram") to draft.instagram.ifBlank { "@instagram" },
                                    t("linkedin") to draft.linkedin.ifBlank { "linkedin.com/in/voce" },
                                    t("note") to draft.note.ifBlank { "Sem nota" }
                                ),
                                qrValue = draft.qrValue,
                                qrLabel = t("qr_code"),
                                onBack = { finish() }
                            )
                        }
                        else -> {
                            if (cardId.isBlank()) {
                                LaunchedEffect(Unit) { finish() }
                            } else if (currentCard == null) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    if (uiState.cards.isEmpty()) {
                                        CircularProgressIndicator()
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Cartão não encontrado.", style = MaterialTheme.typography.titleMedium)
                                            FilledTonalButton(onClick = { finish() }) { Text("Voltar") }
                                        }
                                    }
                                }
                            } else {
                                PassDetailsScreen(
                                    accentColor = parseColor(currentCard.passColor),
                                    photoUri = currentCard.photoUri,
                                    avatarEmoji = currentCard.avatarEmoji,
                                    badgeText = t("pass"),
                                    title = currentCard.name.ifBlank { "Sem nome" },
                                    subtitle = currentCard.role.ifBlank { "" },
                                    tertiary = currentCard.website,
                                    lines = listOf(
                                        t("phone") to currentCard.phone.ifBlank { "Sem telefone" },
                                        t("email") to currentCard.email.ifBlank { "Sem email" },
                                        t("instagram") to currentCard.instagram.ifBlank { "Sem Instagram" },
                                        t("linkedin") to currentCard.linkedin.ifBlank { "Sem LinkedIn" },
                                        t("note") to currentCard.note.ifBlank { "Sem nota" }
                                    ),
                                    qrValue = currentCard.qrValue,
                                    qrLabel = t("qr_code"),
                                    onBack = { finish() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_CARD_ID = "extra_card_id"

        const val MODE_SAVED = "saved"
        const val MODE_DRAFT = "draft"

        const val EXTRA_DRAFT_NAME = "draft_name"
        const val EXTRA_DRAFT_ROLE = "draft_role"
        const val EXTRA_DRAFT_PHONE = "draft_phone"
        const val EXTRA_DRAFT_EMAIL = "draft_email"
        const val EXTRA_DRAFT_INSTAGRAM = "draft_instagram"
        const val EXTRA_DRAFT_LINKEDIN = "draft_linkedin"
        const val EXTRA_DRAFT_WEBSITE = "draft_website"
        const val EXTRA_DRAFT_NOTE = "draft_note"
        const val EXTRA_DRAFT_PHOTO_URI = "draft_photo_uri"
        const val EXTRA_DRAFT_AVATAR_EMOJI = "draft_avatar_emoji"
        const val EXTRA_DRAFT_QR_VALUE = "draft_qr_value"
        const val EXTRA_DRAFT_PASS_COLOR = "draft_pass_color"
    }
}

@Composable
fun VisitasApp(
    onPickPhoto: () -> Unit,
    onPickQrCode: () -> Unit,
    onSaveToWallet: (VisitingCard) -> Unit,
    viewModel: VisitasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var tab by remember { mutableStateOf(AppTab.HOME) }
    val scope = rememberCoroutineScope()
    var detailsCardId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDraftDetails by rememberSaveable { mutableStateOf(false) }
    var showColorfulOverlay by rememberSaveable { mutableStateOf(false) }
    val t: (String) -> String = { key -> tr(uiState.appLanguage, key) }
    val detailsCard = uiState.cards.firstOrNull { it.id == detailsCardId }
    val overlayScreen = when {
        showDraftDetails -> OverlayScreen.DETAILS_DRAFT
        detailsCard != null -> OverlayScreen.DETAILS_SAVED
        else -> OverlayScreen.MAIN
    }
    val themeAccent = parseColor(detailsCard?.passColor ?: uiState.draft.passColor)
    TransfiraNowTheme(dynamicColor = true, accentColor = themeAccent) {
        AppLockGate(enabled = uiState.appLockEnabled) {
            Box(Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = overlayScreen,
                    transitionSpec = {
                    val forward = initialState == OverlayScreen.MAIN && targetState != OverlayScreen.MAIN

                    val enterSpring = spring<Float>(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                    val exitSpring = spring<Float>(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )

                    val enter = if (forward) {
                        slideInHorizontally(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                        ) { fullWidth -> (fullWidth * 0.92f).toInt() } +
                            fadeIn(tween(180)) +
                            scaleIn(animationSpec = enterSpring, initialScale = 0.985f)
                    } else {
                        slideInHorizontally(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        ) { fullWidth -> (-fullWidth * 0.18f).toInt() } +
                            fadeIn(tween(160))
                    }

                    val exit = if (forward) {
                        slideOutHorizontally(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                        ) { fullWidth -> (-fullWidth * 0.18f).toInt() } +
                            fadeOut(tween(140)) +
                            scaleOut(animationSpec = exitSpring, targetScale = 0.99f)
                    } else {
                        slideOutHorizontally(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                        ) { fullWidth -> (fullWidth * 0.92f).toInt() } +
                            fadeOut(tween(140)) +
                            scaleOut(animationSpec = exitSpring, targetScale = 0.985f)
                    }

                    enter.togetherWith(exit).using(SizeTransform(clip = false))
                },
                label = "overlay-screen"
            ) { screen ->
                when (screen) {
                    OverlayScreen.MAIN -> {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            bottomBar = { PillBar(tab, t) { tab = it } }
                        ) { padding ->
                            when (tab) {
                                AppTab.HOME -> CardsScreen(
                                    padding = padding,
                                    title = t("home_head"),
                                    subtitle = "",
                                    message = "",
                                    cards = uiState.cards.take(10),
                                    t = t,
                                    eventModeEnabled = uiState.eventModeEnabled,
                                    showDelete = false,
                                    onSaveToWallet = onSaveToWallet,
                                    onDelete = {},
                                    onEdit = {
                                        viewModel.editCard(it)
                                        tab = AppTab.CREATE
                                    },
                                    onOpenDetails = { card ->
                                        val intent = Intent(context, PassDetailsActivity::class.java)
                                            .putExtra(PassDetailsActivity.EXTRA_MODE, PassDetailsActivity.MODE_SAVED)
                                            .putExtra(PassDetailsActivity.EXTRA_CARD_ID, card.id)
                                        context.startActivity(intent)
                                    }
                                )
                                AppTab.CREATE -> CreateScreen(
                                    padding = padding,
                                    draft = uiState.draft,
                                    t = t,
                                    onPickPhoto = onPickPhoto,
                                    onPickQrCode = onPickQrCode,
                                    onScanQrFromBitmap = viewModel::scanQrFromBitmap,
                                    onImportVCard = viewModel::importVCardFromUri,
                                    onDraftChange = viewModel::updateDraft,
                                    onCreatePass = {
                                        scope.launch {
                                             val draft = uiState.draft
                                             val cardName = draft.name.trim()
                                             val role = draft.role.trim()
                                             val phone = draft.phone.trim()
                                             val instagram = draft.instagram.trim()
                                                 .takeIf { it.isNotBlank() }
                                                 ?.let { if (it.startsWith("@")) it else "@$it" }
                                                 .orEmpty()
                                             val presentationText = listOfNotNull(
                                                 role.takeIf { it.isNotBlank() },
                                                 phone.takeIf { it.isNotBlank() },
                                                 instagram.takeIf { it.isNotBlank() }
                                             ).joinToString(" • ").ifBlank { cardName.ifBlank { "Seu cartão está pronto para compartilhar" } }

                                             val saveResult = viewModel.saveDraft() ?: return@launch
                                             val job = saveResult.job
                                             val cardId = saveResult.cardId

                                             val shouldNotify =
                                                 uiState.notificationsEnabled && AppNotifications.canPostNotifications(context)

                                             val requestPromoted = uiState.liveUpdatesEnabled

                                             if (shouldNotify && uiState.liveUpdatesEnabled) {
                                                 AppNotifications.postCardGenerationLiveUpdate(
                                                     context,
                                                     cardName,
                                                     requestPromoted = requestPromoted,
                                                     pillColor = uiState.nowBarColor
                                                 )
                                             }

                                            showColorfulOverlay = true
                                            job.join()
                                             delay(1100)
                                              showColorfulOverlay = false

                                              if (shouldNotify) {
                                                  if (uiState.liveUpdatesEnabled) {
                                                      AppNotifications.cancelCardStatus(context)
                                                      AppNotifications.ensureChannels(context)
                                                      if (!AppNotifications.isEventChannelEnabled(context)) {
                                                          AppNotifications.openEventChannelSettings(context)
                                                          AppNotifications.postCardGenerationCompleted(
                                                              context,
                                                              cardName,
                                                              requestPromoted = false
                                                          )
                                                          return@launch
                                                      }
                                                      AppNotifications.startEventMode(
                                                          context,
                                                          title = cardName.ifBlank { t("presentation_mode") },
                                                          text = presentationText,
                                                          cardId = cardId
                                                      )
                                                      delay(400)
                                                      if (Build.VERSION.SDK_INT >= 36 && !AppNotifications.canPostPromotedNotifications(context)) {
                                                          AppNotifications.openAppNotificationPromotionSettings(context)
                                                      }
                                                  } else {
                                                      AppNotifications.postCardGenerationCompleted(
                                                          context,
                                                          cardName,
                                                          requestPromoted = false
                                                      )
                                                  }
                                              }
                                          }
                                      },
                                    onClearDraft = viewModel::clearDraft,
                                    onClearQr = viewModel::clearDraftQr,
                                    onOpenDraftDetails = {
                                        val draft = uiState.draft
                                        val intent = Intent(context, PassDetailsActivity::class.java)
                                            .putExtra(PassDetailsActivity.EXTRA_MODE, PassDetailsActivity.MODE_DRAFT)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_NAME, draft.name)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_ROLE, draft.role)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_PHONE, draft.phone)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_EMAIL, draft.email)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_INSTAGRAM, draft.instagram)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_LINKEDIN, draft.linkedin)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_WEBSITE, draft.website)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_NOTE, draft.note)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_PHOTO_URI, draft.photoUri)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_AVATAR_EMOJI, draft.avatarEmoji)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_QR_VALUE, draft.qrValue)
                                            .putExtra(PassDetailsActivity.EXTRA_DRAFT_PASS_COLOR, draft.passColor)
                                        context.startActivity(intent)
                                    }
                                )
                                AppTab.SAVED -> CardsScreen(
                                    padding = padding,
                                    title = t("saved_head"),
                                    subtitle = t("saved_sub"),
                                    message = "${uiState.cards.size} ${t("saved_count")}",
                                    cards = uiState.cards,
                                    t = t,
                                    eventModeEnabled = uiState.eventModeEnabled,
                                    showDelete = true,
                                    onSaveToWallet = onSaveToWallet,
                                    onDelete = { viewModel.deleteCard(it.id) },
                                    onEdit = {
                                        viewModel.editCard(it)
                                        tab = AppTab.CREATE
                                    },
                                    onOpenDetails = { card ->
                                        val intent = Intent(context, PassDetailsActivity::class.java)
                                            .putExtra(PassDetailsActivity.EXTRA_MODE, PassDetailsActivity.MODE_SAVED)
                                            .putExtra(PassDetailsActivity.EXTRA_CARD_ID, card.id)
                                        context.startActivity(intent)
                                    }
                                )
                                AppTab.SETTINGS -> SettingsScreen(
                                    padding = padding,
                                    cards = uiState.cards,
                                    currentLanguage = uiState.appLanguage,
                                    appLockEnabled = uiState.appLockEnabled,
                                    notificationsEnabled = uiState.notificationsEnabled,
                                    liveUpdatesEnabled = uiState.liveUpdatesEnabled,
                                    nowBarColor = uiState.nowBarColor,
                                    t = t,
                                    onLanguageSelected = viewModel::updateLanguage,
                                    onAppLockEnabledChange = viewModel::setAppLockEnabled,
                                    onNotificationsEnabledChange = viewModel::setNotificationsEnabled,
                                    onLiveUpdatesEnabledChange = viewModel::setLiveUpdatesEnabled,
                                    onNowBarColorChange = viewModel::setNowBarColor,
                                    onImportBackup = viewModel::importBackupFromUri
                                )
                            }
                        }
                    }
                    OverlayScreen.DETAILS_SAVED -> {
                        if (detailsCard == null) {
                            LaunchedEffect(detailsCardId) { detailsCardId = null }
                        } else {
                            PassDetailsScreen(
                                accentColor = parseColor(detailsCard.passColor),
                                photoUri = detailsCard.photoUri,
                                avatarEmoji = detailsCard.avatarEmoji,
                                badgeText = t("pass"),
                                title = detailsCard.name.ifBlank { "Sem nome" },
                                subtitle = detailsCard.role.ifBlank { "" },
                                tertiary = detailsCard.website.ifBlank { "" },
                                lines = listOf(
                                    t("phone") to detailsCard.phone.ifBlank { "Sem telefone" },
                                    t("email") to detailsCard.email.ifBlank { "Sem email" },
                                    t("instagram") to detailsCard.instagram.ifBlank { "Sem Instagram" },
                                    t("linkedin") to detailsCard.linkedin.ifBlank { "Sem LinkedIn" },
                                    t("note") to detailsCard.note.ifBlank { "Sem nota" }
                                ),
                                qrValue = detailsCard.qrValue,
                                qrLabel = t("qr_code"),
                                onBack = { detailsCardId = null }
                            )
                        }
                    }
                    OverlayScreen.DETAILS_DRAFT -> {
                        PassDetailsScreen(
                            accentColor = parseColor(uiState.draft.passColor),
                            photoUri = uiState.draft.photoUri,
                            avatarEmoji = uiState.draft.avatarEmoji,
                            badgeText = t("pass"),
                            title = uiState.draft.name.ifBlank { "Seu nome" },
                            subtitle = uiState.draft.role.ifBlank { "Cargo ou descrição" },
                            tertiary = uiState.draft.website.ifBlank { "https://seu-link.com" },
                            lines = listOf(
                                t("phone") to uiState.draft.phone.ifBlank { "+55 00 00000-0000" },
                                t("email") to uiState.draft.email.ifBlank { "email@exemplo.com" },
                                t("instagram") to uiState.draft.instagram.ifBlank { "@instagram" },
                                t("linkedin") to uiState.draft.linkedin.ifBlank { "linkedin.com/in/voce" },
                                t("note") to uiState.draft.note.ifBlank { "Sem nota" }
                            ),
                            qrValue = uiState.draft.qrValue,
                            qrLabel = t("qr_code"),
                            onBack = { showDraftDetails = false }
                        )
                    }
                }
            }

                ColorfulLoadingOverlay(
                    visible = showColorfulOverlay,
                    message = "gerar o cartão",
                    accentColor = parseColor(uiState.draft.passColor)
                )
            }
        }
    }
}

@Composable
private fun PillBar(selected: AppTab, t: (String) -> String, onSelect: (AppTab) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PillItem(AppTab.HOME, selected, t("home"), Icons.Rounded.Home, onSelect)
                PillItem(AppTab.CREATE, selected, t("create"), Icons.Rounded.Add, onSelect)
                PillItem(AppTab.SAVED, selected, t("saved"), Icons.Rounded.Style, onSelect)
                PillItem(AppTab.SETTINGS, selected, t("settings"), Icons.Rounded.Settings, onSelect)
            }
        }
    }
}

@Composable
private fun RowScope.PillItem(tab: AppTab, selected: AppTab, label: String, icon: ImageVector, onSelect: (AppTab) -> Unit) {
    val active = tab == selected
    Surface(modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).clickable { onSelect(tab) }, color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, modifier = Modifier.size(20.dp), tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CardsScreen(padding: PaddingValues, title: String, subtitle: String, message: String, cards: List<VisitingCard>, t: (String) -> String, eventModeEnabled: Boolean, showDelete: Boolean, onSaveToWallet: (VisitingCard) -> Unit, onDelete: (VisitingCard) -> Unit, onEdit: (VisitingCard) -> Unit, onOpenDetails: (VisitingCard) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { ListHeader(title = title, subtitle = subtitle, message = message) }
            if (!showDelete && cards.isNotEmpty()) {
                item {
                    val context = LocalContext.current
                    val latest = cards.first()
                    ElevatedCard(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(t("presentation_mode"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    latest.name.ifBlank { "Cartão mais recente" },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    if (eventModeEnabled) {
                                        AppNotifications.stopEventMode(context)
                                        showToast(context, "Modo Evento encerrado.")
                                        return@FilledTonalButton
                                    }

                                    if (!AppNotifications.canPostNotifications(context)) {
                                        showToast(context, "Ative as notificações para funcionar.")
                                        AppNotifications.openAppNotificationSettings(context)
                                        return@FilledTonalButton
                                    }

                                    AppNotifications.ensureChannels(context)
                                    if (!AppNotifications.isEventChannelEnabled(context)) {
                                        showToast(context, "Ative o canal “Modo Evento” para aparecer na Now Bar.")
                                        AppNotifications.openEventChannelSettings(context)
                                        return@FilledTonalButton
                                    }

                                    AppNotifications.startEventMode(
                                        context,
                                        title = t("presentation_mode"),
                                        text = latest.name.ifBlank { "Seu cartão está pronto para compartilhar" },
                                        cardId = latest.id
                                    )

                                     if (Build.VERSION.SDK_INT >= 36 && !AppNotifications.canPostPromotedNotifications(context)) {
                                         showToast(context, t("live_updates_denied"))
                                         AppNotifications.openAppNotificationPromotionSettings(context)
                                     } else {
                                         showToast(context, t("presentation_started"))
                                     }
                                }
                            ) { Text(if (eventModeEnabled) "Encerrar" else "Ativar") }
                        }
                    }
                }
            }
            if (cards.isEmpty()) item { EmptyCard(t(if (showDelete) "saved_empty_title" else "home_empty_title"), t(if (showDelete) "saved_empty_body" else "home_empty_body")) }
            else items(cards, key = { it.id }) { card -> SavedPassCard(card, t, showDelete, { onEdit(card) }, { onDelete(card) }, { onSaveToWallet(card) }, { onOpenDetails(card) }) }
        }
    }
}

@Composable
private fun ListHeader(title: String, subtitle: String, message: String) {
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val compact = config.screenHeightDp < 620 || config.screenWidthDp < 360

    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CreateScreen(
    padding: PaddingValues,
    draft: CardDraft,
    t: (String) -> String,
    onPickPhoto: () -> Unit,
    onPickQrCode: () -> Unit,
    onScanQrFromBitmap: (Bitmap) -> Unit,
    onImportVCard: (Uri) -> Unit,
    onDraftChange: ((CardDraft) -> CardDraft) -> Unit,
    onCreatePass: () -> Unit,
    onClearDraft: () -> Unit,
    onClearQr: () -> Unit,
    onOpenDraftDetails: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                CreateInvitesEditorCard(
                    draft = draft,
                    t = t,
                    onPickPhoto = onPickPhoto,
                    onPickQrCode = onPickQrCode,
                    onScanQrFromBitmap = onScanQrFromBitmap,
                    onImportVCard = onImportVCard,
                    onDraftChange = onDraftChange,
                    onCreatePass = onCreatePass,
                    onClearDraft = onClearDraft,
                    onClearQr = onClearQr,
                    onOpenDraftDetails = onOpenDraftDetails
                )
            }
        }
    }
}

@Composable
private fun CreateInvitesEditorCard(
    draft: CardDraft,
    t: (String) -> String,
    onPickPhoto: () -> Unit,
    onPickQrCode: () -> Unit,
    onScanQrFromBitmap: (Bitmap) -> Unit,
    onImportVCard: (Uri) -> Unit,
    onDraftChange: ((CardDraft) -> CardDraft) -> Unit,
    onCreatePass: () -> Unit,
    onClearDraft: () -> Unit,
    onClearQr: () -> Unit,
    onOpenDraftDetails: () -> Unit
) {
    val accentColor = parseColor(draft.passColor)
    var preview by rememberSaveable { mutableStateOf(false) }

    val qrCamera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap ?: return@rememberLauncherForActivityResult
        onScanQrFromBitmap(bitmap)
    }

    val vcfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onImportVCard(uri)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (draft.photoUri.isNotBlank()) {
                AsyncImage(
                    model = draft.photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.95f),
                                    accentColor.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(color = Color.Black.copy(alpha = 0.28f), shape = RoundedCornerShape(999.dp)) {
                        TextButton(onClick = { preview = !preview }) {
                            Text(if (preview) "Editar" else "Preview", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Surface(color = Color.Black.copy(alpha = 0.28f), shape = RoundedCornerShape(999.dp)) {
                        TextButton(onClick = onPickPhoto) {
                            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Editar fundo", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (preview) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Black.copy(alpha = 0.22f)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DraftPreview(draft, t("pass"), t, onOpenDraftDetails)
                            Text(t("create_hint"), color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                        }
                    }
                    return@Column
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color.Black.copy(alpha = 0.22f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FrostedField(draft.name, t("name")) { onDraftChange { d -> d.copy(name = it) } }
                        FrostedField(draft.avatarEmoji, t("emoji")) { onDraftChange { d -> d.copy(avatarEmoji = it) } }
                        FrostedField(draft.role, t("role")) { onDraftChange { d -> d.copy(role = it) } }
                        FrostedField(draft.phone, t("phone")) { onDraftChange { d -> d.copy(phone = it) } }
                        FrostedField(draft.email, t("email")) { onDraftChange { d -> d.copy(email = it) } }
                        FrostedField(draft.instagram, t("instagram")) { onDraftChange { d -> d.copy(instagram = it) } }
                        FrostedField(draft.linkedin, t("linkedin")) { onDraftChange { d -> d.copy(linkedin = it) } }
                        FrostedField(draft.website, t("url")) { onDraftChange { d -> d.copy(website = it) } }
                        FrostedField(draft.note, t("note"), singleLine = false) { onDraftChange { d -> d.copy(note = it) } }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color.Black.copy(alpha = 0.22f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(t("qr_code"), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (draft.qrValue.isBlank()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilledTonalButton(onClick = onPickQrCode, modifier = Modifier.weight(1f)) { Text(t("qr_pick")) }
                                FilledTonalButton(onClick = { qrCamera.launch(null) }, modifier = Modifier.weight(1f)) { Text(t("qr_scan")) }
                            }
                        } else {
                            QrCodeCard(value = draft.qrValue, label = t("qr_code"), showValueText = false)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                FilledTonalButton(onClick = onPickQrCode, modifier = Modifier.weight(1f)) { Text(t("qr_change")) }
                                FilledTonalButton(onClick = { qrCamera.launch(null) }, modifier = Modifier.weight(1f)) { Text(t("qr_scan")) }
                                TextButton(onClick = onClearQr) { Text(t("qr_remove"), color = Color.White) }
                            }
                        }
                        TextButton(onClick = { vcfPicker.launch(arrayOf("text/vcard", "text/x-vcard", "text/plain", "*/*")) }) {
                            Text(t("vcf_import"), color = Color.White)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color.Black.copy(alpha = 0.22f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(t("pass_color"), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        ColorPicker(draft.passColor) { color -> onDraftChange { d -> d.copy(passColor = color) } }
                    }
                }

                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color.Black.copy(alpha = 0.22f)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onCreatePass, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Save, null)
                            Spacer(Modifier.width(8.dp))
                            Text(t("create_pass"))
                        }
                        TextButton(onClick = onClearDraft) { Text(t("clear"), color = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FrostedField(value: String, label: String, singleLine: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        colors = TextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedContainerColor = Color.White.copy(alpha = 0.10f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
            focusedLabelColor = Color.White.copy(alpha = 0.85f),
            unfocusedLabelColor = Color.White.copy(alpha = 0.70f),
            focusedIndicatorColor = Color.White.copy(alpha = 0.55f),
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.30f),
            cursorColor = Color.White
        )
    )
}

@Composable
private fun EditorCard(
    draft: CardDraft,
    canUseGoogleWallet: Boolean,
    walletIssuerId: String,
    walletClassSuffix: String,
    walletBackendUrl: String,
    t: (String) -> String,
    onPickQrCode: () -> Unit,
    onDraftChange: ((CardDraft) -> CardDraft) -> Unit,
    onWalletSettingsChange: (String?, String?, String?) -> Unit,
    onCreatePass: () -> Unit,
    onPersistWalletSettings: () -> Unit,
    onClearDraft: () -> Unit,
    onClearQr: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Field(draft.name, t("name")) { onDraftChange { d -> d.copy(name = it) } }
            Field(draft.role, t("role")) { onDraftChange { d -> d.copy(role = it) } }
            Field(draft.phone, t("phone")) { onDraftChange { d -> d.copy(phone = it) } }
            Field(draft.email, t("email")) { onDraftChange { d -> d.copy(email = it) } }
            Field(draft.instagram, t("instagram")) { onDraftChange { d -> d.copy(instagram = it) } }
            Field(draft.linkedin, t("linkedin")) { onDraftChange { d -> d.copy(linkedin = it) } }
            Field(draft.website, t("url")) { onDraftChange { d -> d.copy(website = it) } }
            Field(draft.note, t("note")) { onDraftChange { d -> d.copy(note = it) } }
            Field(draft.walletPhotoUrl, t("wallet_photo")) { onDraftChange { d -> d.copy(walletPhotoUrl = it) } }

            Text(t("qr_code"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            if (draft.qrValue.isBlank()) {
                FilledTonalButton(onClick = onPickQrCode) { Text(t("qr_pick")) }
            } else {
                QrCodeCard(value = draft.qrValue, label = t("qr_code"), showValueText = false)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = onPickQrCode) { Text(t("qr_change")) }
                    TextButton(onClick = onClearQr) { Text(t("qr_remove")) }
                }
            }

            Text(t("pass_color"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            ColorPicker(draft.passColor) { color -> onDraftChange { d -> d.copy(passColor = color) } }
            WalletCard(canUseGoogleWallet, walletIssuerId, walletClassSuffix, walletBackendUrl, t, onWalletSettingsChange, onPersistWalletSettings)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onCreatePass, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Save, null); Spacer(Modifier.width(8.dp)); Text(t("create_pass")) }
                TextButton(onClick = onClearDraft) { Text(t("clear")) }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    padding: PaddingValues,
    cards: List<VisitingCard>,
    currentLanguage: AppLanguage,
    appLockEnabled: Boolean,
    notificationsEnabled: Boolean,
    liveUpdatesEnabled: Boolean,
    nowBarColor: Int,
    t: (String) -> String,
    onLanguageSelected: (AppLanguage) -> Unit,
    onAppLockEnabledChange: (Boolean) -> Unit,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    onLiveUpdatesEnabledChange: (Boolean) -> Unit,
    onNowBarColorChange: (Int) -> Unit,
    onImportBackup: (Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        onImportBackup(uri)
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
        val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Hero(t("settings_head"), t("settings_sub"), "${t("version")}: ${BuildConfig.VERSION_NAME}") }
            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Contacts, null)
                            Text(t("contacts"), style = MaterialTheme.typography.titleLarge)
                        }

                        SettingsLinkRow(
                            icon = Icons.Rounded.Email,
                            title = "Email",
                            value = "Erivaldojeson8@gmail.com"
                        ) {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:Erivaldojeson8@gmail.com")
                            }
                            context.startActivity(intent)
                        }

                        SettingsLinkRow(
                            icon = Icons.Rounded.Link,
                            title = "GitHub",
                            value = "github.com/Erivaldojelson"
                        ) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Erivaldojelson"))
                            context.startActivity(intent)
                        }

                        SettingsLinkRow(
                            icon = Icons.Rounded.Phone,
                            title = "Telefone",
                            value = "31983472309"
                        ) {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:31983472309")
                            }
                            context.startActivity(intent)
                        }
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Rounded.Language, null); Text(t("language"), style = MaterialTheme.typography.titleLarge) }
                        listOf(AppLanguage.PT_BR to "Português (Brasil)", AppLanguage.PT_PT to "Português (Portugal)", AppLanguage.PT_AO to "Português (Angola)", AppLanguage.EN to "English", AppLanguage.ZH to "中文").forEach { (lang, label) ->
                            FilterChip(selected = currentLanguage == lang, onClick = { onLanguageSelected(lang) }, label = { Text(label) })
                        }
                    }
                }
            }
            item {
                val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                val canUseBiometric = remember {
                    BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
                }
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Segurança", style = MaterialTheme.typography.titleLarge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Bloquear o app com biometria", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Ao abrir pelo ícone/atalhos/notificações, pede impressão digital para continuar.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!canUseBiometric) {
                                    Text(
                                        "Biometria indisponível neste dispositivo.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = appLockEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        onAppLockEnabledChange(false)
                                    } else if (!canUseBiometric) {
                                        Toast.makeText(context, "Biometria indisponível neste dispositivo.", Toast.LENGTH_LONG).show()
                                    } else {
                                        onAppLockEnabledChange(true)
                                    }
                                },
                                enabled = canUseBiometric
                            )
                        }
                    }
                }
            }
            item {
                val lifecycleOwner = LocalLifecycleOwner.current
                val isAndroid16 = Build.VERSION.SDK_INT >= 36

                fun checkNotificationPermission(): Boolean {
                    if (Build.VERSION.SDK_INT < 33) return true
                    return ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }

                fun checkSystemNotificationsEnabled(): Boolean {
                    return AppNotifications.canPostNotifications(context)
                }

                fun checkCanPromote(): Boolean {
                    if (!isAndroid16) return false
                    return AppNotifications.canPostPromotedNotifications(context)
                }

                var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission()) }
                var systemNotificationsEnabled by remember { mutableStateOf(checkSystemNotificationsEnabled()) }
                var canPromote by remember { mutableStateOf(checkCanPromote()) }

                fun refreshNotificationStatus() {
                    hasNotificationPermission = checkNotificationPermission()
                    systemNotificationsEnabled = checkSystemNotificationsEnabled()
                    canPromote = checkCanPromote()
                }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            refreshNotificationStatus()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val requestPermission =
                    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                        refreshNotificationStatus()
                        if (granted) {
                            AppNotifications.ensureChannels(context)
                            onNotificationsEnabledChange(true)

                            if (!systemNotificationsEnabled) {
                                AppNotifications.openAppNotificationSettings(context)
                                Toast.makeText(
                                    context,
                                    "Ative “Permitir notificações” nas configurações do sistema.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Notificações ativadas.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            onNotificationsEnabledChange(false)
                            Toast.makeText(context, "Permissão de notificações negada.", Toast.LENGTH_LONG).show()
                        }
                    }

                LaunchedEffect(hasNotificationPermission, notificationsEnabled) {
                    if (notificationsEnabled && !hasNotificationPermission) {
                        onNotificationsEnabledChange(false)
                    }
                }

                LaunchedEffect(notificationsEnabled, liveUpdatesEnabled) {
                    if (!notificationsEnabled && liveUpdatesEnabled) {
                        onLiveUpdatesEnabledChange(false)
                    }
                }

                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Notificações", style = MaterialTheme.typography.titleLarge)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Ativar notificações", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Receba avisos quando um cartão for criado.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) {
                                    Text(
                                        "Permita a permissão de notificações para funcionar.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else if (!systemNotificationsEnabled) {
                                    Text(
                                        "Ative “Permitir notificações” nas configurações do sistema.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        onNotificationsEnabledChange(false)
                                        onLiveUpdatesEnabledChange(false)
                                        AppNotifications.cancelCardStatus(context)
                                        return@Switch
                                    }

                                    if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) {
                                        requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        AppNotifications.ensureChannels(context)
                                        onNotificationsEnabledChange(true)

                                        if (!systemNotificationsEnabled) {
                                            AppNotifications.openAppNotificationSettings(context)
                                            Toast.makeText(
                                                context,
                                                "Ative “Permitir notificações” nas configurações do sistema.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            )
                        }

                        if ((Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) || !systemNotificationsEnabled) {
                            FilledTonalButton(onClick = { AppNotifications.openAppNotificationSettings(context) }) {
                                Text("Abrir configurações de notificações")
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Live Updates (Android 16+)", style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (isAndroid16) {
                                                "Mostra uma notificação fixada com chip na barra de status."
                                            } else {
                                                "Disponível somente no Android 16 ou superior."
                                            },
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (isAndroid16 && notificationsEnabled && liveUpdatesEnabled && !canPromote) {
                                            Text(
                                                "Ative “Live notifications” nas configurações do sistema.",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Switch(
                                        checked = liveUpdatesEnabled,
                                        onCheckedChange = { enabled ->
                                            if (!enabled) {
                                                onLiveUpdatesEnabledChange(false)
                                                return@Switch
                                            }

                                            if (!notificationsEnabled) {
                                                Toast.makeText(context, "Ative as notificações primeiro.", Toast.LENGTH_LONG).show()
                                                return@Switch
                                            }

                                            if (!isAndroid16) {
                                                Toast.makeText(context, "Requer Android 16+.", Toast.LENGTH_LONG).show()
                                                return@Switch
                                            }

                                            onLiveUpdatesEnabledChange(true)

                                            if (!canPromote) {
                                                AppNotifications.openAppNotificationPromotionSettings(context)
                                                Toast.makeText(context, "Ative “Live notifications” e volte aqui.", Toast.LENGTH_LONG).show()
                                                return@Switch
                                            }
                                        },
                                        enabled = notificationsEnabled && isAndroid16
                                    )
                                }

                                if (isAndroid16 && notificationsEnabled && liveUpdatesEnabled && !canPromote) {
                                    FilledTonalButton(onClick = { AppNotifications.openAppNotificationPromotionSettings(context) }) {
                                        Text("Permitir Live Updates")
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Cor da Now Bar", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (isAndroid16) {
                                        "Escolha a cor da pílula do Live Update/Now Bar."
                                    } else {
                                        "Disponível somente no Android 16 ou superior."
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                val spectrum = remember {
                                    listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { hue ->
                                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                                    }
                                }

                                val initialHue = remember(nowBarColor) {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(nowBarColor, hsv)
                                    hsv[0]
                                }
                                var hue by remember(nowBarColor) { mutableStateOf(initialHue) }
                                var previewColor by remember(nowBarColor) { mutableStateOf(nowBarColor) }

                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Brush.horizontalGradient(spectrum))
                                )

                                Slider(
                                    value = hue,
                                    onValueChange = { value ->
                                        hue = value
                                        previewColor = android.graphics.Color.HSVToColor(floatArrayOf(value, 1f, 1f))
                                    },
                                    valueRange = 0f..360f,
                                    enabled = isAndroid16,
                                    onValueChangeFinished = { onNowBarColorChange(previewColor) }
                                )

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(previewColor))
                                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    )
                                    Text(
                                        "#%06X".format(0xFFFFFF and previewColor),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        if (notificationsEnabled && hasNotificationPermission && systemNotificationsEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilledTonalButton(
                                        onClick = {
                                            AppNotifications.postCardGenerationCompleted(context, "Cartão de teste")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Testar notificação")
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            scope.launch {
                                                AppNotifications.startEventMode(
                                                    context,
                                                    title = "Modo Evento Ativo",
                                                    text = "Cartão de teste (Live Update)"
                                                )
                                                delay(400)
                                                if (!canPromote) {
                                                    AppNotifications.openAppNotificationPromotionSettings(context)
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = isAndroid16 && liveUpdatesEnabled
                                    ) {
                                        Text("Iniciar Modo Evento")
                                    }
                                }

                                if (isAndroid16) {
                                    TextButton(
                                        onClick = { AppNotifications.stopEventMode(context) },
                                        enabled = liveUpdatesEnabled
                                    ) {
                                        Text("Encerrar Modo Evento")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(t("backup"), style = MaterialTheme.typography.titleLarge)
                        Text("${cards.size} ${t("saved_count")}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            val uri = withContext(Dispatchers.IO) { CardsBackup.exportJson(context, cards) }
                                            shareFile(context, uri, "application/json", t("backup"))
                                        }.onFailure { error ->
                                            showToast(context, error.message ?: "Falha ao exportar backup.")
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(t("backup_export"))
                            }
                            FilledTonalButton(
                                onClick = { backupPicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(t("backup_import"))
                            }
                        }
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t("terms"), style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Todos os direitos do app (incluindo nome, interface, animações e código) são reservados a Erivaldojelson.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(t("version"), style = MaterialTheme.typography.titleMedium)
                        Text(BuildConfig.VERSION_NAME)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsLinkRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    value,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Rounded.OpenInNew, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Hero(title: String, subtitle: String, message: String) {
    ElevatedCard(shape = RoundedCornerShape(36.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.secondaryContainer) { Text("Visitas", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.onSecondaryContainer) }
            Text(title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable private fun EmptyCard(title: String, body: String) { ElevatedCard(shape = RoundedCornerShape(28.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(body) } } }
@Composable private fun Field(value: String, label: String, onChange: (String) -> Unit) { OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }

@Composable
private fun WalletCard(canUseGoogleWallet: Boolean, walletIssuerId: String, walletClassSuffix: String, walletBackendUrl: String, t: (String) -> String, onWalletSettingsChange: (String?, String?, String?) -> Unit, onPersistWalletSettings: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t("wallet"), style = MaterialTheme.typography.titleLarge)
            Text(if (canUseGoogleWallet) t("wallet_on") else t("wallet_off"), color = if (canUseGoogleWallet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Field(walletIssuerId, "Issuer ID") { onWalletSettingsChange(it, null, null) }
            Field(walletClassSuffix, "Class suffix") { onWalletSettingsChange(null, it, null) }
            Field(walletBackendUrl, t("backend")) { onWalletSettingsChange(null, null, it) }
            FilledTonalButton(onClick = onPersistWalletSettings) { Text(t("wallet_save")) }
        }
    }
}

@Composable
private fun DraftPreview(draft: CardDraft, passLabel: String, t: (String) -> String, onOpenDetails: () -> Unit) {
    val c = parseColor(draft.passColor)
    ExpandablePassCard(
        accentColor = c,
        photoUri = draft.photoUri,
        avatarEmoji = draft.avatarEmoji,
        badgeText = passLabel,
        title = draft.name.ifBlank { "Seu nome" },
        subtitle = draft.role.ifBlank { "Cargo ou descrição" },
        tertiary = draft.website.ifBlank { "https://seu-link.com" },
        lines = listOf(
            t("phone") to draft.phone.ifBlank { "+55 00 00000-0000" },
            t("email") to draft.email.ifBlank { "email@exemplo.com" },
            t("instagram") to draft.instagram.ifBlank { "@instagram" }
        ),
        qrValue = draft.qrValue,
        qrLabel = t("qr_code"),
        expanded = false,
        onToggleExpanded = onOpenDetails,
        footer = null
    )
}

@Composable
private fun SavedPassCard(card: VisitingCard, t: (String) -> String, showDelete: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onSaveToWallet: () -> Unit, onOpenDetails: () -> Unit) {
    val accentColor = parseColor(card.passColor)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var shareMenuExpanded by remember(card.id) { mutableStateOf(false) }
    val phone = card.phone.ifBlank { "Sem telefone" }
    val email = card.email.ifBlank { "Sem email" }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onOpenDetails() },
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (card.photoUri.isNotBlank()) {
                AsyncImage(
                    model = card.photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.95f),
                                    accentColor.copy(alpha = 0.55f),
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.10f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.70f)
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                accentColor.copy(alpha = 0.20f),
                                accentColor.copy(alpha = 0.92f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(color = Color.Black.copy(alpha = 0.28f), shape = RoundedCornerShape(999.dp)) {
                    Text(
                        t("pass"),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(10.dp))
                PhotoBubble(card.photoUri, accentColor, Modifier.size(48.dp), avatarEmoji = card.avatarEmoji)
                Text(
                    card.name.ifBlank { "Sem nome" },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    phone,
                    color = Color.White.copy(alpha = 0.90f),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    email,
                    color = Color.White.copy(alpha = 0.86f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = Color.Black.copy(alpha = 0.22f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(onClick = onEdit) {
                            Icon(Icons.Rounded.Edit, null)
                            if (showDelete) {
                                Spacer(Modifier.width(8.dp))
                                Text(t("edit"))
                            }
                        }
                        Box {
                            FilledTonalButton(onClick = { shareMenuExpanded = true }) {
                                Icon(Icons.Rounded.Share, null)
                            }
                            DropdownMenu(expanded = shareMenuExpanded, onDismissRequest = { shareMenuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(t("share_image")) },
                                    onClick = {
                                        shareMenuExpanded = false
                                        scope.launch {
                                            runCatching {
                                                val uri = withContext(Dispatchers.IO) { CardExport.exportPng(context, card) }
                                                shareFile(context, uri, "image/png", t("share"))
                                            }.onFailure { error ->
                                                showToast(context, error.message ?: "Falha ao exportar imagem.")
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(t("share_pdf")) },
                                    onClick = {
                                        shareMenuExpanded = false
                                        scope.launch {
                                            runCatching {
                                                val uri = withContext(Dispatchers.IO) { CardExport.exportPdf(context, card) }
                                                shareFile(context, uri, "application/pdf", t("share"))
                                            }.onFailure { error ->
                                                showToast(context, error.message ?: "Falha ao exportar PDF.")
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(t("share_vcard")) },
                                    onClick = {
                                        shareMenuExpanded = false
                                        scope.launch {
                                            runCatching {
                                                val uri = withContext(Dispatchers.IO) { CardExport.exportVcf(context, card) }
                                                shareFile(context, uri, "text/x-vcard", t("share"))
                                            }.onFailure { error ->
                                                showToast(context, error.message ?: "Falha ao exportar vCard.")
                                            }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(t("share_contacts")) },
                                    onClick = {
                                        shareMenuExpanded = false
                                        runCatching {
                                            val notes = buildList {
                                                card.note.trim().takeIf { it.isNotBlank() }?.let { add(it) }
                                                card.instagram.trim().takeIf { it.isNotBlank() }?.let { add("Instagram: $it") }
                                                card.linkedin.trim().takeIf { it.isNotBlank() }?.let { add("LinkedIn: $it") }
                                                card.website.trim().takeIf { it.isNotBlank() }?.let { add("Website: $it") }
                                            }.joinToString("\n")

                                            val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                                                type = ContactsContract.RawContacts.CONTENT_TYPE
                                                putExtra(ContactsContract.Intents.Insert.NAME, card.name.trim())
                                                putExtra(ContactsContract.Intents.Insert.JOB_TITLE, card.role.trim())
                                                putExtra(ContactsContract.Intents.Insert.PHONE, card.phone.trim())
                                                putExtra(ContactsContract.Intents.Insert.EMAIL, card.email.trim())
                                                putExtra(ContactsContract.Intents.Insert.NOTES, notes)
                                            }
                                            context.startActivity(intent)
                                        }.onFailure { error ->
                                            showToast(context, error.message ?: "Falha ao abrir Contatos.")
                                        }
                                    }
                                )
                            }
                        }
                        WalletActionButton(onClick = onSaveToWallet, label = t("wallet_add"), modifier = Modifier.weight(1f))
                        if (showDelete) TextButton(onClick = onDelete) {
                            Icon(Icons.Rounded.Delete, null)
                            Spacer(Modifier.width(4.dp))
                            Text(t("delete"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassDetailsScreen(
    accentColor: Color,
    photoUri: String,
    avatarEmoji: String,
    badgeText: String,
    title: String,
    subtitle: String,
    tertiary: String,
    lines: List<Pair<String, String>>,
    qrValue: String,
    qrLabel: String,
    onBack: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(Modifier.fillMaxSize()) {
        AnimatedGraySparkleBackground(
            modifier = Modifier.matchParentSize(),
            seed = accentColor,
            alpha = 1f
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 18.dp, bottom = 12.dp)
                ) {
                    val side = adaptiveSidePadding(maxWidth, maxContentWidth = 560.dp)
                    Box(Modifier.fillMaxWidth().padding(horizontal = side)) {
                        Surface(
                            onClick = onBack,
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                val side = adaptiveSidePadding(maxWidth, maxContentWidth = 560.dp)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(side, 32.dp, side, 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        AnimatedVisibility(
                            visible = visible,
                            enter = fadeIn(tween(180)) + scaleIn(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                                initialScale = 0.92f
                            ),
                            exit = fadeOut(tween(140)) + scaleOut(
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                                targetScale = 0.985f
                            )
                        ) {
                            PassExpandedCard(
                                accentColor = accentColor,
                                photoUri = photoUri,
                                avatarEmoji = avatarEmoji,
                                badgeText = badgeText,
                                title = title,
                                subtitle = subtitle,
                                tertiary = tertiary,
                                lines = lines,
                                qrValue = qrValue,
                                qrLabel = qrLabel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PassExpandedCard(
    accentColor: Color,
    photoUri: String,
    avatarEmoji: String,
    badgeText: String,
    title: String,
    subtitle: String,
    tertiary: String,
    lines: List<Pair<String, String>>,
    qrValue: String,
    qrLabel: String
) {
    PassTicketGlass(
        accentColor = accentColor,
        photoUri = photoUri,
        avatarEmoji = avatarEmoji,
        badgeText = badgeText,
        title = title,
        subtitle = subtitle,
        tertiary = tertiary,
        lines = lines,
        qrValue = qrValue,
        qrLabel = qrLabel
    )
}

@Composable
private fun PassTicketGlass(
    accentColor: Color,
    photoUri: String,
    avatarEmoji: String,
    badgeText: String,
    title: String,
    subtitle: String,
    tertiary: String,
    lines: List<Pair<String, String>>,
    qrValue: String,
    qrLabel: String
) {
    val shape = RoundedCornerShape(34.dp)
    val transition = rememberInfiniteTransition(label = "glass-ticket")
    val time01 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8200, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "glass-time"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Color.White.copy(alpha = 0.14f), shape)
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val tau = (2f * PI).toFloat()

                    val c1x = 0.5f + 0.42f * sin(tau * (time01 * 0.18f) + 0.2f)
                    val c1y = 0.5f + 0.38f * cos(tau * (time01 * 0.16f) + 0.7f)
                    val c2x = 0.5f + 0.45f * cos(tau * (time01 * 0.22f) + 1.1f)
                    val c2y = 0.5f + 0.40f * sin(tau * (time01 * 0.20f) + 0.4f)

                    val base = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0B0C0F),
                            Color(0xFF141823),
                            Color(0xFF0B0C0F)
                        ),
                        start = Offset(size.width * 0.05f, 0f),
                        end = Offset(size.width * 0.95f, size.height)
                    )

                    val glow1 = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.65f),
                            accentColor.copy(alpha = 0.0f)
                        ),
                        center = Offset(size.width * c1x, size.height * c1y),
                        radius = size.minDimension * 0.85f
                    )

                    val glow2 = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * c2x, size.height * c2y),
                        radius = size.minDimension * 0.95f
                    )

                    onDrawBehind {
                        drawRect(brush = base, alpha = 0.98f)
                        drawCircle(brush = glow1, radius = size.minDimension * 0.85f, center = Offset(size.width * c1x, size.height * c1y), alpha = 0.55f)
                        drawCircle(brush = glow2, radius = size.minDimension * 0.95f, center = Offset(size.width * c2x, size.height * c2y), alpha = 0.60f)
                    }
                }
                .blur(18.dp)
        )

        Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.16f)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                PhotoBubble(photoUri, accentColor, Modifier.size(52.dp), avatarEmoji = avatarEmoji)
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (subtitle.isNotBlank()) {
                        Text(subtitle, color = Color.White.copy(alpha = 0.78f))
                    }
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(badgeText, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(26.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                lines.forEach { (label, value) -> GlassTicketLine(label, value) }
            }

            if (qrValue.isNotBlank()) {
                GlassTicketQr(value = qrValue, label = qrLabel)
            }
        }
    }
}

@Composable
private fun GlassTicketLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.74f))
        Spacer(Modifier.width(16.dp))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GlassTicketQr(value: String, label: String) {
    val bitmap = remember(value) { generateQrBitmap(value) }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        bitmap?.let {
            androidx.compose.foundation.Image(
                bitmap = it.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White.copy(alpha = 0.94f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(28.dp))
                    .padding(14.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.titleMedium, color = Color.White)
    }
}

private data class SparkleDot(
    val baseX: Float,
    val baseY: Float,
    val radiusDp: Float,
    val drift: Float,
    val speed: Float,
    val phase: Float,
    val intensity: Float
)

@Composable
private fun AnimatedGraySparkleBackground(modifier: Modifier = Modifier, seed: Color, alpha: Float = 1f) {
    val transition = rememberInfiniteTransition(label = "sparkle-bg")
    val time01 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "time-01"
    )
    val twinkle01 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "twinkle-01"
    )

    val seedInt = remember(seed) { seed.toArgb() }
    val dots = remember(seedInt) {
        val rnd = Random(seedInt)
        List(26) {
            SparkleDot(
                baseX = rnd.nextFloat(),
                baseY = rnd.nextFloat(),
                radiusDp = 1.1f + rnd.nextFloat() * 2.0f,
                drift = 0.035f + rnd.nextFloat() * 0.07f,
                speed = 0.18f + rnd.nextFloat() * 0.55f,
                phase = rnd.nextFloat(),
                intensity = 0.35f + rnd.nextFloat() * 0.75f
            )
        }
    }

    Box(
        modifier = modifier.drawWithCache {
            val tau = (2f * PI).toFloat()

            val gX = 0.5f + 0.5f * sin(tau * time01)
            val gY = 0.5f + 0.5f * cos(tau * (time01 * 0.82f) + 0.6f)

            val start = Offset(size.width * (0.10f + 0.80f * gX), size.height * (0.10f + 0.80f * gY))
            val end = Offset(size.width * (0.90f - 0.80f * gY), size.height * (0.90f - 0.80f * gX))

            val base = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0B0C0F),
                    Color(0xFF171A21),
                    Color(0xFF2A2E37),
                    Color(0xFF101216)
                ),
                start = start,
                end = end
            )

            onDrawBehind {
                drawRect(brush = base, alpha = alpha)

                val minDim = size.minDimension
                listOf(0.15f, 0.48f, 0.79f).forEachIndexed { i, phase ->
                    val bx = 0.5f + 0.45f * sin(tau * (time01 * (0.56f + i * 0.07f) + phase))
                    val by = 0.5f + 0.45f * cos(tau * (time01 * (0.49f + i * 0.06f) + phase * 1.13f))
                    val center = Offset(size.width * bx, size.height * by)
                    val radius = minDim * (0.55f + 0.08f * sin(tau * (time01 + phase)))
                    val blob = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    )
                    drawCircle(brush = blob, radius = radius, center = center, alpha = alpha * 0.85f)
                }

                dots.forEach { dot ->
                    val angle = tau * (time01 * dot.speed + dot.phase)
                    val xN = wrap01(dot.baseX + dot.drift * sin(angle))
                    val yN = wrap01(dot.baseY + dot.drift * cos(angle * 0.93f))

                    val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(tau * (twinkle01 * (0.6f + dot.speed) + dot.phase)))
                    val a = alpha * dot.intensity * twinkle

                    val center = Offset(size.width * xN, size.height * yN)
                    val r = dot.radiusDp.dp.toPx()

                    drawCircle(color = Color.White.copy(alpha = a * 0.06f), radius = r * 12f, center = center)
                    drawCircle(color = Color.White.copy(alpha = a * 0.16f), radius = r * 5f, center = center)
                    drawCircle(color = Color.White.copy(alpha = a), radius = r, center = center)
                }
            }
        }
    )
}

@Composable
private fun ColorfulLoadingOverlay(visible: Boolean, message: String, accentColor: Color) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(140)) + scaleIn(
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            initialScale = 0.94f
        ),
        exit = fadeOut(tween(160)) + scaleOut(
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
            targetScale = 0.985f
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            AnimatedGraySparkleBackground(Modifier.matchParentSize(), seed = accentColor, alpha = 1f)
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.55f)))
            Surface(
                modifier = Modifier.align(Alignment.Center),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Aguarde…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun wrap01(value: Float): Float {
    val wrapped = value - floor(value)
    return if (wrapped < 0f) wrapped + 1f else wrapped
}

@Composable
private fun ExpandablePassCard(
    accentColor: Color,
    photoUri: String,
    avatarEmoji: String,
    badgeText: String,
    title: String,
    subtitle: String,
    tertiary: String,
    lines: List<Pair<String, String>>,
    qrValue: String,
    qrLabel: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    footer: (@Composable (() -> Unit))?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp)),
        color = accentColor
    ) {
        Column(Modifier.fillMaxWidth().padding(2.dp)) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 140.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .clickable { onToggleExpanded() },
                shape = RoundedCornerShape(30.dp),
                color = if (expanded) accentColor else MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        (fadeIn(tween(220)) + expandVertically(tween(240))).togetherWith(
                            fadeOut(tween(120)) + shrinkVertically(tween(220))
                        ).using(SizeTransform(clip = false))
                    },
                    label = "expandable-pass"
                ) { isExpanded ->
                    if (isExpanded) {
                        PassTicket(
                            accentColor = accentColor,
                            photoUri = photoUri,
                            avatarEmoji = avatarEmoji,
                            badgeText = badgeText,
                            title = title,
                            subtitle = subtitle,
                            tertiary = tertiary,
                            lines = lines,
                            qrValue = qrValue,
                            qrLabel = qrLabel
                        )
                    } else {
                        PassCompact(
                            accentColor = accentColor,
                            photoUri = photoUri,
                            avatarEmoji = avatarEmoji,
                            badgeText = badgeText,
                            title = title,
                            subtitle = subtitle,
                            tertiary = tertiary,
                            lines = lines,
                            qrValue = qrValue,
                            qrLabel = qrLabel
                        )
                    }
                }
            }
            if (footer != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Box(Modifier.fillMaxWidth().padding(16.dp)) { footer() }
                }
            }
        }
    }
}

@Composable
private fun PassCompact(
    accentColor: Color,
    photoUri: String,
    avatarEmoji: String,
    badgeText: String,
    title: String,
    subtitle: String,
    tertiary: String,
    lines: List<Pair<String, String>>,
    qrValue: String,
    qrLabel: String
) {
    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PhotoBubble(photoUri, accentColor, Modifier.size(64.dp), avatarEmoji = avatarEmoji)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(subtitle)
                Text(tertiary, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = RoundedCornerShape(999.dp), color = accentColor.copy(alpha = 0.18f)) {
                Text(badgeText, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = accentColor)
            }
        }
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(accentColor).padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                lines.take(3).forEach { (_, value) -> Text(value, color = Color.White) }
            }
        }
        if (qrValue.isNotBlank()) {
            QrCodeCard(value = qrValue, label = qrLabel)
        }
    }
}

@Composable
private fun PassTicket(
    accentColor: Color,
    photoUri: String,
    avatarEmoji: String,
    badgeText: String,
    title: String,
    subtitle: String,
    tertiary: String,
    lines: List<Pair<String, String>>,
    qrValue: String,
    qrLabel: String
) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(26.dp), color = Color.White) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    PhotoBubble(photoUri, accentColor, Modifier.size(52.dp), avatarEmoji = avatarEmoji)
                    Column(Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.clip(RoundedCornerShape(18.dp)).background(accentColor.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(badgeText, color = accentColor, fontWeight = FontWeight.Medium)
                    }
                }

                if (tertiary.isNotBlank()) {
                    Text(tertiary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        lines.forEach { (label, value) -> TicketLine(label, value) }
                    }
                }

                if (qrValue.isNotBlank()) {
                    TicketQr(value = qrValue, label = qrLabel)
                }
            }
        }
    }
}

@Composable
private fun TicketLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TicketQr(value: String, label: String) {
    val bitmap = remember(value) { generateQrBitmap(value) }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        bitmap?.let {
            androidx.compose.foundation.Image(
                bitmap = it.asImageBitmap(),
                contentDescription = label,
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color.White)
                    .padding(14.dp)
            )
        }
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable private fun ColorPicker(selectedColor: String, onSelectColor: (String) -> Unit) {
    val palette = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer).map { "#%08X".format(it.toArgb()) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { palette.forEach { hex -> FilterChip(selected = selectedColor.equals(hex, true), onClick = { onSelectColor(hex) }, label = { Text(" ") }, leadingIcon = { Box(Modifier.size(24.dp).clip(CircleShape).background(parseColor(hex)).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)) }) } }
}

@Composable private fun PhotoBubble(photoUri: String, fallbackColor: Color, modifier: Modifier = Modifier, avatarEmoji: String = "") {
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(fallbackColor.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        if (photoUri.isNotBlank()) {
            AndroidView(
                factory = { context ->
                    ImageView(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        clipToOutline = true
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { it.setImageURI(Uri.parse(photoUri)) }
            )
        } else if (avatarEmoji.isNotBlank()) {
            Text(avatarEmoji.trim(), style = MaterialTheme.typography.headlineSmall, color = Color.White)
        } else {
            Icon(Icons.Rounded.Person, null, tint = fallbackColor, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable private fun WalletActionButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(48.dp), shape = RoundedCornerShape(18.dp)) {
        Icon(Icons.Rounded.Save, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun QrCodeCard(value: String, label: String, showValueText: Boolean = true) {
    val bitmap = remember(value) { generateQrBitmap(value) }
    ElevatedCard(shape = RoundedCornerShape(24.dp), colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            bitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = label,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White)
                        .padding(8.dp)
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                if (showValueText) {
                    Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun parseColor(hex: String): Color = Color(runCatching { android.graphics.Color.parseColor(hex) }.getOrDefault(0xFF6750A4.toInt()))

private fun generateQrBitmap(value: String): Bitmap? = runCatching {
    val size = 512
    val matrix = MultiFormatWriter().encode(
        value,
        BarcodeFormat.QR_CODE,
        size,
        size,
        mapOf(EncodeHintType.MARGIN to 1)
    )
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }
}.getOrNull()

private fun shareFile(context: android.content.Context, uri: Uri, mime: String, chooserTitle: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

private fun showToast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}

private fun tr(language: AppLanguage, key: String): String {
    val pt = mapOf("home" to "Home", "create" to "Criar", "saved" to "Salvos", "settings" to "Config.", "home_head" to "Recentes", "home_sub" to "", "home_empty_title" to "Sem cartões recentes", "home_empty_body" to "Crie um passe para vê-lo aqui.", "create_head" to "Criar cartão", "create_sub" to "Abra o campo criar e monte o passe.", "create_hint" to "Personalize apenas a cor do passe.", "saved_head" to "Todos os cartões salvos", "saved_sub" to "Aqui ficam todos os cartões guardados.", "saved_count" to "cartões salvos", "saved_empty_title" to "Sem cartões salvos", "saved_empty_body" to "Os cartões criados aparecem aqui.", "settings_head" to "Configurações", "settings_sub" to "Idioma, segurança, notificações e backup.", "version" to "Versão", "github" to "Minha conta do GitHub", "language" to "Idioma do app", "contacts" to "Contatos", "terms" to "Termo", "open_create" to "Abrir criar", "edit" to "Editar", "delete" to "Excluir", "pass" to "Passe", "add_photo" to "Adicionar foto", "change_photo" to "Trocar foto", "photo_hint" to "A foto fica salva no app. Para aparecer no Google Wallet, use uma URL pública.", "name" to "Nome", "emoji" to "Emoji", "role" to "Cargo", "phone" to "Celular", "email" to "Email", "instagram" to "Instagram", "linkedin" to "LinkedIn", "url" to "URL", "note" to "Nota", "qr_code" to "QR code", "wallet_photo" to "URL pública da foto para o Wallet", "pass_color" to "Cor do passe", "create_pass" to "Criar passe", "clear" to "Limpar", "wallet" to "Google Wallet", "wallet_on" to "Google Wallet disponível neste aparelho.", "wallet_off" to "Google Wallet indisponível ou não elegível neste aparelho.", "backend" to "URL do backend JWT", "wallet_save" to "Salvar configuração do Wallet", "wallet_add" to "Salvar no Wallet")
    val pt2 = pt + mapOf("qr_pick" to "Selecionar imagem de QR Code", "qr_change" to "Trocar QR", "qr_remove" to "Remover QR", "qr_scan" to "Escanear QR", "vcf_import" to "Importar vCard (.vcf)", "backup" to "Backup", "backup_export" to "Exportar", "backup_import" to "Importar", "share" to "Compartilhar", "share_image" to "Imagem (PNG)", "share_pdf" to "PDF", "share_vcard" to "vCard (.vcf)", "share_contacts" to "Salvar nos contatos", "presentation_mode" to "Modo apresentação", "presentation_started" to "Modo apresentação ativado.", "live_updates_denied" to "Permita Live Updates para aparecer na Now Bar.", "premium_ui_open" to "Abrir UI Premium", "premium_ui_hint" to "Nova interface com estilo premium (Apple + Material You).")
    val en = mapOf("home" to "Home", "create" to "Create", "saved" to "Saved", "settings" to "Settings", "home_head" to "Recents", "home_sub" to "", "home_empty_title" to "No recent cards", "home_empty_body" to "Create a pass to see it here.", "create_head" to "Create card", "create_sub" to "Open the create area and build the pass.", "create_hint" to "Only customize the pass color.", "saved_head" to "All saved cards", "saved_sub" to "Every saved card appears here.", "saved_count" to "saved cards", "saved_empty_title" to "No saved cards", "saved_empty_body" to "Created cards appear here.", "settings_head" to "Settings", "settings_sub" to "Language, security, notifications and backup.", "version" to "Version", "github" to "My GitHub account", "language" to "App language", "contacts" to "Contacts", "terms" to "Terms", "open_create" to "Open create", "edit" to "Edit", "delete" to "Delete", "pass" to "Pass", "add_photo" to "Add photo", "change_photo" to "Change photo", "photo_hint" to "The photo is saved in the app. To show in Google Wallet, use a public image URL.", "name" to "Name", "emoji" to "Emoji", "role" to "Role", "phone" to "Phone", "email" to "Email", "instagram" to "Instagram", "linkedin" to "LinkedIn", "url" to "URL", "note" to "Note", "qr_code" to "QR code", "wallet_photo" to "Public photo URL for Wallet", "pass_color" to "Pass color", "create_pass" to "Create pass", "clear" to "Clear", "wallet" to "Google Wallet", "wallet_on" to "Google Wallet is available on this device.", "wallet_off" to "Google Wallet is unavailable or not eligible on this device.", "backend" to "JWT backend URL", "wallet_save" to "Save Wallet settings", "wallet_add" to "Save to Wallet", "qr_pick" to "Select QR Code image", "qr_change" to "Change QR", "qr_remove" to "Remove QR")
    val en2 = en + mapOf("qr_scan" to "Scan QR", "vcf_import" to "Import vCard (.vcf)", "backup" to "Backup", "backup_export" to "Export", "backup_import" to "Import", "share" to "Share", "share_image" to "Image (PNG)", "share_pdf" to "PDF", "share_vcard" to "vCard (.vcf)", "share_contacts" to "Save to contacts", "presentation_mode" to "Presentation mode", "presentation_started" to "Presentation mode enabled.", "live_updates_denied" to "Allow Live Updates to appear in the Now Bar.", "premium_ui_open" to "Open Premium UI", "premium_ui_hint" to "New premium interface (Apple + Material You).")
    val zh = mapOf("home" to "首页", "create" to "创建", "saved" to "已保存", "settings" to "设置", "home_head" to "最近", "home_sub" to "", "home_empty_title" to "没有最近卡片", "home_empty_body" to "创建通行证后会显示在这里。", "create_head" to "创建卡片", "create_sub" to "打开创建区域并制作通行证。", "create_hint" to "只允许自定义通行证颜色。", "saved_head" to "所有已保存卡片", "saved_sub" to "所有保存的卡片都在这里。", "saved_count" to "张已保存卡片", "saved_empty_title" to "没有已保存卡片", "saved_empty_body" to "已创建的卡片会显示在这里。", "settings_head" to "设置", "settings_sub" to "语言、安全、通知和备份。", "version" to "版本", "github" to "我的 GitHub 账号", "language" to "应用语言", "contacts" to "联系方式", "terms" to "条款", "open_create" to "打开创建", "edit" to "编辑", "delete" to "删除", "pass" to "通行证", "add_photo" to "添加照片", "change_photo" to "更换照片", "photo_hint" to "照片会保存在应用中。若要显示在 Google Wallet 中，请使用公开图片链接。", "name" to "姓名", "emoji" to "Emoji", "role" to "职位", "phone" to "手机", "email" to "邮箱", "instagram" to "Instagram", "linkedin" to "LinkedIn", "url" to "链接", "note" to "备注", "qr_code" to "二维码", "wallet_photo" to "Wallet 公开照片链接", "pass_color" to "通行证颜色", "create_pass" to "创建通行证", "clear" to "清空", "wallet" to "Google Wallet", "wallet_on" to "此设备支持 Google Wallet。", "wallet_off" to "此设备不支持 Google Wallet。", "backend" to "JWT 后端地址", "wallet_save" to "保存 Wallet 设置", "wallet_add" to "保存到 Wallet", "qr_pick" to "选择二维码图片", "qr_change" to "更换二维码", "qr_remove" to "移除二维码", "qr_scan" to "扫描二维码", "vcf_import" to "导入 vCard (.vcf)", "backup" to "备份", "backup_export" to "导出", "backup_import" to "导入", "share" to "分享", "share_image" to "图片 (PNG)", "share_pdf" to "PDF", "share_vcard" to "vCard (.vcf)", "share_contacts" to "保存到联系人", "presentation_mode" to "演示模式", "presentation_started" to "演示模式已启用。", "live_updates_denied" to "允许实时更新以在 Now Bar 中显示。", "premium_ui_open" to "打开高级界面", "premium_ui_hint" to "新界面（Apple + Material You）。")
    return when (language) { AppLanguage.EN -> en2[key] ?: key; AppLanguage.ZH -> zh[key] ?: key; else -> pt2[key] ?: key }
}
