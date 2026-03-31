package com.monst.transfiranow.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import com.monst.transfiranow.R
import com.monst.transfiranow.data.AccentOption
import com.monst.transfiranow.data.TransferEntry
import com.monst.transfiranow.data.TransferOrigin
import com.monst.transfiranow.data.TransferRepository
import com.monst.transfiranow.data.TransferState
import com.monst.transfiranow.data.formatBytes
import com.monst.transfiranow.data.formatSpeed
import com.monst.transfiranow.data.isIndeterminate
import com.monst.transfiranow.data.progressPercent
import com.monst.transfiranow.ui.theme.TransfiraNowTheme

@Composable
fun TransfiraNowApp(
    onEnsureMonitor: () -> Unit,
    onStartDownload: (String) -> Unit
) {
    val context = LocalContext.current
    val uiState by TransferRepository.uiState.collectAsState()

    TransfiraNowTheme(
        dynamicColor = uiState.dynamicColorEnabled,
        accentColor = uiState.accentColor
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
                    HeaderCard()
                }
                item {
                    OverviewCard(
                        totalSpeed = uiState.totalSpeedBytesPerSecond,
                        accentColor = uiState.accentColor
                    )
                }
                item {
                    MonitorCard(
                        notificationsGranted = uiState.notificationsAccessGranted,
                        onOpenNotificationAccess = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onEnsureMonitor = onEnsureMonitor
                    )
                }
                item {
                    DownloadLauncherCard(
                        url = uiState.downloadUrl,
                        helperMessage = uiState.helperMessage,
                        onUrlChange = TransferRepository::setDownloadUrl,
                        onStartDownload = onStartDownload
                    )
                }
                item {
                    SettingsCard(
                        selectedColor = uiState.accentColor,
                        dynamicColorEnabled = uiState.dynamicColorEnabled,
                        onSelectAccent = TransferRepository::updateAccentColor,
                        onDynamicColorChange = TransferRepository::setDynamicColorEnabled
                    )
                }
                item {
                    Text(
                        text = "Downloads do app",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (uiState.appManagedEntries.isEmpty()) {
                    item { EmptyCard() }
                } else {
                    items(uiState.appManagedEntries, key = { it.id }) { entry ->
                        TransferRow(entry)
                    }
                }
                item {
                    Text(
                        text = "Detectados de terceiros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (uiState.externalEntries.isEmpty()) {
                    item { ExternalFallbackCard() }
                } else {
                    items(uiState.externalEntries, key = { it.id }) { entry ->
                        TransferRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadLauncherCard(
    url: String,
    helperMessage: String,
    onUrlChange: (String) -> Unit,
    onStartDownload: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Public, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text("Baixar pelo Transfira-now", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Cole um link direto. Esse fluxo é o mais elegível para Android 16 + ProgressStyle e tem mais chance de entrar na Now Bar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                label = { Text("URL do arquivo") },
                placeholder = { Text("https://exemplo.com/app.apk") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = helperMessage,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.width(12.dp))
                Button(
                    onClick = { onStartDownload(url.trim()) },
                    enabled = url.trim().startsWith("http")
                ) {
                    Text("Baixar")
                }
            }
        }
    }
}

@Composable
private fun HeaderCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_foreground),
                contentDescription = "Logo do Transfira Now",
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Column {
                Text(
                    text = "Transfira-now",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Monitore downloads e leve o progresso para a experiência da Now Bar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun OverviewCard(totalSpeed: Long, accentColor: Color) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Velocidade total", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = totalSpeed.formatSpeed(),
                style = MaterialTheme.typography.displaySmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricColumn("Download", totalSpeed.formatSpeed())
                MetricColumn("Upload", "0 B/s")
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MonitorCard(
    notificationsGranted: Boolean,
    onOpenNotificationAccess: () -> Unit,
    onEnsureMonitor: () -> Unit
) {
    val status = if (notificationsGranted) "Monitor ativo" else "Aguardando permissão"
    val subtitle = if (notificationsGranted) {
        "O app já pode espelhar notificações de download"
    } else {
        "Ative o acesso às notificações para detectar downloads de outros apps"
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.NotificationsActive, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text(status, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onEnsureMonitor) {
                    Text("Iniciar")
                }
                TextButton(onClick = onOpenNotificationAccess) {
                    Text("Permissões")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsCard(
    selectedColor: Color,
    dynamicColorEnabled: Boolean,
    onSelectAccent: (Color) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val options = listOf(
        AccentOption("Lilás", Color(0xFF5D56C4)),
        AccentOption("Azul", Color(0xFF1565C0)),
        AccentOption("Verde", Color(0xFF2E7D32)),
        AccentOption("Coral", Color(0xFFE65159)),
        AccentOption("Âmbar", Color(0xFFFF8F00))
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Palette, contentDescription = null)
                Spacer(Modifier.size(10.dp))
                Column {
                    Text("Cor da Now Bar", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "A cor escolhida alimenta o tema do app e a notificação contínua. Na Now Bar, a cor final pode ser ajustada pelo sistema.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { option ->
                    FilterChip(
                        selected = option.color == selectedColor,
                        onClick = { onSelectAccent(option.color) },
                        label = { Text(option.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(option.color)
                            )
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Material You dinâmico", style = MaterialTheme.typography.titleSmall)
                    Text("Usa as cores do sistema no restante da interface", style = MaterialTheme.typography.bodyMedium)
                }
                Switch(
                    checked = dynamicColorEnabled,
                    onCheckedChange = onDynamicColorChange
                )
            }
        }
    }
}

@Composable
private fun EmptyCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Nenhuma transferência detectada", style = MaterialTheme.typography.titleMedium)
            Text(
                "Assim que você iniciar um download pelo próprio app, ele aparece aqui com acompanhamento nativo via DownloadManager.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ExternalFallbackCard() {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Modo fallback para outros apps", style = MaterialTheme.typography.titleMedium)
            Text(
                "Downloads iniciados fora do Transfira-now continuam aparecendo aqui quando houver notificação de progresso. Para esses casos, a entrega confiável é lock screen + notificação contínua.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TransferRow(entry: TransferEntry) {
    Card(
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ExpressiveProgressIcon(entry)
                    Spacer(Modifier.size(14.dp))
                    Column {
                        Text(entry.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (entry.origin == TransferOrigin.AppManaged) {
                                "Download iniciado pelo app"
                            } else {
                                entry.sourceApp
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = when (entry.state) {
                        TransferState.Active -> entry.progress?.let { "$it%" } ?: "Ativo"
                        TransferState.Waiting -> "Preparando"
                        TransferState.Completed -> "Concluído"
                        TransferState.Failed -> "Falhou"
                    },
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (entry.state == TransferState.Active || entry.state == TransferState.Waiting) {
                LinearProgressIndicator(
                    progress = { entry.progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                )
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${entry.downloadedBytes?.formatBytes() ?: "0 B"} / ${entry.totalBytes?.formatBytes() ?: "?"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    entry.detail?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = entry.speedBytesPerSecond?.formatSpeed() ?: if (entry.isIndeterminate) "Aguardando progresso" else "${entry.progressPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ExpressiveProgressIcon(entry: TransferEntry) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    Box(
        modifier = Modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { entry.progressPercent / 100f },
            modifier = Modifier.size(52.dp),
            strokeWidth = 5.dp,
            color = primary,
            trackColor = primary.copy(alpha = 0.16f)
        )
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(container)
                .border(1.dp, primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (entry.state == TransferState.Completed) Icons.Rounded.CheckCircle else Icons.Rounded.Download,
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(20.dp)
            )
        }
        if (!entry.isIndeterminate && entry.state == TransferState.Active) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(primary)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "${entry.progressPercent}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
