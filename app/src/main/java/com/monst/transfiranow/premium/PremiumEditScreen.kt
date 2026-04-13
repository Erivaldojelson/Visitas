package com.monst.transfiranow.premium

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.monst.transfiranow.ui.VisitasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumEditScreen(
    viewModel: VisitasViewModel,
    onBack: () -> Unit,
    onPickPhoto: () -> Unit,
    onPickQrCode: () -> Unit,
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val draft = uiState.draft

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Create / Edit", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp, 10.dp, 18.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp), modifier = Modifier.animateContentSize()) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            AsyncImage(
                                model = draft.photoUri.ifBlank { null },
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(draft.name.ifBlank { "Seu nome" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(draft.role.ifBlank { "Cargo / descrição" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = draft.name,
                            onValueChange = { v -> viewModel.updateDraft { it.copy(name = v) } },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.role,
                            onValueChange = { v -> viewModel.updateDraft { it.copy(role = v) } },
                            label = { Text("Role") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.instagram,
                            onValueChange = { v -> viewModel.updateDraft { it.copy(instagram = v) } },
                            label = { Text("Instagram (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.website,
                            onValueChange = { v -> viewModel.updateDraft { it.copy(website = v) } },
                            label = { Text("Website (optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(onClick = onPickPhoto, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.Image, null)
                                Spacer(Modifier.size(8.dp))
                                Text("Google Photos")
                            }
                            FilledTonalButton(onClick = onPickQrCode, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.QrCode2, null)
                                Spacer(Modifier.size(8.dp))
                                Text("QR Image")
                            }
                        }

                        AnimatedVisibility(visible = draft.qrValue.isNotBlank()) {
                            val qr = rememberQrBitmap(draft.qrValue, size = 760)
                            if (qr != null) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        androidx.compose.foundation.Image(
                                            bitmap = qr,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(220.dp)
                                                .clip(RoundedCornerShape(22.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .padding(12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.saveDraft()
                                onDone()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Save, null)
                            Spacer(Modifier.size(8.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
