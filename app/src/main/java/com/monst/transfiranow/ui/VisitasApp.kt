package com.monst.transfiranow.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monst.transfiranow.data.CardDraft
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.ui.theme.TransfiraNowTheme

@Composable
fun VisitasApp(
    onSaveToWallet: (VisitingCard) -> Unit,
    viewModel: VisitasViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TransfiraNowTheme(
        dynamicColor = false,
        accentColor = Color(runCatching { android.graphics.Color.parseColor(uiState.draft.hexColor) }.getOrDefault(0xFF1E3A8A.toInt()))
    ) {
        Scaffold { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeaderCard(uiState.statusMessage)
                }
                item {
                    CardPreview(uiState.draft)
                }
                item {
                    CardEditor(
                        draft = uiState.draft,
                        walletIssuerId = uiState.walletIssuerId,
                        walletClassSuffix = uiState.walletClassSuffix,
                        walletBackendUrl = uiState.walletBackendUrl,
                        canUseGoogleWallet = uiState.canUseGoogleWallet,
                        onDraftChange = viewModel::updateDraft,
                        onWalletSettingsChange = viewModel::updateWalletSettings,
                        onSaveCard = viewModel::saveDraft,
                        onPersistWalletSettings = viewModel::persistWalletSettings,
                        onClearDraft = viewModel::clearDraft
                    )
                }
                item {
                    Text("Cartões salvos", style = MaterialTheme.typography.titleMedium)
                }
                if (uiState.cards.isEmpty()) {
                    item {
                        EmptyStateCard()
                    }
                } else {
                    items(uiState.cards, key = { it.id }) { card ->
                        SavedCardRow(
                            card = card,
                            onEdit = { viewModel.editCard(card) },
                            onDelete = { viewModel.deleteCard(card.id) },
                            onSaveToWallet = { onSaveToWallet(card) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(message: String) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Visitas", style = MaterialTheme.typography.headlineMedium)
            Text("Crie cartões pessoais com cor customizada, contatos, redes sociais e exportação para Google Wallet.", style = MaterialTheme.typography.bodyMedium)
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun CardPreview(draft: CardDraft) {
    val color = Color(runCatching { android.graphics.Color.parseColor(draft.hexColor) }.getOrDefault(0xFF1E3A8A.toInt()))
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(draft.name.ifBlank { "Seu nome" }, style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text(draft.role.ifBlank { "Cargo ou descrição" }, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.92f))
            Text(draft.phone.ifBlank { "+55 00 00000-0000" }, color = Color.White)
            Text(draft.email.ifBlank { "email@exemplo.com" }, color = Color.White)
            Text(draft.website.ifBlank { "https://seusite.com" }, color = Color.White)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CardEditor(
    draft: CardDraft,
    walletIssuerId: String,
    walletClassSuffix: String,
    walletBackendUrl: String,
    canUseGoogleWallet: Boolean,
    onDraftChange: ((CardDraft) -> CardDraft) -> Unit,
    onWalletSettingsChange: (String?, String?, String?) -> Unit,
    onSaveCard: () -> Unit,
    onPersistWalletSettings: () -> Unit,
    onClearDraft: () -> Unit
) {
    val palette = listOf("#1E3A8A", "#7C3AED", "#EA580C", "#0F766E", "#BE123C", "#111827")
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(draft.name, { value -> onDraftChange { it.copy(name = value) } }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.role, { value -> onDraftChange { it.copy(role = value) } }, label = { Text("Cargo") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.phone, { value -> onDraftChange { it.copy(phone = value) } }, label = { Text("Celular") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.email, { value -> onDraftChange { it.copy(email = value) } }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.instagram, { value -> onDraftChange { it.copy(instagram = value) } }, label = { Text("Instagram") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.linkedin, { value -> onDraftChange { it.copy(linkedin = value) } }, label = { Text("LinkedIn") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.website, { value -> onDraftChange { it.copy(website = value) } }, label = { Text("URL") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.note, { value -> onDraftChange { it.copy(note = value) } }, label = { Text("Nota") }, modifier = Modifier.fillMaxWidth())
            Text("Cor do cartão", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                palette.forEach { hex ->
                    FilterChip(
                        selected = draft.hexColor == hex,
                        onClick = { onDraftChange { it.copy(hexColor = hex) } },
                        label = { Text(hex) },
                        leadingIcon = {
                            Box(
                                Modifier.size(18.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))).border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    CircleShape
                                )
                            )
                        }
                    )
                }
            }
            Text("Google Wallet", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(walletIssuerId, { onWalletSettingsChange(it, null, null) }, label = { Text("Issuer ID") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(walletClassSuffix, { onWalletSettingsChange(null, it, null) }, label = { Text("Class suffix") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(walletBackendUrl, { onWalletSettingsChange(null, null, it) }, label = { Text("URL do backend JWT") }, modifier = Modifier.fillMaxWidth())
            Text(
                if (canUseGoogleWallet) "Google Wallet disponível neste aparelho." else "Google Wallet indisponível ou não elegível neste aparelho.",
                style = MaterialTheme.typography.bodySmall,
                color = if (canUseGoogleWallet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSaveCard) {
                    Icon(Icons.Rounded.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Salvar")
                }
                TextButton(onClick = onPersistWalletSettings) { Text("Salvar Wallet") }
                TextButton(onClick = onClearDraft) { Text("Limpar") }
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Nenhum cartão salvo", style = MaterialTheme.typography.titleMedium)
            Text("Salve seu primeiro cartão e depois exporte para o Google Wallet.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SavedCardRow(
    card: VisitingCard,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSaveToWallet: () -> Unit
) {
    val color = Color(runCatching { android.graphics.Color.parseColor(card.hexColor) }.getOrDefault(0xFF1E3A8A.toInt()))
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(card.name, style = MaterialTheme.typography.titleMedium)
                    Text(card.role.ifBlank { "Sem cargo" }, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onEdit) { Text("Editar") }
            }
            Text(card.phone.ifBlank { card.email.ifBlank { card.website.ifBlank { "Sem contatos extras" } } }, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveToWallet) {
                    Icon(Icons.Rounded.Wallet, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Wallet")
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Excluir")
                }
            }
        }
    }
}
