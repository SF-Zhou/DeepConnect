package com.deepconnect.vpn.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.deepconnect.vpn.BuildConfig
import com.deepconnect.vpn.data.AppSettings
import com.deepconnect.vpn.ui.AppViewModel
import com.deepconnect.vpn.ui.components.AuroraSegmentedControl
import com.deepconnect.vpn.ui.components.AuroraTopBar
import com.deepconnect.vpn.ui.components.BrandMark
import com.deepconnect.vpn.ui.components.ChoicePillRow
import com.deepconnect.vpn.ui.components.GlassDivider
import com.deepconnect.vpn.ui.components.MetaChip
import com.deepconnect.vpn.ui.components.SectionCard
import com.deepconnect.vpn.ui.components.SettingNavRow
import com.deepconnect.vpn.ui.components.SettingSwitchRow
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.theme.LocalAuroraColors
import com.deepconnect.vpn.vpn.OpenConnectNative

private const val REPO_URL = "https://github.com/SF-Zhou/DeepConnect"

/**
 * App preferences grouped into Startup / Appearance / Logging / About glass cards.
 */
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onOpenLogs: () -> Unit,
    onOpenProfiles: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current

    val logLevel by viewModel.logLevel.collectAsState()
    val showTimestamps by viewModel.showTimestampsInLog.collectAsState()
    val autoConnectOnLaunch by viewModel.autoConnectOnLaunch.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val dynamicColor by viewModel.dynamicColor.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val defaultProfileId by viewModel.defaultProfileId.collectAsState()
    val defaultProfileName = profiles.firstOrNull { it.id == defaultProfileId }?.displayName

    val nativeVersion = remember { OpenConnectNative.version }
    val capabilities = remember { OpenConnectNative.capabilities() }
    val snackbarHostState = remember { SnackbarHostState() }
    val dynamicColorAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuroraTopBar(
                title = "Settings",
                subtitle = "Aurora Security · v${BuildConfig.VERSION_NAME}",
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "startup") {
                SectionCard(title = "Startup", icon = AppIcons.Bolt) {
                    SettingSwitchRow(
                        icon = AppIcons.Power,
                        title = "Auto-connect on launch",
                        subtitle = "Open the tunnel to the default profile when DeepConnect starts.",
                        checked = autoConnectOnLaunch,
                        onCheckedChange = { viewModel.setAutoConnectOnLaunch(it) },
                    )
                    GlassDivider()
                    SettingNavRow(
                        icon = AppIcons.Star,
                        title = "Default profile",
                        subtitle = if (defaultProfileName != null) {
                            "Used for auto-connect and the Connect button"
                        } else {
                            "None yet — add a profile to get started"
                        },
                        value = defaultProfileName ?: "None",
                        onClick = onOpenProfiles,
                    )
                }
            }

            item(key = "appearance") {
                SectionCard(title = "Appearance", icon = AppIcons.Contrast) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Choose how DeepConnect looks on this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        AuroraSegmentedControl(
                            options = listOf(
                                AppSettings.THEME_SYSTEM,
                                AppSettings.THEME_LIGHT,
                                AppSettings.THEME_DARK,
                            ),
                            selected = themeMode,
                            onSelect = { viewModel.setThemeMode(it) },
                            label = { mode ->
                                when (mode) {
                                    AppSettings.THEME_LIGHT -> "Light"
                                    AppSettings.THEME_DARK -> "Dark"
                                    else -> "System"
                                }
                            },
                        )
                        Spacer(Modifier.height(14.dp))
                        PalettePreview()
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassDivider()
                    SettingSwitchRow(
                        icon = AppIcons.Contrast,
                        title = "Dynamic colour",
                        subtitle = if (dynamicColorAvailable) {
                            "Match the Material You palette from your wallpaper."
                        } else {
                            "Requires Android 12 or newer."
                        },
                        checked = dynamicColor && dynamicColorAvailable,
                        enabled = dynamicColorAvailable,
                        onCheckedChange = { viewModel.setDynamicColor(it) },
                    )
                }
            }

            item(key = "logging") {
                SectionCard(title = "Logging", icon = AppIcons.Terminal) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Log level",
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.onSurface,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = LogLevelOptions.first { it.level == logLevel }.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        ChoicePillRow(
                            options = LogLevelOptions.map { it.level },
                            selected = logLevel,
                            onSelect = { viewModel.setLogLevel(it) },
                            label = { level ->
                                LogLevelOptions.first { it.level == level }.label
                            },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassDivider()
                    SettingSwitchRow(
                        icon = AppIcons.Clock,
                        title = "Show log timestamps",
                        subtitle = "Prefix every log line with its local time.",
                        checked = showTimestamps,
                        onCheckedChange = { viewModel.setShowTimestampsInLog(it) },
                    )
                    GlassDivider()
                    SettingNavRow(
                        icon = AppIcons.Terminal,
                        title = "Open connection log",
                        subtitle = "Inspect the OpenConnect handshake output.",
                        onClick = onOpenLogs,
                    )
                }
            }

            item(key = "about") {
                SectionCard(title = "About", icon = AppIcons.Info) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BrandMark(size = 46.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DeepConnect",
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Version ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        MetaChip(text = "OPEN SOURCE")
                    }
                    Spacer(Modifier.height(14.dp))
                    GlassDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "OPENCONNECT LIBRARY",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = nativeVersion,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = scheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "CAPABILITIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    CapabilityChips(capabilities = capabilities)
                    Spacer(Modifier.height(14.dp))
                    GlassDivider()
                    SettingNavRow(
                        icon = AppIcons.ExternalLink,
                        title = "GitHub repository",
                        subtitle = "SF-Zhou/DeepConnect",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(REPO_URL))
                            runCatching { context.startActivity(intent) }
                        },
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Based on the OpenConnect project (LGPL). DeepConnect is an " +
                            "independent client and is not affiliated with Cisco, Juniper, " +
                            "Pulse Secure, Palo Alto Networks or Fortinet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
                    )
                }
            }

            item(key = "footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Aurora Security · built with Jetpack Compose",
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* pieces                                                                    */
/* -------------------------------------------------------------------------- */

@Composable
private fun PalettePreview() {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.glassStrongBottom)
            .border(1.dp, tokens.glassBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Palette",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.surfaceVariant).forEach { color ->
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.4f), CircleShape),
            )
        }
    }
}

@Composable
private fun CapabilityChips(capabilities: OpenConnectNative.Capabilities?) {
    val scheme = MaterialTheme.colorScheme
    if (capabilities == null) {
        Text(
            text = "The native OpenConnect library could not be loaded.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.error,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CapabilityChip(label = "OATH / TOTP", supported = capabilities.oath)
            CapabilityChip(label = "SToken", supported = capabilities.stoken)
            CapabilityChip(label = "PKCS#11", supported = capabilities.pkcs11)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CapabilityChip(label = "TSS", supported = capabilities.tss)
            CapabilityChip(label = "YubiKey OATH", supported = capabilities.yubiOath)
        }
    }
}

@Composable
private fun CapabilityChip(label: String, supported: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val accent = if (supported) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.7f)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (supported) scheme.primary.copy(alpha = 0.14f) else tokens.glassStrongTop)
            .border(
                1.dp,
                if (supported) scheme.primary.copy(alpha = 0.28f) else tokens.glassBorder,
                CircleShape,
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(13.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = if (supported) 0.22f else 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (supported) AppIcons.Check else AppIcons.Close,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(9.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (supported) scheme.primary else scheme.onSurfaceVariant,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* log levels                                                                */
/* -------------------------------------------------------------------------- */

private data class LogLevelOption(
    val level: Int,
    val label: String,
    val description: String,
)

private val LogLevelOptions = listOf(
    LogLevelOption(0, "Errors", "Only failures and critical messages are captured."),
    LogLevelOption(1, "Info", "Connection progress and important events. Recommended."),
    LogLevelOption(2, "Debug", "Verbose handshake diagnostics."),
    LogLevelOption(3, "Trace", "Everything reported by the native library."),
)
