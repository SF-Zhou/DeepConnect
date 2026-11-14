@file:OptIn(ExperimentalMaterial3Api::class)

package com.deepconnect.vpn.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepconnect.vpn.data.Profile
import com.deepconnect.vpn.vpn.TrafficStats
import com.deepconnect.vpn.vpn.DtlsState
import com.deepconnect.vpn.vpn.VpnStatus
import kotlinx.coroutines.delay

/**
 * Home screen: connection state hero card plus the list of saved profiles.
 *
 * All taps funnel through the ViewModel so the VPN consent handshake stays in one place.
 */
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val defaultProfileId by viewModel.defaultProfileId.collectAsState()
    val status by viewModel.status.collectAsState()
    val message by viewModel.message.collectAsState()

    var profileToDelete by remember { mutableStateOf<Profile?>(null) }
    var profileToDisconnect by remember { mutableStateOf<Profile?>(null) }

    val connectedStatus = status as? VpnStatus.Connected
    val connectedProfileId = connectedStatus?.profileId

    // A local clock that only ticks while a session is up, used for the session timer.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val connectedSince = connectedStatus?.since
    LaunchedEffect(connectedSince) {
        if (connectedSince != null) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }
    val elapsedMs = connectedSince?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    fun connect(profile: Profile) {
        when (status) {
            is VpnStatus.Disconnected, is VpnStatus.Failed -> viewModel.startConnect(context, profile)
            is VpnStatus.Connected -> {
                if (profile.id != connectedProfileId) {
                    profileToDisconnect = profile
                }
            }
            is VpnStatus.Connecting, is VpnStatus.Disconnecting -> Unit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = "DeepConnect", fontWeight = FontWeight.SemiBold)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenLogs) {
                        Icon(Icons.Filled.Terminal, contentDescription = "Connection log")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProfile) {
                Icon(Icons.Filled.Add, contentDescription = "Add profile")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                StatusHeroCard(
                    status = status,
                    elapsedMs = elapsedMs,
                    onDisconnect = { viewModel.disconnect(context) },
                    onRetry = { profileId ->
                        profiles.firstOrNull { it.id == profileId }?.let { failedProfile ->
                            viewModel.startConnect(context, failedProfile)
                        }
                    },
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${profiles.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (profiles.isEmpty()) {
                item { EmptyProfiles(onAddProfile = onAddProfile) }
            } else {
                items(items = profiles, key = { profile -> profile.id }) { profile ->
                    ProfileCard(
                        profile = profile,
                        status = status,
                        isDefault = profile.id == defaultProfileId,
                        onClick = { connect(profile) },
                        onEdit = { onEditProfile(profile.id) },
                        onDelete = { profileToDelete = profile },
                        onSetDefault = { viewModel.setDefaultProfile(profile.id) },
                    )
                }
            }
        }
    }

    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Delete profile?") },
            text = {
                Text(
                    "\"${profile.displayName}\" will be removed from this device. " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(profile.id)
                        profileToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    profileToDisconnect?.let { target ->
        AlertDialog(
            onDismissRequest = { profileToDisconnect = null },
            title = { Text("Disconnect current session?") },
            text = {
                Text(
                    "You are connected to ${connectedStatus?.profileName ?: "the current VPN"}. " +
                        "Disconnect it before connecting to ${target.displayName}.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.disconnect(context)
                        profileToDisconnect = null
                    },
                ) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDisconnect = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/* ---------------------------------------------------------------------- */
/* status hero                                                            */
/* ---------------------------------------------------------------------- */

@Composable
private fun StatusHeroCard(
    status: VpnStatus,
    elapsedMs: Long,
    onDisconnect: () -> Unit,
    onRetry: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = statusColor(status)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(status = status, accent = accent)
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statusTitle(status),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    statusCaption(status)?.let { caption ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            when (status) {
                is VpnStatus.Disconnected -> {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Select a profile below to establish an encrypted tunnel. " +
                            "Your traffic stays private on untrusted networks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is VpnStatus.Connecting -> {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Stage: ${status.stage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is VpnStatus.Disconnecting -> {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Tearing down the tunnel…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is VpnStatus.Connected -> {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "CONNECTED FOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(elapsedMs),
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                    )
                    Spacer(Modifier.height(14.dp))
                    TrafficRow(stats = status.stats)
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    )
                    Spacer(Modifier.height(12.dp))
                    DetailLine("Server", status.server)
                    DetailLine(
                        label = "Local address",
                        value = listOfNotNull(status.ipv4, status.ipv6)
                            .joinToString(" · ")
                            .ifBlank { "—" },
                    )
                    DetailLine("MTU", if (status.mtu > 0) status.mtu.toString() else "—")
                    DetailLine(
                        label = "DNS",
                        value = if (status.dns.isEmpty()) "—" else status.dns.joinToString(", "),
                    )
                    DetailLine("Protocol", status.protocol.ifBlank { "—" })
                    DetailLine("TLS cipher", status.cstpCipher?.takeIf { it.isNotBlank() } ?: "—")
                    DetailLine("DTLS (UDP)", dtlsSummary(status))
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Disconnect")
                    }
                }

                is VpnStatus.Failed -> {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ) {
                        Text(
                            text = status.message,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    FilledTonalButton(
                        onClick = { onRetry(status.profileId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Try again")
                    }
                }
            }
        }
    }
}

/** Human readable summary of the optional DTLS side channel. */
private fun dtlsSummary(status: VpnStatus.Connected): String = when (status.dtlsState) {
    DtlsState.ACTIVE -> status.dtlsCipher?.takeIf { it.isNotBlank() } ?: "active"
    DtlsState.NEGOTIATING -> "negotiating…"
    DtlsState.UNAVAILABLE -> "unavailable (TLS only)"
    DtlsState.DISABLED -> "disabled"
}

@Composable
private fun StatusBadge(status: VpnStatus, accent: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            is VpnStatus.Connecting, is VpnStatus.Disconnecting -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = accent,
                    strokeWidth = 2.5.dp,
                )
            }

            is VpnStatus.Failed -> Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp),
            )

            is VpnStatus.Connected -> Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp),
            )

            is VpnStatus.Disconnected -> Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

@Composable
private fun TrafficRow(stats: TrafficStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TrafficStat(
            icon = Icons.Filled.ArrowDownward,
            label = "Download",
            value = formatBytes(stats.rxBytes),
            modifier = Modifier.weight(1f),
        )
        TrafficStat(
            icon = Icons.Filled.ArrowUpward,
            label = "Upload",
            value = formatBytes(stats.txBytes),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TrafficStat(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(112.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun statusTitle(status: VpnStatus): String = when (status) {
    is VpnStatus.Disconnected -> "Not connected"
    is VpnStatus.Connecting -> "Connecting…"
    is VpnStatus.Connected -> status.profileName
    is VpnStatus.Disconnecting -> "Disconnecting…"
    is VpnStatus.Failed -> "Connection failed"
}

private fun statusCaption(status: VpnStatus): String? = when (status) {
    is VpnStatus.Disconnected -> "Your connection is not protected"
    is VpnStatus.Connecting -> status.profileName
    is VpnStatus.Connected -> status.server
    is VpnStatus.Disconnecting -> status.profileName
    is VpnStatus.Failed -> status.profileName
}

/* ---------------------------------------------------------------------- */
/* profile list                                                           */
/* ---------------------------------------------------------------------- */

@Composable
private fun ProfileCard(
    profile: Profile,
    status: VpnStatus,
    isDefault: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit,
) {
    val connected = status as? VpnStatus.Connected
    val isConnected = connected?.profileId == profile.id
    val isTransitioning = status is VpnStatus.Connecting || status is VpnStatus.Disconnecting
    val connectedAccent = statusColors().connected
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            },
        ),
        border = if (isConnected) BorderStroke(1.dp, connectedAccent.copy(alpha = 0.5f)) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isTransitioning, onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(name = profile.displayName)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isDefault) {
                        Spacer(Modifier.width(8.dp))
                        MetaChip(text = "Default", emphasized = true)
                    }
                    if (isConnected) {
                        Spacer(Modifier.width(8.dp))
                        MetaChip(text = "Connected", emphasized = true)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = profile.serverHost.ifBlank { "No server configured" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (profile.username.isNotBlank()) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaChip(text = protocolChipLabel(profile.protocol))
                    if (profile.totpEnabled) {
                        MetaChip(text = "TOTP")
                    }
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Profile actions")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (!isDefault) {
                        DropdownMenuItem(
                            text = { Text("Set as default") },
                            onClick = {
                                menuExpanded = false
                                onSetDefault()
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.StarOutline, contentDescription = null)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Edit, contentDescription = null)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, contentDescription = null)
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(name: String) {
    val initials = profileInitials(name)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun MetaChip(text: String, emphasized: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (emphasized) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        },
        contentColor = if (emphasized) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyProfiles(onAddProfile: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No VPN profiles yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add your first OpenConnect profile to start a secure session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAddProfile) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Add profile")
            }
        }
    }
}

private fun protocolChipLabel(protocolId: String): String = when (protocolId) {
    "anyconnect" -> "AnyConnect"
    "nc" -> "Juniper"
    "pulse" -> "Pulse"
    "gp" -> "GlobalProtect"
    "fortinet" -> "Fortinet"
    "f5" -> "F5"
    else -> protocolId
}
