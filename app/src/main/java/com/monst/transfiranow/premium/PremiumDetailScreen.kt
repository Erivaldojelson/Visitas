package com.monst.transfiranow.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.ui.VisitasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumDetailScreen(
    viewModel: VisitasViewModel,
    cardId: String,
    onBack: () -> Unit,
    onEdit: (VisitingCard) -> Unit,
    onSaveToWallet: (VisitingCard) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val card = uiState.cards.firstOrNull { it.id == cardId }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Detail", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (card == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Card not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            AsyncImage(
                                model = card.photoUri.ifBlank { null },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(card.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                            Text(card.role.ifBlank { "—" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                val qr = rememberQrBitmap(card.qrValue, size = 760)
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("QR Code", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Surface(
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            Box(
                                Modifier
                                    .padding(14.dp)
                                    .size(260.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                if (qr != null) {
                                    androidx.compose.foundation.Image(
                                        bitmap = qr,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = card.note.isNotBlank()) {
                            Text(card.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { onEdit(card) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Edit, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Edit")
                    }
                    FilledTonalButton(onClick = { onSaveToWallet(card) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Save, null)
                        Spacer(Modifier.size(8.dp))
                        Text("Wallet")
                    }
                }
            }

            item {
                ActionLinks(card)
            }
        }
    }
}

@Composable
private fun ActionLinks(card: VisitingCard) {
    val actions = listOfNotNull(
        card.instagram.takeIf { it.isNotBlank() }?.let { "Instagram" to it },
        card.phone.takeIf { it.isNotBlank() }?.let { "WhatsApp" to it },
        card.website.takeIf { it.isNotBlank() }?.let { "Website" to it }
    )

    if (actions.isEmpty()) return

    ElevatedCard(shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Links", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            actions.forEach { (label, value) ->
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, fontWeight = FontWeight.Medium)
                        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

