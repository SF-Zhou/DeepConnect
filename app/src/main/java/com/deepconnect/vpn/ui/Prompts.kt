package com.deepconnect.vpn.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.deepconnect.vpn.vpn.ConnectionController
import com.deepconnect.vpn.vpn.UserPrompt

/**
 * Modal questions that the native OpenConnect handshake is blocked on. They are hosted at the
 * root of the UI so they survive navigation, and every decision is routed back through
 * [ConnectionController].
 */
@Composable
fun PromptDialogs(prompt: UserPrompt?) {
    when (prompt) {
        is UserPrompt.Certificate -> CertificatePromptDialog(prompt)
        is UserPrompt.OneTimePassword -> OneTimePasswordPromptDialog(prompt)
        null -> Unit
    }
}

/** Dialog properties used for prompts: a decision is required, so dismissal is disabled. */
private val BlockingDialogProperties = DialogProperties(
    dismissOnBackPress = false,
    dismissOnClickOutside = false,
)

@Composable
private fun CertificatePromptDialog(prompt: UserPrompt.Certificate) {
    var showDetails by remember(prompt) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* A certificate decision must be explicit. */ },
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Untrusted certificate") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "${prompt.profileName} presented a certificate that this device does " +
                        "not trust. Only continue if you expect this server to use a " +
                        "self-signed or private certificate.",
                )
                if (prompt.message.isNotBlank()) {
                    Text(
                        text = prompt.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "SHA-256 fingerprint",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    SelectionContainer {
                        Text(
                            text = prompt.fingerprint,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                if (prompt.details.isNotBlank()) {
                    TextButton(
                        onClick = { showDetails = !showDetails },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(if (showDetails) "Hide details" else "Show details")
                    }
                    if (showDetails) {
                        SelectionContainer {
                            Text(
                                text = prompt.details,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = {
                        ConnectionController.answerCertificate(accept = true, remember = true)
                    },
                ) {
                    Text("Trust & remember")
                }
                TextButton(
                    onClick = {
                        ConnectionController.answerCertificate(accept = true, remember = false)
                    },
                ) {
                    Text("Trust once")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    ConnectionController.answerCertificate(accept = false, remember = false)
                },
            ) {
                Text("Cancel")
            }
        },
        properties = BlockingDialogProperties,
    )
}

@Composable
private fun OneTimePasswordPromptDialog(prompt: UserPrompt.OneTimePassword) {
    var code by remember(prompt) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(prompt) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = { /* A decision is required; use Cancel. */ },
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = { Text("One-time password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${prompt.profileName} requires a one-time code to finish signing in.")
                if (prompt.label.isNotBlank()) {
                    Text(
                        text = prompt.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { value ->
                        code = value.filter { it.isDigit() }.take(12)
                    },
                    label = { Text("Code") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { ConnectionController.answerOneTimePassword(code) },
                enabled = code.isNotBlank(),
            ) {
                Text("Continue")
            }
        },
        dismissButton = {
            TextButton(onClick = { ConnectionController.answerOneTimePassword(null) }) {
                Text("Cancel")
            }
        },
        properties = BlockingDialogProperties,
    )
}
