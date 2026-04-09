package com.monst.transfiranow.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.biometric.BiometricManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.monst.transfiranow.BuildConfig
import com.monst.transfiranow.data.AppLanguage
import com.monst.transfiranow.data.VisitingCard
import com.monst.transfiranow.notifications.AppNotifications
import com.monst.transfiranow.share.CardsBackup
import com.monst.transfiranow.premium.PremiumCardsActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsPage { HOME, LANGUAGE, SECURITY, NOTIFICATIONS, NOW_BAR, BACKUP, ABOUT }

private fun adaptiveSidePadding(maxWidth: Dp, maxContentWidth: Dp, minPadding: Dp = 16.dp): Dp {
    if (maxWidth <= maxContentWidth) return minPadding
    val extra = (maxWidth - maxContentWidth) / 2f
    return if (extra > minPadding) extra else minPadding
}

@Composable
fun OrganizedSettingsScreen(
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
    var page by rememberSaveable { mutableStateOf(SettingsPage.HOME) }

    BackHandler(enabled = page != SettingsPage.HOME) {
        page = SettingsPage.HOME
    }

    when (page) {
        SettingsPage.HOME -> BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            t("settings_head"),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            t("settings_sub"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    SettingsNavRow(
                        icon = Icons.Rounded.Style,
                        title = t("premium_ui_open"),
                        subtitle = t("premium_ui_hint"),
                        trailing = Icons.Rounded.OpenInNew
                    ) {
                        context.startActivity(Intent(context, PremiumCardsActivity::class.java))
                    }
                }

                item {
                    SettingsNavRow(
                        icon = Icons.Rounded.Notifications,
                        title = t("settings_nav_notifications"),
                        subtitle = if (notificationsEnabled) t("settings_state_on") else t("settings_state_off"),
                    ) {
                        page = SettingsPage.NOTIFICATIONS
                    }
                }

                item {
                    SettingsNavRow(
                        icon = Icons.Rounded.Settings,
                        title = t("settings_nav_now_bar"),
                        subtitle = if (liveUpdatesEnabled) t("settings_now_bar_on") else t("settings_now_bar_off"),
                        trailing = Icons.Rounded.OpenInNew
                    ) {
                        page = SettingsPage.NOW_BAR
                    }
                }

                item {
                    val languageLabel = when (currentLanguage) {
                        AppLanguage.PT_BR -> "Português (Brasil)"
                        AppLanguage.PT_PT -> "Português (Portugal)"
                        AppLanguage.PT_AO -> "Português (Angola)"
                        AppLanguage.EN -> "English"
                        AppLanguage.ZH -> "中文"
                    }
                    SettingsNavRow(
                        icon = Icons.Rounded.Language,
                        title = t("language"),
                        subtitle = languageLabel,
                    ) {
                        page = SettingsPage.LANGUAGE
                    }
                }

                item {
                    SettingsNavRow(
                        icon = Icons.Rounded.Person,
                        title = t("settings_nav_security"),
                        subtitle = if (appLockEnabled) t("settings_state_on") else t("settings_state_off"),
                    ) {
                        page = SettingsPage.SECURITY
                    }
                }

                item {
                    SettingsNavRow(
                        icon = Icons.Rounded.Share,
                        title = t("settings_nav_backup"),
                        subtitle = "${cards.size} ${t("saved_count")}",
                    ) {
                        page = SettingsPage.BACKUP
                    }
                }

                item {
                    SettingsNavRow(
                        icon = Icons.Rounded.Contacts,
                        title = t("settings_nav_about"),
                        subtitle = "Visitas ${BuildConfig.VERSION_NAME}",
                    ) {
                        page = SettingsPage.ABOUT
                    }
                }
            }
        }

        SettingsPage.LANGUAGE -> BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SettingsTopBar(title = t("language")) { page = SettingsPage.HOME } }
                item {
                    ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(
                                AppLanguage.PT_BR to "Português (Brasil)",
                                AppLanguage.PT_PT to "Português (Portugal)",
                                AppLanguage.PT_AO to "Português (Angola)",
                                AppLanguage.EN to "English",
                                AppLanguage.ZH to "中文"
                            ).forEach { (lang, label) ->
                                FilterChip(
                                    selected = currentLanguage == lang,
                                    onClick = { onLanguageSelected(lang) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingsPage.SECURITY -> BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SettingsTopBar(title = t("settings_nav_security")) { page = SettingsPage.HOME } }
                item {
                    val authenticators =
                        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                    val canUseBiometric = remember {
                        BiometricManager.from(context).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
                    }

                    ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(t("settings_security_lock_title"), style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        t("settings_security_lock_body"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (!canUseBiometric) {
                                        Text(
                                            t("settings_security_unavailable"),
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
                                            Toast.makeText(context, t("settings_security_unavailable"), Toast.LENGTH_LONG).show()
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
            }
        }

        SettingsPage.BACKUP -> BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                onImportBackup(uri)
            }

            val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SettingsTopBar(title = t("settings_nav_backup")) { page = SettingsPage.HOME } }
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
                                                Toast.makeText(
                                                    context,
                                                    error.message ?: "Falha ao exportar backup.",
                                                    Toast.LENGTH_LONG
                                                ).show()
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
            }
        }

        SettingsPage.ABOUT -> BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SettingsTopBar(title = t("settings_nav_about")) { page = SettingsPage.HOME } }

                item {
                    ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(t("contacts"), style = MaterialTheme.typography.titleLarge)

                            SettingsNavRow(
                                icon = Icons.Rounded.Email,
                                title = "Email",
                                subtitle = "Erivaldojeson8@gmail.com",
                                trailing = Icons.Rounded.OpenInNew
                            ) {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:Erivaldojeson8@gmail.com")
                                }
                                context.startActivity(intent)
                            }

                            SettingsNavRow(
                                icon = Icons.Rounded.Link,
                                title = "GitHub",
                                subtitle = "github.com/Erivaldojelson",
                                trailing = Icons.Rounded.OpenInNew
                            ) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Erivaldojelson"))
                                context.startActivity(intent)
                            }

                            SettingsNavRow(
                                icon = Icons.Rounded.Phone,
                                title = "Telefone",
                                subtitle = "31983472309",
                                trailing = Icons.Rounded.OpenInNew
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

        SettingsPage.NOTIFICATIONS -> {
            val lifecycleOwner = LocalLifecycleOwner.current

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

            var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission()) }
            var systemNotificationsEnabled by remember { mutableStateOf(checkSystemNotificationsEnabled()) }

            fun refreshNotificationStatus() {
                hasNotificationPermission = checkNotificationPermission()
                systemNotificationsEnabled = checkSystemNotificationsEnabled()
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
                            Toast.makeText(context, t("onboarding_notifications_system_disabled"), Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, t("settings_notifications_enabled"), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        onNotificationsEnabledChange(false)
                        Toast.makeText(context, t("onboarding_notifications_denied"), Toast.LENGTH_LONG).show()
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

            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SettingsTopBar(title = t("settings_nav_notifications")) { page = SettingsPage.HOME } }
                    item {
                        ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(t("settings_notifications_toggle_title"), style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            t("settings_notifications_toggle_body"),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) {
                                            Text(
                                                t("settings_notifications_needs_permission"),
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else if (!systemNotificationsEnabled) {
                                            Text(
                                                t("onboarding_notifications_system_disabled"),
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
                                                        t("onboarding_notifications_system_disabled"),
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        }
                                    )
                                }

                                if ((Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission) || !systemNotificationsEnabled) {
                                    FilledTonalButton(onClick = { AppNotifications.openAppNotificationSettings(context) }) {
                                        Text(t("settings_open_notification_settings"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SettingsPage.NOW_BAR -> {
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

            LaunchedEffect(notificationsEnabled, liveUpdatesEnabled) {
                if (!notificationsEnabled && liveUpdatesEnabled) {
                    onLiveUpdatesEnabledChange(false)
                }
            }

            BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
                val side = adaptiveSidePadding(maxWidth, maxContentWidth = 720.dp)
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(side, 16.dp, side, 120.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SettingsTopBar(title = t("settings_nav_now_bar")) { page = SettingsPage.HOME } }

                    if (!notificationsEnabled) {
                        item {
                            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(t("settings_now_bar_needs_notifications"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    FilledTonalButton(onClick = { page = SettingsPage.NOTIFICATIONS }) {
                                        Text(t("settings_go_to_notifications"))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
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

                                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
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
                }
            }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Icon(icon, null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            val trailingIcon = trailing ?: Icons.AutoMirrored.Rounded.ArrowBack
            Icon(
                trailingIcon,
                null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (trailing == null) Modifier.rotate(180f) else Modifier
            )
        }
    }
}

@Composable
private fun SettingsTopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
            }
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
    }
}

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
