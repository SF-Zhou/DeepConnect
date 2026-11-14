@file:OptIn(ExperimentalMaterial3Api::class)

package com.deepconnect.vpn.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.deepconnect.vpn.data.Profile
import com.deepconnect.vpn.data.Protocols
import com.deepconnect.vpn.data.ReportedOs

/**
 * Create / edit a saved profile. The form state is local; on save the existing profile is
 * copied so fields that are not exposed here (user agent, auth group, pinned hash) survive.
 */
@Composable
fun EditProfileScreen(
    viewModel: AppViewModel,
    profileId: Long,
    onBack: () -> Unit,
) {
    val profiles by viewModel.profiles.collectAsState()
    val existing = remember(profileId, profiles) {
        profiles.firstOrNull { it.id == profileId }
    }
    val editing = profileId != 0L

    var name by rememberSaveable(profileId) { mutableStateOf(existing?.name.orEmpty()) }
    var server by rememberSaveable(profileId) { mutableStateOf(existing?.server.orEmpty()) }
    var username by rememberSaveable(profileId) { mutableStateOf(existing?.username.orEmpty()) }
    // Never put the password / TOTP secret into rememberSaveable: saved instance state is
    // written out by the system and could persist plaintext secrets.
    var password by remember(profileId) { mutableStateOf(existing?.password.orEmpty()) }
    var showPassword by rememberSaveable(profileId) { mutableStateOf(false) }

    var totpEnabled by rememberSaveable(profileId) {
        mutableStateOf(existing?.totpEnabled ?: false)
    }
    var totpSecret by remember(profileId) {
        mutableStateOf(existing?.totpSecret.orEmpty())
    }
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
    var serverError by rememberSaveable { mutableStateOf(false) }
    var usernameError by rememberSaveable { mutableStateOf(false) }

    val canSave = server.isNotBlank() && username.isNotBlank()

    fun attemptSave() {
        serverError = server.isBlank()
        usernameError = username.isBlank()
        if (serverError || usernameError) return

        val base = existing ?: Profile()
        viewModel.saveProfile(
            base.copy(
                id = if (editing) profileId else 0L,
                name = name.trim(),
                server = server.trim(),
                username = username.trim(),
                password = password,
                totpEnabled = totpEnabled,
                // Disabling TOTP keeps the (encrypted) secret so re-enabling it later
                // restores the previous code generator instead of silently dropping it.
                totpSecret = if (totpEnabled) totpSecret.trim() else base.totpSecret,
                protocol = protocol,
                reportedOs = reportedOs,
                disableDtls = disableDtls,
                mtu = mtuText.toIntOrNull() ?: 0,
                extraCaPem = extraCaPem.trim(),
                pinnedCertHash = pinnedHash,
            ),
        )
        onBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (editing) "Edit profile" else "New profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editing) {
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = { attemptSave() },
                        enabled = canSave,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (editing) "Save changes" else "Save profile")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Connection") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile name") },
                    placeholder = { Text("Optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = server,
                    onValueChange = { value ->
                        server = value
                        if (serverError && value.isNotBlank()) serverError = false
                    },
                    label = { Text("Server address") },
                    placeholder = { Text("vpn.example.com or https://vpn.example.com/group") },
                    isError = serverError,
                    supportingText = {
                        Text(
                            if (serverError) {
                                "Server address is required"
                            } else {
                                "Hostname or full https:// URL, optionally with a group path"
                            },
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { value ->
                        username = value
                        if (usernameError && value.isNotBlank()) usernameError = false
                    },
                    label = { Text("Username") },
                    isError = usernameError,
                    supportingText = {
                        Text(
                            if (usernameError) {
                                "Username is required"
                            } else {
                                "Account used for the VPN session"
                            },
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
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
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (showPassword) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                },
                            )
                        }
                    },
                    supportingText = { Text("Stored encrypted on this device") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionCard(title = "One-time password") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Use TOTP", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Generate a one-time code from a shared secret.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = totpEnabled, onCheckedChange = { totpEnabled = it })
                }
                if (totpEnabled) {
                    OutlinedTextField(
                        value = totpSecret,
                        onValueChange = { totpSecret = it },
                        label = { Text("TOTP secret") },
                        singleLine = true,
                        visualTransformation = if (showTotpSecret) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showTotpSecret = !showTotpSecret }) {
                                Icon(
                                    imageVector = if (showTotpSecret) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (showTotpSecret) {
                                        "Hide TOTP secret"
                                    } else {
                                        "Show TOTP secret"
                                    },
                                )
                            }
                        },
                        supportingText = {
                            Text(
                                "Base32 secret (e.g. JBSWY3DPEHPK3PXP) or a complete " +
                                    "otpauth:// URI",
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            AdvancedSection(
                expanded = advancedExpanded,
                onToggle = { advancedExpanded = !advancedExpanded },
            ) {
                DropdownField(
                    label = "Protocol",
                    value = Protocols.labelFor(protocol),
                    options = Protocols.ALL.map { entry -> entry.id to entry.label },
                    onSelect = { protocol = it },
                )
                DropdownField(
                    label = "Reported OS",
                    value = ReportedOs.labelFor(reportedOs),
                    options = ReportedOs.ALL.map { os -> os to ReportedOs.labelFor(os) },
                    onSelect = { reportedOs = it },
                )
                OutlinedTextField(
                    value = mtuText,
                    onValueChange = { value -> mtuText = value.filter { it.isDigit() }.take(4) },
                    label = { Text("MTU") },
                    placeholder = { Text("Automatic") },
                    supportingText = { Text("Empty lets the server decide") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Disable DTLS", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Fall back to a TLS-only transport.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = disableDtls, onCheckedChange = { disableDtls = it })
                }
                OutlinedTextField(
                    value = extraCaPem,
                    onValueChange = { extraCaPem = it },
                    label = { Text("Extra CA certificate (PEM)") },
                    placeholder = { Text("-----BEGIN CERTIFICATE-----") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
                PinnedCertificateRow(
                    pinnedHash = pinnedHash,
                    onClear = { pinnedHash = "" },
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete profile?") },
            text = {
                Text(
                    "\"${existing?.displayName ?: "This profile"}\" will be removed from " +
                        "this device. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteProfile(profileId)
                        onBack()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

/* ---------------------------------------------------------------------- */
/* building blocks                                                        */
/* ---------------------------------------------------------------------- */

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun AdvancedSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Protocol, MTU, DTLS and certificates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse advanced options" else "Expand advanced options",
                )
            }
            if (expanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (id, optionLabel) ->
                    DropdownMenuItem(
                        text = { Text(optionLabel) },
                        onClick = {
                            expanded = false
                            onSelect(id)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedCertificateRow(
    pinnedHash: String,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Fingerprint,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pinned certificate",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pinnedHash.isBlank()) {
                Text(
                    text = "None. You will be asked to trust the server certificate on first use.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = pinnedHash,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (pinnedHash.isNotBlank()) {
            TextButton(onClick = onClear) {
                Text("Clear")
            }
        }
    }
}
