@file:OptIn(ExperimentalMaterial3Api::class)

package com.deepconnect.vpn.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.deepconnect.vpn.data.Profile
import com.deepconnect.vpn.data.Protocols
import com.deepconnect.vpn.data.ReportedOs
import com.deepconnect.vpn.ui.AppViewModel
import com.deepconnect.vpn.ui.components.AuroraButton
import com.deepconnect.vpn.ui.components.AuroraButtonStyle
import com.deepconnect.vpn.ui.components.AuroraConfirmDialog
import com.deepconnect.vpn.ui.components.AuroraDropdownField
import com.deepconnect.vpn.ui.components.AuroraTextField
import com.deepconnect.vpn.ui.components.AuroraTopBar
import com.deepconnect.vpn.ui.components.GlassCard
import com.deepconnect.vpn.ui.components.GlassDivider
import com.deepconnect.vpn.ui.components.GlassIconButton
import com.deepconnect.vpn.ui.components.SectionCard
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.theme.LocalAuroraColors

/**
 * Create / edit a saved profile.
 *
 * Non-secret fields live in saved-instance state; the password and TOTP secret are kept in
 * plain composition state on purpose so nothing secret is written to the system bundle.
 */
@Composable
fun EditProfileScreen(
    viewModel: AppViewModel,
    profileId: Long,
    onBack: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val haptics = LocalHapticFeedback.current
    val profiles by viewModel.profiles.collectAsState()
    val existing = remember(profileId, profiles) { profiles.firstOrNull { it.id == profileId } }
    val editing = profileId != 0L

    var name by rememberSaveable(profileId) { mutableStateOf(existing?.name.orEmpty()) }
    var server by rememberSaveable(profileId) { mutableStateOf(existing?.server.orEmpty()) }
    var username by rememberSaveable(profileId) { mutableStateOf(existing?.username.orEmpty()) }
    var password by remember(profileId) { mutableStateOf(existing?.password.orEmpty()) }
    var showPassword by rememberSaveable(profileId) { mutableStateOf(false) }

    var totpEnabled by rememberSaveable(profileId) {
        mutableStateOf(existing?.totpEnabled ?: false)
    }
    var totpSecret by remember(profileId) { mutableStateOf(existing?.totpSecret.orEmpty()) }
    var showTotpSecret by rememberSaveable(profileId) { mutableStateOf(false) }

    var protocol by rememberSaveable(profileId) {
        mutableStateOf(existing?.protocol ?: "anyconnect")
    }
    var reportedOs by rememberSaveable(profileId) {
        mutableStateOf(existing?.reportedOs ?: "linux")
    }
    var mtuText by rememberSaveable(profileId) {
        mutableStateOf(existing?.mtu?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var disableDtls by rememberSaveable(profileId) {
        mutableStateOf(existing?.disableDtls ?: false)
    }
    var extraCaPem by rememberSaveable(profileId) {
        mutableStateOf(existing?.extraCaPem.orEmpty())
    }
    var pinnedHash by rememberSaveable(profileId) {
        mutableStateOf(existing?.pinnedCertHash.orEmpty())
    }

    var advancedExpanded by rememberSaveable(profileId) { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }
    var serverError by rememberSaveable { mutableStateOf(false) }
    var usernameError by rememberSaveable { mutableStateOf(false) }

    val baseline = remember(profileId, existing) { FormValues.from(existing) }
    val form = FormValues(
        name = name,
        server = server,
        username = username,
        password = password,
        totpEnabled = totpEnabled,
        totpSecret = totpSecret,
        protocol = protocol,
        reportedOs = reportedOs,
        mtuText = mtuText,
        disableDtls = disableDtls,
        extraCaPem = extraCaPem,
        pinnedHash = pinnedHash,
    )
    val isDirty = form != baseline
    val canSave = server.isNotBlank() && username.isNotBlank()

    fun attemptSave() {
        serverError = server.isBlank()
        usernameError = username.isBlank()
        if (serverError || usernameError) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
            return
        }
        val base = existing ?: Profile()
        viewModel.saveProfile(
            base.copy(
                id = if (editing) profileId else 0L,
                name = name.trim(),
                server = server.trim(),
                username = username.trim(),
                password = password,
                totpEnabled = totpEnabled,
                // Disabling TOTP keeps the encrypted secret so re-enabling restores it.
                totpSecret = if (totpEnabled) totpSecret.trim() else base.totpSecret,
                protocol = protocol,
                reportedOs = reportedOs,
                disableDtls = disableDtls,
                mtu = mtuText.toIntOrNull() ?: 0,
                extraCaPem = extraCaPem.trim(),
                pinnedCertHash = pinnedHash,
            ),
        )
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        onBack()
    }

    fun requestBack() {
        if (isDirty) showDiscardDialog = true else onBack()
    }

    BackHandler(enabled = true) { requestBack() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AuroraTopBar(
                title = if (editing) "Edit profile" else "New profile",
                subtitle = if (editing) existing?.displayName else "OpenConnect profile",
                leading = {
                    GlassIconButton(
                        icon = AppIcons.ChevronLeft,
                        contentDescription = "Back",
                        onClick = { requestBack() },
                        size = 38.dp,
                    )
                },
                actions = {
                    GlassIconButton(
                        icon = AppIcons.Check,
                        contentDescription = "Save profile",
                        onClick = { attemptSave() },
                        size = 38.dp,
                        enabled = canSave,
                        tint = if (canSave) scheme.primary else scheme.onSurfaceVariant,
                    )
                },
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, tokens.backdropBottom.copy(alpha = 0.96f)),
                        ),
                    )
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
            ) {
                AuroraButton(
                    label = if (editing) "Save changes" else "Save profile",
                    icon = AppIcons.Check,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canSave,
                    onClick = { attemptSave() },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 130.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "connection") {
                SectionCard(title = "Connection", icon = AppIcons.Globe) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AuroraTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Profile name",
                            placeholder = "Optional, defaults to the server",
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )
                        AuroraTextField(
                            value = server,
                            onValueChange = { value ->
                                server = value
                                if (serverError && value.isNotBlank()) serverError = false
                            },
                            label = "Server address",
                            placeholder = "vpn.example.com or https://vpn.example.com/group",
                            isError = serverError,
                            supportingText = if (serverError) {
                                "Server address is required"
                            } else {
                                "Hostname or full https:// URL, optionally with a group path."
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        AuroraTextField(
                            value = username,
                            onValueChange = { value ->
                                username = value
                                if (usernameError && value.isNotBlank()) usernameError = false
                            },
                            label = "Username",
                            placeholder = "VPN account",
                            isError = usernameError,
                            supportingText = if (usernameError) {
                                "Username is required"
                            } else {
                                "Account used for the VPN session."
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                        )
                        AuroraTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            singleLine = true,
                            visualTransformation = if (showPassword) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { attemptSave() }),
                            trailingIcon = {
                                EyeToggle(
                                    visible = showPassword,
                                    contentDescription = if (showPassword) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    },
                                    onToggle = { showPassword = !showPassword },
                                )
                            },
                            supportingText = "Stored encrypted on this device.",
                        )
                    }
                }
            }

            item(key = "totp") {
                SectionCard(title = "One-time password", icon = AppIcons.Key) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use TOTP",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                            )
                            Text(
                                text = "Generate a one-time code from a shared secret.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = totpEnabled,
                            onCheckedChange = { totpEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = scheme.onPrimary,
                                checkedTrackColor = scheme.primary,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = scheme.onSurfaceVariant,
                                uncheckedTrackColor = tokens.glassStrongBottom,
                                uncheckedBorderColor = scheme.outline,
                            ),
                        )
                    }
                    AnimatedVisibility(
                        visible = totpEnabled,
                        enter = expandVertically(animationSpec = tween(220)) + fadeIn(tween(180)),
                        exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120)),
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            AuroraTextField(
                                value = totpSecret,
                                onValueChange = { totpSecret = it },
                                label = "TOTP secret",
                                singleLine = true,
                                visualTransformation = if (showTotpSecret) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    EyeToggle(
                                        visible = showTotpSecret,
                                        contentDescription = if (showTotpSecret) {
                                            "Hide TOTP secret"
                                        } else {
                                            "Show TOTP secret"
                                        },
                                        onToggle = { showTotpSecret = !showTotpSecret },
                                    )
                                },
                                supportingText = "Base32 secret (e.g. JBSWY3DPEHPK3PXP) " +
                                    "or a complete otpauth:// URI.",
                            )
                        }
                    }
                }
            }

            item(key = "advanced") {
                AdvancedSection(
                    expanded = advancedExpanded,
                    onToggle = { advancedExpanded = !advancedExpanded },
                ) {
                    AuroraDropdownField(
                        label = "Protocol",
                        selectedId = protocol,
                        options = Protocols.ALL.map { entry -> entry.id to entry.label },
                        onSelect = { protocol = it },
                    )
                    AuroraDropdownField(
                        label = "Reported OS",
                        selectedId = reportedOs,
                        options = ReportedOs.ALL.map { os -> os to ReportedOs.labelFor(os) },
                        onSelect = { reportedOs = it },
                        helper = "OS string announced to the gateway during the handshake.",
                    )
                    AuroraTextField(
                        value = mtuText,
                        onValueChange = { value -> mtuText = value.filter { it.isDigit() }.take(4) },
                        label = "MTU",
                        placeholder = "Automatic",
                        supportingText = "Leave empty to let the server decide.",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next,
                        ),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Disable DTLS",
                                style = MaterialTheme.typography.bodyLarge,
                                color = scheme.onSurface,
                            )
                            Text(
                                text = "Fall back to a TLS-only transport over TCP.",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = disableDtls,
                            onCheckedChange = { disableDtls = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = scheme.onPrimary,
                                checkedTrackColor = scheme.primary,
                                checkedBorderColor = Color.Transparent,
                                uncheckedThumbColor = scheme.onSurfaceVariant,
                                uncheckedTrackColor = tokens.glassStrongBottom,
                                uncheckedBorderColor = scheme.outline,
                            ),
                        )
                    }
                    AuroraTextField(
                        value = extraCaPem,
                        onValueChange = { extraCaPem = it },
                        label = "Extra CA certificate (PEM)",
                        placeholder = "-----BEGIN CERTIFICATE-----",
                        singleLine = false,
                        minLines = 4,
                        maxLines = 8,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Ascii,
                            imeAction = ImeAction.Default,
                        ),
                    )
                    PinnedCertificateRow(
                        pinnedHash = pinnedHash,
                        onClear = { pinnedHash = "" },
                    )
                }
            }

            if (editing) {
                item(key = "delete") {
                    AuroraButton(
                        label = "Delete profile",
                        icon = AppIcons.Trash,
                        modifier = Modifier.fillMaxWidth(),
                        style = AuroraButtonStyle.Danger,
                        onClick = { showDeleteDialog = true },
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AuroraConfirmDialog(
            title = "Delete profile?",
            message = "\"${existing?.displayName ?: "This profile"}\" will be removed from " +
                "this device. This cannot be undone.",
            confirmLabel = "Delete profile",
            icon = AppIcons.Trash,
            destructive = true,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteProfile(profileId)
                onBack()
            },
        )
    }

    if (showDiscardDialog) {
        AuroraConfirmDialog(
            title = "Discard changes?",
            message = "You have unsaved changes on this profile. Leaving now will lose them.",
            confirmLabel = "Discard changes",
            icon = AppIcons.AlertTriangle,
            destructive = true,
            onDismiss = { showDiscardDialog = false },
            onConfirm = {
                showDiscardDialog = false
                onBack()
            },
        )
    }
}

/* -------------------------------------------------------------------------- */
/* building blocks                                                           */
/* -------------------------------------------------------------------------- */

@Composable
private fun EyeToggle(
    visible: Boolean,
    contentDescription: String,
    onToggle: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (visible) AppIcons.EyeOff else AppIcons.Eye,
            contentDescription = contentDescription,
            tint = scheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun AdvancedSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(220),
        label = "advancedChevron",
    )
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(scheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Tune,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Advanced",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Protocol, MTU, DTLS and certificates",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = AppIcons.ChevronDown,
                contentDescription = if (expanded) {
                    "Collapse advanced options"
                } else {
                    "Expand advanced options"
                },
                tint = scheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = chevronRotation },
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(240)) + fadeIn(tween(180)),
            exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(tween(120)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                GlassDivider()
                content()
            }
        }
    }
}

@Composable
private fun PinnedCertificateRow(
    pinnedHash: String,
    onClear: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(tokens.glassStrongBottom)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = AppIcons.Fingerprint,
            contentDescription = null,
            tint = if (pinnedHash.isBlank()) scheme.onSurfaceVariant else scheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pinned certificate",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
            )
            if (pinnedHash.isBlank()) {
                Text(
                    text = "None. You will be asked to trust the server certificate on " +
                        "first use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = pinnedHash,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (pinnedHash.isNotBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Clear",
                style = MaterialTheme.typography.labelLarge,
                color = scheme.error,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* form snapshot                                                             */
/* -------------------------------------------------------------------------- */

private data class FormValues(
    val name: String,
    val server: String,
    val username: String,
    val password: String,
    val totpEnabled: Boolean,
    val totpSecret: String,
    val protocol: String,
    val reportedOs: String,
    val mtuText: String,
    val disableDtls: Boolean,
    val extraCaPem: String,
    val pinnedHash: String,
) {
    companion object {
        fun from(profile: Profile?): FormValues = FormValues(
            name = profile?.name.orEmpty(),
            server = profile?.server.orEmpty(),
            username = profile?.username.orEmpty(),
            password = profile?.password.orEmpty(),
            totpEnabled = profile?.totpEnabled ?: false,
            totpSecret = profile?.totpSecret.orEmpty(),
            protocol = profile?.protocol ?: "anyconnect",
            reportedOs = profile?.reportedOs ?: "linux",
            mtuText = profile?.mtu?.takeIf { it > 0 }?.toString().orEmpty(),
            disableDtls = profile?.disableDtls ?: false,
            extraCaPem = profile?.extraCaPem.orEmpty(),
            pinnedHash = profile?.pinnedCertHash.orEmpty(),
        )
    }
}
