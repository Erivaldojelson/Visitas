package com.monst.transfiranow.premium

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.os.Build
import android.widget.Toast
import coil.compose.AsyncImage
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.ui.VisitasViewModel
import androidx.compose.runtime.collectAsState
import com.monst.transfiranow.notifications.AppNotifications

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumHomeScreen(
    viewModel: VisitasViewModel,
    onCreateNew: () -> Unit,
    onOpen: (VisitingCard) -> Unit,
    onClose: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Cards", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNew,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) { Icon(Icons.Rounded.Add, contentDescription = "Create") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
            .padding(padding),
            contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.cards.isNotEmpty()) {
                item {
                    val latest = uiState.cards.first()
                    ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Modo Evento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    latest.name.ifBlank { "Cartão mais recente" },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    if (uiState.eventModeEnabled) {
                                        AppNotifications.stopEventMode(context)
                                        Toast.makeText(context, "Modo Evento encerrado.", Toast.LENGTH_SHORT).show()
                                        return@FilledTonalButton
                                    }

                                    if (!AppNotifications.canPostNotifications(context)) {
                                        Toast.makeText(context, "Ative as notificações para funcionar.", Toast.LENGTH_LONG).show()
                                        AppNotifications.openAppNotificationSettings(context)
                                        return@FilledTonalButton
                                    }

                                    AppNotifications.ensureChannels(context)
                                    if (!AppNotifications.isEventChannelEnabled(context)) {
                                        Toast.makeText(context, "Ative o canal “Modo Evento” para aparecer na Now Bar.", Toast.LENGTH_LONG).show()
                                        AppNotifications.openEventChannelSettings(context)
                                        return@FilledTonalButton
                                    }

                                    AppNotifications.startEventMode(
                                        context,
                                        title = "Modo Evento Ativo",
                                        text = latest.name.ifBlank { "Seu cartão está pronto para compartilhar" },
                                        cardId = latest.id
                                    )

                                    if (Build.VERSION.SDK_INT >= 36 && !AppNotifications.canPostPromotedNotifications(context)) {
                                        Toast.makeText(context, "Permita Live Updates para aparecer na Now Bar.", Toast.LENGTH_LONG).show()
                                        AppNotifications.openAppNotificationPromotionSettings(context)
                                    } else {
                                        Toast.makeText(context, "Modo Evento ativado.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text(if (uiState.eventModeEnabled) "Encerrar" else "Ativar")
                            }
                        }
                    }
                }
            }
            items(uiState.cards, key = { it.id }) { card ->
                PremiumCardRow(card = card, onClick = { onOpen(card) })
            }
            if (uiState.cards.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No cards yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("Tap + to create your first card.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumCardRow(card: VisitingCard, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                AsyncImage(
                    model = card.photoUri.ifBlank { null },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Text(
                        "Card",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        card.name.ifBlank { "Sem nome" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        card.role.ifBlank { "—" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val qr = rememberQrBitmap(card.qrValue, size = 220)
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    if (qr != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qr,
                            contentDescription = null,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    } else {
                        Text("QR", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
