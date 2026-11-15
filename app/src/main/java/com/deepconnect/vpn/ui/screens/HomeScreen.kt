@file:OptIn(ExperimentalMaterial3Api::class)

package com.deepconnect.vpn.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepconnect.vpn.data.Profile
import com.deepconnect.vpn.ui.AppViewModel
import com.deepconnect.vpn.ui.ConnectionPhase
import com.deepconnect.vpn.ui.components.AnimatedStatText
import com.deepconnect.vpn.ui.components.AuroraButton
import com.deepconnect.vpn.ui.components.AuroraButtonStyle
import com.deepconnect.vpn.ui.components.AuroraConfirmDialog
import com.deepconnect.vpn.ui.components.BrandMark
import com.deepconnect.vpn.ui.components.ConnectionOrb
import com.deepconnect.vpn.ui.components.GlassCard
import com.deepconnect.vpn.ui.components.GlassDivider
import com.deepconnect.vpn.ui.components.GlassIconButton
import com.deepconnect.vpn.ui.components.LiveDot
import com.deepconnect.vpn.ui.components.MetaChip
import com.deepconnect.vpn.ui.components.ProfileAvatar
import com.deepconnect.vpn.ui.components.SectionLabel
import com.deepconnect.vpn.ui.dtlsSummary
import com.deepconnect.vpn.ui.eyebrow
import com.deepconnect.vpn.ui.formatBytes
import com.deepconnect.vpn.ui.formatClockTime
import com.deepconnect.vpn.ui.formatDuration
import com.deepconnect.vpn.ui.formatPacketCount
import com.deepconnect.vpn.ui.headline
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.localAddresses
import com.deepconnect.vpn.ui.phase
import com.deepconnect.vpn.ui.protocolLabel
import com.deepconnect.vpn.ui.subtitle
import com.deepconnect.vpn.ui.theme.LocalAuroraColors
import com.deepconnect.vpn.vpn.DtlsState
import com.deepconnect.vpn.vpn.VpnStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home screen: aurora backdrop, the connection orb hero, live traffic, session details and
 * the profile list. Every connection intent funnels through the ViewModel so the VPN
 * consent handshake stays in one place.
 */
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onAddProfile: () -> Unit,
    onEditProfile: (Long) -> Unit,
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tokens = LocalAuroraColors.current
    val scheme = MaterialTheme.colorScheme

    val profiles by viewModel.profiles.collectAsState()
    val defaultProfileId by viewModel.defaultProfileId.collectAsState()
    val status by viewModel.status.collectAsState()
    val message by viewModel.message.collectAsState()

    val phase = status.phase()
    val accent = tokens.status.of(status)
    val connected = status as? VpnStatus.Connected
    val failed = status as? VpnStatus.Failed
    val defaultProfile = profiles.firstOrNull { it.id == defaultProfileId } ?: profiles.firstOrNull()

    var profileToDelete by remember { mutableStateOf<Profile?>(null) }
    var profileToDisconnect by remember { mutableStateOf<Profile?>(null) }
    val listState = rememberLazyListState()

    // Session timer: only ticks while a tunnel is actually up.
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val connectedSince = connected?.since
    LaunchedEffect(connectedSince) {
        if (connectedSince != null) {
            while (true) {
                nowMs = System.currentTimeMillis()
                delay(1_000L)
            }
        }
    }
    val elapsedMs = connectedSince?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L

    // Tactile feedback on the transitions that matter, and bring the hero back into
    // view when the connection state changes (e.g. after tapping a card further down).
    LaunchedEffect(phase) {
        runCatching { listState.animateScrollToItem(0) }
        when (phase) {
            ConnectionPhase.Connected, ConnectionPhase.Disconnected ->
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)

            ConnectionPhase.Failed -> haptics.performHapticFeedback(HapticFeedbackType.Reject)
            else -> Unit
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.consumeMessage()
    }

    fun connect(profile: Profile) {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        when (status) {
            is VpnStatus.Disconnected, is VpnStatus.Failed ->
                viewModel.startConnect(context, profile)

            is VpnStatus.Connected -> {
                if (profile.id != connected?.profileId) profileToDisconnect = profile
            }

            is VpnStatus.Connecting, is VpnStatus.Disconnecting -> Unit
        }
    }

    fun disconnect() {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        viewModel.disconnect(context)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = tokens.glassStrongTop,
                    contentColor = scheme.onSurface,
                    actionColor = scheme.primary,
                    dismissActionContentColor = scheme.onSurfaceVariant,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HomeHeader(status = status)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "hero") {
                    ConnectionHero(
                        status = status,
                        elapsedMs = elapsedMs,
                        canConnect = defaultProfile != null,
                        hasProfiles = profiles.isNotEmpty(),
                        onConnect = { defaultProfile?.let(::connect) },
                        onDisconnect = ::disconnect,
                        onRetry = {
                            failed?.let { failure ->
                                profiles.firstOrNull { it.id == failure.profileId }
                                    ?.let(::connect)
                                    ?: defaultProfile?.let(::connect)
                            }
                        },
                        onAddProfile = onAddProfile,
                    )
                }

                if (connected != null) {
                    item(key = "traffic") {
                        TrafficRow(status = connected)
                    }
                }

                if (failed != null) {
                    item(key = "failure") {
                        FailureCard(message = failed.message)
                    }
                }

                if (connected != null) {
                    item(key = "details") {
                        DetailsCard(status = connected)
                    }
                }

                item(key = "profilesHeader") {
                    ProfilesHeader(count = profiles.size, onAddProfile = onAddProfile)
                }

                if (profiles.isEmpty()) {
                    item(key = "empty") {
                        EmptyProfiles(onAddProfile = onAddProfile)
                    }
                } else {
                    items(items = profiles, key = { profile -> profile.id }) { profile ->
                        SwipeableProfileCard(
                            modifier = Modifier.animateItem(),
                            profile = profile,
                            isDefault = profile.id == defaultProfileId,
                            isConnected = connected?.profileId == profile.id,
                            isBusy = phase == ConnectionPhase.Connecting ||
                                phase == ConnectionPhase.Disconnecting,
                            onConnect = { connect(profile) },
                            onEdit = { onEditProfile(profile.id) },
                            onDeleteRequest = { profileToDelete = profile },
                            onSetDefault = { viewModel.setDefaultProfile(profile.id) },
                        )
                    }
                }
            }
        }
    }

    profileToDelete?.let { profile ->
        AuroraConfirmDialog(
            title = "Delete profile?",
            message = "\"${profile.displayName}\" will be removed from this device. " +
                "This cannot be undone.",
            confirmLabel = "Delete profile",
            icon = AppIcons.Trash,
            destructive = true,
            onDismiss = { profileToDelete = null },
            onConfirm = {
                profileToDelete = null
                viewModel.deleteProfile(profile.id)
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "\"${profile.displayName}\" deleted",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.saveProfile(profile)
                    }
                }
            },
        )
    }

    profileToDisconnect?.let { target ->
        AuroraConfirmDialog(
            title = "Switch profile?",
            message = "You are connected to ${connected?.profileName ?: "the current VPN"}. " +
                "Disconnect it before connecting to ${target.displayName}.",
            confirmLabel = "Disconnect",
            icon = AppIcons.Power,
            onDismiss = { profileToDisconnect = null },
            onConfirm = {
                profileToDisconnect = null
                viewModel.disconnect(context)
            },
        )
    }
}

/* -------------------------------------------------------------------------- */
/* header                                                                    */
/* -------------------------------------------------------------------------- */

@Composable
private fun HomeHeader(status: VpnStatus) {
    val tokens = LocalAuroraColors.current
    val scheme = MaterialTheme.colorScheme
    val phase = status.phase()
    val accent = tokens.status.of(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DeepConnect",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = "AURORA SECURITY",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
        }
        MetaChip(
            text = when (phase) {
                ConnectionPhase.Connected -> "PROTECTED"
                ConnectionPhase.Connecting -> "CONNECTING"
                ConnectionPhase.Failed -> "ATTENTION"
                ConnectionPhase.Disconnecting -> "CLOSING"
                ConnectionPhase.Disconnected -> "OFFLINE"
            },
            icon = null,
            emphasized = true,
            accent = accent,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* hero                                                                      */
/* -------------------------------------------------------------------------- */

@Composable
private fun ConnectionHero(
    status: VpnStatus,
    elapsedMs: Long,
    canConnect: Boolean,
    hasProfiles: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRetry: () -> Unit,
    onAddProfile: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val phase = status.phase()
    val accent = tokens.status.of(status)
    val connected = status as? VpnStatus.Connected

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(4.dp))
        ConnectionOrb(status = status, size = 226.dp)
        Spacer(Modifier.height(18.dp))
        SectionLabel(text = status.eyebrow(), color = accent)
        Spacer(Modifier.height(8.dp))
        AnimatedContent(
            targetState = status.headline(),
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(initialScale = 0.94f, animationSpec = tween(280)))
                    .togetherWith(fadeOut(tween(150)))
            },
            label = "headline",
        ) { headline ->
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineLarge,
                color = scheme.onBackground,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = status.subtitle(),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        if (connected != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tokens.glassStrongBottom)
                    .border(1.dp, tokens.glassBorder, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.Clock,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    text = "SESSION",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatDuration(elapsedMs),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontFeatureSettings = "tnum",
                    ),
                    color = accent,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        when (phase) {
            ConnectionPhase.Disconnected -> {
                if (hasProfiles) {
                    AuroraButton(
                        label = "Connect",
                        icon = AppIcons.Power,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canConnect,
                        onClick = onConnect,
                    )
                } else {
                    AuroraButton(
                        label = "Add your first profile",
                        icon = AppIcons.Plus,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onAddProfile,
                    )
                }
            }

            ConnectionPhase.Connecting -> AuroraButton(
                label = "Connecting…",
                icon = AppIcons.Shield,
                modifier = Modifier.fillMaxWidth(),
                style = AuroraButtonStyle.Glass,
                enabled = false,
                onClick = {},
            )

            ConnectionPhase.Connected -> AuroraButton(
                label = "Disconnect",
                icon = AppIcons.Power,
                modifier = Modifier.fillMaxWidth(),
                style = AuroraButtonStyle.Danger,
                onClick = onDisconnect,
            )

            ConnectionPhase.Disconnecting -> AuroraButton(
                label = "Disconnecting…",
                icon = AppIcons.Power,
                modifier = Modifier.fillMaxWidth(),
                style = AuroraButtonStyle.Glass,
                enabled = false,
                onClick = {},
            )

            ConnectionPhase.Failed -> AuroraButton(
                label = "Try again",
                icon = AppIcons.Refresh,
                modifier = Modifier.fillMaxWidth(),
                onClick = onRetry,
            )
        }
        if (phase == ConnectionPhase.Disconnected && hasProfiles) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap a profile below to switch servers",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* traffic                                                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun TrafficRow(status: VpnStatus.Connected) {
    val tokens = LocalAuroraColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TrafficCard(
            modifier = Modifier.weight(1f),
            icon = AppIcons.Download,
            label = "Download",
            bytes = status.stats.rxBytes,
            packets = status.stats.rxPackets,
            tint = tokens.status.connected,
        )
        TrafficCard(
            modifier = Modifier.weight(1f),
            icon = AppIcons.Upload,
            label = "Upload",
            bytes = status.stats.txBytes,
            packets = status.stats.txPackets,
            tint = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun TrafficCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bytes: Long,
    packets: Long,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        AnimatedStatText(
            value = bytes,
            format = ::formatBytes,
            style = MaterialTheme.typography.titleLarge,
            color = scheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "${formatPacketCount(packets)} packets",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

/* -------------------------------------------------------------------------- */
/* failure                                                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun FailureCard(message: String) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = scheme.error.copy(alpha = 0.35f),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = AppIcons.AlertTriangle,
                contentDescription = null,
                tint = scheme.error,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Last connection attempt failed",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* details                                                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun DetailsCard(status: VpnStatus.Connected) {
    val scheme = MaterialTheme.colorScheme
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "detailsChevron",
    )
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = AppIcons.Server,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Connection details",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                )
                Text(
                    text = "Address, MTU, ciphers and session start",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = if (expanded) {
                    "Collapse connection details"
                } else {
                    "Expand connection details"
                },
                tint = scheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(240)) + fadeIn(tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(tween(120)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            ) {
                GlassDivider()
                Spacer(Modifier.height(10.dp))
                DetailRow(label = "Server", value = status.server)
                DetailRow(label = "Local address", value = status.localAddresses())
                DetailRow(
                    label = "MTU",
                    value = if (status.mtu > 0) status.mtu.toString() else "—",
                )
                DetailRow(
                    label = "DNS",
                    value = status.dns.joinToString(", ").ifBlank { "—" },
                )
                DetailRow(label = "Protocol", value = protocolLabel(status.protocol))
                DetailRow(
                    label = "TLS cipher",
                    value = status.cstpCipher?.takeIf { it.isNotBlank() } ?: "—",
                )
                DetailRow(
                    label = "DTLS (UDP)",
                    value = status.dtlsSummary(),
                    accent = if (status.dtlsState == DtlsState.ACTIVE) {
                        LocalAuroraColors.current.status.connected
                    } else {
                        null
                    },
                )
                DetailRow(label = "Session start", value = formatClockTime(status.since))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, accent: Color? = null) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.width(104.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            ),
            color = accent ?: scheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/* -------------------------------------------------------------------------- */
/* profile list                                                              */
/* -------------------------------------------------------------------------- */

@Composable
private fun ProfilesHeader(count: Int, onAddProfile: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Profiles",
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onBackground,
        )
        Spacer(Modifier.width(8.dp))
        MetaChip(text = count.toString())
        Spacer(Modifier.weight(1f))
        if (count > 0) {
            Text(
                text = "Tap to connect",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
            Spacer(Modifier.width(10.dp))
        }
        GlassIconButton(
            icon = AppIcons.Plus,
            contentDescription = "Add profile",
            onClick = onAddProfile,
            size = 38.dp,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableProfileCard(
    profile: Profile,
    isDefault: Boolean,
    isConnected: Boolean,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = { distance -> distance * 0.35f },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            onDeleteRequest()
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(RoundedCornerShape(22.dp)),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { /* handled through currentValue so the row can be reset */ },
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                scheme.error.copy(alpha = 0.05f),
                                scheme.error.copy(alpha = 0.28f),
                            ),
                        ),
                    )
                    .padding(horizontal = 22.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = AppIcons.Trash,
                    contentDescription = "Delete profile",
                    tint = scheme.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.error,
                )
            }
        },
    ) {
        ProfileCardContent(
            profile = profile,
            isDefault = isDefault,
            isConnected = isConnected,
            isBusy = isBusy,
            onConnect = onConnect,
            onEdit = onEdit,
            onDeleteRequest = onDeleteRequest,
            onSetDefault = onSetDefault,
        )
    }
}

@Composable
private fun ProfileCardContent(
    profile: Profile,
    isDefault: Boolean,
    isConnected: Boolean,
    isBusy: Boolean,
    onConnect: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onSetDefault: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val connectedAccent = tokens.status.connected
    var menuExpanded by remember { mutableStateOf(false) }
    val border = when {
        isConnected -> connectedAccent.copy(alpha = 0.45f)
        isDefault -> scheme.primary.copy(alpha = 0.18f)
        else -> null
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        borderColor = border,
        contentPadding = PaddingValues(0.dp),
        onClick = if (isBusy) null else onConnect,
        opaqueBase = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(name = profile.displayName, seed = profile.id, size = 46.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isDefault) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = AppIcons.Star,
                            contentDescription = "Default profile",
                            tint = tokens.status.connecting,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    if (isConnected) {
                        Spacer(Modifier.width(8.dp))
                        LiveDot(color = connectedAccent, size = 7.dp)
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = connectedAccent,
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = profile.serverHost.ifBlank { "No server configured" },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (profile.username.isNotBlank()) {
                    Text(
                        text = profile.username,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaChip(text = protocolChipLabel(profile.protocol))
                    if (profile.totpEnabled) {
                        MetaChip(text = "TOTP", icon = AppIcons.Key)
                    }
                    if (profile.disableDtls) {
                        MetaChip(text = "TLS only")
                    }
                }
            }
            Box {
                GlassIconButton(
                    icon = AppIcons.MoreVert,
                    contentDescription = "Profile actions for ${profile.displayName}",
                    onClick = { menuExpanded = true },
                    size = 36.dp,
                    tint = scheme.onSurfaceVariant,
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (!isDefault) {
                        DropdownMenuItem(
                            text = { Text("Set as default") },
                            leadingIcon = {
                                Icon(
                                    imageVector = AppIcons.StarOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp),
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSetDefault()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Edit profile") },
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Pencil,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = {
                            Icon(
                                imageVector = AppIcons.Trash,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = scheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteRequest()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyProfiles(onAddProfile: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(scheme.primary.copy(alpha = 0.18f), Color.Transparent),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(tokens.glassStrongTop)
                        .border(
                            1.dp,
                            scheme.primary.copy(alpha = 0.28f),
                            RoundedCornerShape(20.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.ShieldCheck,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No profiles yet",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Add your first OpenConnect profile to build an encrypted tunnel.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            AuroraButton(
                label = "Add profile",
                icon = AppIcons.Plus,
                onClick = onAddProfile,
            )
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
