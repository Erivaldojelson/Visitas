package com.monst.transfiranow.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.ImageView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.monst.transfiranow.BuildConfig
import com.monst.transfiranow.data.AppLanguage
import com.monst.transfiranow.data.CardDraft
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.premium.PremiumCardsActivity
import com.monst.transfiranow.ui.theme.TransfiraNowTheme

private enum class AppTab { HOME, CREATE, SAVED, SETTINGS }

@Composable
fun VisitasApp(
    onPickPhoto: () -> Unit,
    onPickQrCode: () -> Unit,
    onSaveToWallet: (VisitingCard) -> Unit,
    viewModel: VisitasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var tab by remember { mutableStateOf(AppTab.HOME) }
    val t: (String) -> String = { key -> tr(uiState.appLanguage, key) }
    TransfiraNowTheme(dynamicColor = true, accentColor = parseColor(uiState.draft.passColor)) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { PillBar(tab, t) { tab = it } }
        ) { padding ->
            when (tab) {
                AppTab.HOME -> CardsScreen(padding, t("home_head"), t("home_sub"), uiState.statusMessage, uiState.cards.take(10), t, false, onSaveToWallet, {}) {
                    viewModel.editCard(it); tab = AppTab.CREATE
                }
                AppTab.CREATE -> CreateScreen(padding, uiState.draft, uiState.canUseGoogleWallet, uiState.walletIssuerId, uiState.walletClassSuffix, uiState.walletBackendUrl, t, onPickPhoto, onPickQrCode, viewModel::updateDraft, viewModel::updateWalletSettings, viewModel::saveDraft, viewModel::persistWalletSettings, viewModel::clearDraft, viewModel::clearDraftQr)
                AppTab.SAVED -> CardsScreen(padding, t("saved_head"), t("saved_sub"), "${uiState.cards.size} ${t("saved_count")}", uiState.cards, t, true, onSaveToWallet, { viewModel.deleteCard(it.id) }) {
                    viewModel.editCard(it); tab = AppTab.CREATE
                }
                AppTab.SETTINGS -> SettingsScreen(padding, uiState.appLanguage, uiState.canUseGoogleWallet, uiState.walletIssuerId, uiState.walletClassSuffix, uiState.walletBackendUrl, t, viewModel::updateLanguage, viewModel::updateWalletSettings, viewModel::persistWalletSettings)
            }
        }
    }
}

@Composable
private fun PillBar(selected: AppTab, t: (String) -> String, onSelect: (AppTab) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), shape = RoundedCornerShape(999.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PillItem(AppTab.HOME, selected, t("home"), Icons.Rounded.Home, onSelect)
            PillItem(AppTab.CREATE, selected, t("create"), Icons.Rounded.Add, onSelect)
            PillItem(AppTab.SAVED, selected, t("saved"), Icons.Rounded.Style, onSelect)
            PillItem(AppTab.SETTINGS, selected, t("settings"), Icons.Rounded.Settings, onSelect)
        }
    }
}

@Composable
private fun RowScope.PillItem(tab: AppTab, selected: AppTab, label: String, icon: ImageVector, onSelect: (AppTab) -> Unit) {
    val active = tab == selected
    Surface(modifier = Modifier.weight(1f).clip(RoundedCornerShape(999.dp)).clickable { onSelect(tab) }, color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CardsScreen(padding: PaddingValues, title: String, subtitle: String, message: String, cards: List<VisitingCard>, t: (String) -> String, showDelete: Boolean, onSaveToWallet: (VisitingCard) -> Unit, onDelete: (VisitingCard) -> Unit, onEdit: (VisitingCard) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ListHeader(title = title, subtitle = subtitle, message = message) }
        if (cards.isEmpty()) item { EmptyCard(t(if (showDelete) "saved_empty_title" else "home_empty_title"), t(if (showDelete) "saved_empty_body" else "home_empty_body")) }
        else items(cards, key = { it.id }) { card -> SavedPassCard(card, t, showDelete, { onEdit(card) }, { onDelete(card) }, { onSaveToWallet(card) }) }
    }
}

@Composable
private fun ListHeader(title: String, subtitle: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CreateScreen(
    padding: PaddingValues,
    draft: CardDraft,
    canUseGoogleWallet: Boolean,
    walletIssuerId: String,
    walletClassSuffix: String,
    walletBackendUrl: String,
    t: (String) -> String,
    onPickPhoto: () -> Unit,
    onPickQrCode: () -> Unit,
    onDraftChange: ((CardDraft) -> CardDraft) -> Unit,
    onWalletSettingsChange: (String?, String?, String?) -> Unit,
    onCreatePass: () -> Unit,
    onPersistWalletSettings: () -> Unit,
    onClearDraft: () -> Unit,
    onClearQr: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Hero(t("create_head"), t("create_sub"), t("create_hint")) }
        item { DraftPreview(draft, t("pass"), t) }
        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        PhotoBubble(draft.photoUri, parseColor(draft.passColor), Modifier.size(84.dp))
                        FilledTonalButton(onClick = onPickPhoto) { Icon(Icons.Rounded.AddPhotoAlternate, null); Spacer(Modifier.width(8.dp)); Text(if (draft.photoUri.isBlank()) t("add_photo") else t("change_photo")) }
                    }
                    Text(t("photo_hint"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            EditorCard(draft, canUseGoogleWallet, walletIssuerId, walletClassSuffix, walletBackendUrl, t, onPickQrCode, onDraftChange, onWalletSettingsChange, onCreatePass, onPersistWalletSettings, onClearDraft, onClearQr)
        }
    }
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
private fun SettingsScreen(padding: PaddingValues, currentLanguage: AppLanguage, canUseGoogleWallet: Boolean, walletIssuerId: String, walletClassSuffix: String, walletBackendUrl: String, t: (String) -> String, onLanguageSelected: (AppLanguage) -> Unit, onWalletSettingsChange: (String?, String?, String?) -> Unit, onPersistWalletSettings: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 120.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Hero(t("settings_head"), t("settings_sub"), "${t("version")}: ${BuildConfig.VERSION_NAME}") }
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
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("UI", style = MaterialTheme.typography.titleLarge)
                    Text(t("premium_ui_hint"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilledTonalButton(onClick = { context.startActivity(Intent(context, PremiumCardsActivity::class.java)) }) {
                        Text(t("premium_ui_open"))
                    }
                }
            }
        }
        item { WalletCard(canUseGoogleWallet, walletIssuerId, walletClassSuffix, walletBackendUrl, t, onWalletSettingsChange, onPersistWalletSettings) }
        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t("version"), style = MaterialTheme.typography.titleMedium)
                    Text(BuildConfig.VERSION_NAME)
                    Text(t("github"), style = MaterialTheme.typography.titleMedium)
                    Text("Erivaldojelson")
                }
            }
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
private fun DraftPreview(draft: CardDraft, passLabel: String, t: (String) -> String) {
    val c = parseColor(draft.passColor)
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExpandablePassCard(
        accentColor = c,
        photoUri = draft.photoUri,
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
        expanded = expanded,
        onToggleExpanded = { expanded = !expanded },
        footer = null
    )
}

@Composable
private fun SavedPassCard(card: VisitingCard, t: (String) -> String, showDelete: Boolean, onEdit: () -> Unit, onDelete: () -> Unit, onSaveToWallet: () -> Unit) {
    val accentColor = parseColor(card.passColor)
    val subtitle = card.role.ifBlank { "-" }
    val tertiary = card.website.ifBlank { "Sem URL" }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(292.dp)
                    .clip(RoundedCornerShape(36.dp))
            ) {
                if (card.photoUri.isNotBlank()) {
                    AsyncImage(
                        model = card.photoUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
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

                Surface(
                    color = Color.Black.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    Text(
                        t("pass"),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PhotoBubble(card.photoUri, accentColor, Modifier.size(44.dp))
                    Spacer(Modifier.height(2.dp))
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
                        subtitle,
                        color = Color.White.copy(alpha = 0.90f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        tertiary,
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text(t(if (showDelete) "edit" else "open_create"))
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

@Composable
private fun ExpandablePassCard(
    accentColor: Color,
    photoUri: String,
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
            PhotoBubble(photoUri, accentColor, Modifier.size(64.dp))
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
                    PhotoBubble(photoUri, accentColor, Modifier.size(52.dp))
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

@Composable private fun PhotoBubble(photoUri: String, fallbackColor: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(fallbackColor.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
        if (photoUri.isNotBlank()) AndroidView(factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; clipToOutline = true } }, modifier = Modifier.fillMaxSize(), update = { it.setImageURI(Uri.parse(photoUri)) })
        else Icon(Icons.Rounded.Person, null, tint = fallbackColor, modifier = Modifier.size(34.dp))
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

private fun tr(language: AppLanguage, key: String): String {
    val pt = mapOf("home" to "Home", "create" to "Criar", "saved" to "Salvos", "settings" to "Config.", "home_head" to "Recentes", "home_sub" to "A tela inicial mostra até dez cartões recentes.", "home_empty_title" to "Sem cartões recentes", "home_empty_body" to "Crie um passe para vê-lo aqui.", "create_head" to "Criar cartão", "create_sub" to "Abra o campo criar e monte o passe.", "create_hint" to "Personalize apenas a cor do passe.", "saved_head" to "Todos os cartões salvos", "saved_sub" to "Aqui ficam todos os cartões guardados.", "saved_count" to "cartões salvos", "saved_empty_title" to "Sem cartões salvos", "saved_empty_body" to "Os cartões criados aparecem aqui.", "settings_head" to "Configurações", "settings_sub" to "Versão, GitHub, idioma e Google Wallet.", "version" to "Versão", "github" to "Minha conta do GitHub", "language" to "Idioma do app", "open_create" to "Abrir criar", "edit" to "Editar", "delete" to "Excluir", "pass" to "Passe", "add_photo" to "Adicionar foto", "change_photo" to "Trocar foto", "photo_hint" to "A foto fica salva no app. Para aparecer no Google Wallet, use uma URL pública.", "name" to "Nome", "role" to "Cargo", "phone" to "Celular", "email" to "Email", "instagram" to "Instagram", "linkedin" to "LinkedIn", "url" to "URL", "note" to "Nota", "qr_code" to "QR code", "wallet_photo" to "URL pública da foto para o Wallet", "pass_color" to "Cor do passe", "create_pass" to "Criar passe", "clear" to "Limpar", "wallet" to "Google Wallet", "wallet_on" to "Google Wallet disponível neste aparelho.", "wallet_off" to "Google Wallet indisponível ou não elegível neste aparelho.", "backend" to "URL do backend JWT", "wallet_save" to "Salvar configuração do Wallet", "wallet_add" to "Salvar no Wallet")
    val pt2 = pt + mapOf("qr_pick" to "Selecionar imagem de QR Code", "qr_change" to "Trocar QR", "qr_remove" to "Remover QR", "premium_ui_open" to "Abrir UI Premium", "premium_ui_hint" to "Nova interface com estilo premium (Apple + Material You).")
    val en = mapOf("home" to "Home", "create" to "Create", "saved" to "Saved", "settings" to "Settings", "home_head" to "Recents", "home_sub" to "The home screen shows up to ten recent cards.", "home_empty_title" to "No recent cards", "home_empty_body" to "Create a pass to see it here.", "create_head" to "Create card", "create_sub" to "Open the create area and build the pass.", "create_hint" to "Only customize the pass color.", "saved_head" to "All saved cards", "saved_sub" to "Every saved card appears here.", "saved_count" to "saved cards", "saved_empty_title" to "No saved cards", "saved_empty_body" to "Created cards appear here.", "settings_head" to "Settings", "settings_sub" to "Version, GitHub, language and Google Wallet.", "version" to "Version", "github" to "My GitHub account", "language" to "App language", "open_create" to "Open create", "edit" to "Edit", "delete" to "Delete", "pass" to "Pass", "add_photo" to "Add photo", "change_photo" to "Change photo", "photo_hint" to "The photo is saved in the app. To show in Google Wallet, use a public image URL.", "name" to "Name", "role" to "Role", "phone" to "Phone", "email" to "Email", "instagram" to "Instagram", "linkedin" to "LinkedIn", "url" to "URL", "note" to "Note", "qr_code" to "QR code", "wallet_photo" to "Public photo URL for Wallet", "pass_color" to "Pass color", "create_pass" to "Create pass", "clear" to "Clear", "wallet" to "Google Wallet", "wallet_on" to "Google Wallet is available on this device.", "wallet_off" to "Google Wallet is unavailable or not eligible on this device.", "backend" to "JWT backend URL", "wallet_save" to "Save Wallet settings", "wallet_add" to "Save to Wallet", "qr_pick" to "Select QR Code image", "qr_change" to "Change QR", "qr_remove" to "Remove QR")
    val en2 = en + mapOf("premium_ui_open" to "Open Premium UI", "premium_ui_hint" to "New premium interface (Apple + Material You).")
    val zh = mapOf("home" to "首页", "create" to "创建", "saved" to "已保存", "settings" to "设置", "home_head" to "最近", "home_sub" to "首页最多显示十张最近卡片。", "home_empty_title" to "没有最近卡片", "home_empty_body" to "创建通行证后会显示在这里。", "create_head" to "创建卡片", "create_sub" to "打开创建区域并制作通行证。", "create_hint" to "只允许自定义通行证颜色。", "saved_head" to "所有已保存卡片", "saved_sub" to "所有保存的卡片都在这里。", "saved_count" to "张已保存卡片", "saved_empty_title" to "没有已保存卡片", "saved_empty_body" to "已创建的卡片会显示在这里。", "settings_head" to "设置", "settings_sub" to "版本、GitHub、语言和 Google Wallet。", "version" to "版本", "github" to "我的 GitHub 账号", "language" to "应用语言", "open_create" to "打开创建", "edit" to "编辑", "delete" to "删除", "pass" to "通行证", "add_photo" to "添加照片", "change_photo" to "更换照片", "photo_hint" to "照片会保存在应用中。若要显示在 Google Wallet 中，请使用公开图片链接。", "name" to "姓名", "role" to "职位", "phone" to "手机", "email" to "邮箱", "instagram" to "Instagram", "linkedin" to "LinkedIn", "url" to "链接", "note" to "备注", "qr_code" to "二维码", "wallet_photo" to "Wallet 公开照片链接", "pass_color" to "通行证颜色", "create_pass" to "创建通行证", "clear" to "清空", "wallet" to "Google Wallet", "wallet_on" to "此设备支持 Google Wallet。", "wallet_off" to "此设备不支持 Google Wallet。", "backend" to "JWT 后端地址", "wallet_save" to "保存 Wallet 设置", "wallet_add" to "保存到 Wallet", "qr_pick" to "选择二维码图片", "qr_change" to "更换二维码", "qr_remove" to "移除二维码", "premium_ui_open" to "打开高级界面", "premium_ui_hint" to "新界面（Apple + Material You）。")
    return when (language) { AppLanguage.EN -> en2[key] ?: key; AppLanguage.ZH -> zh[key] ?: key; else -> pt2[key] ?: key }
}
