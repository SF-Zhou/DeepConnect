package com.deepconnect.vpn.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.theme.LocalAuroraColors
import com.deepconnect.vpn.vpn.ConnectionController
import com.deepconnect.vpn.vpn.UserPrompt

/* -------------------------------------------------------------------------- */
/* dialog container                                                          */
/* -------------------------------------------------------------------------- */

/** Rounded glass dialog that replaces the stock AlertDialog throughout the app. */
@Composable
fun AuroraDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAuroraColors.current
    val shape = RoundedCornerShape(30.dp)
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(tokens.glassStrongTop, tokens.glassStrongBottom),
                    ),
                )
                .border(1.dp, tokens.glassBorder, shape)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            content = content,
        )
    }
}

@Composable
private fun DialogHeader(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(tint.copy(alpha = 0.16f))
                .border(1.dp, tint.copy(alpha = 0.28f), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(23.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* prompts that block the native handshake                                   */
/* -------------------------------------------------------------------------- */

@Composable
fun PromptDialogs(prompt: UserPrompt?) {
    when (prompt) {
        is UserPrompt.Certificate -> CertificatePromptDialog(prompt)
        is UserPrompt.OneTimePassword -> OneTimePasswordPromptDialog(prompt)
        null -> Unit
    }
}

@Composable
private fun CertificatePromptDialog(prompt: UserPrompt.Certificate) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val context = LocalContext.current
    var showDetails by remember(prompt) { mutableStateOf(false) }
    var copied by remember(prompt) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (showDetails) 180f else 0f,
        animationSpec = tween(220),
        label = "detailsChevron",
    )

    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1_600)
            copied = false
        }
    }

    AuroraDialog(
        onDismissRequest = { /* A certificate decision must be explicit. */ },
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
    ) {
        DialogHeader(
            icon = AppIcons.ShieldAlert,
            tint = tokens.status.connecting,
            title = "Untrusted certificate",
            subtitle = prompt.profileName,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "${prompt.profileName} presented a certificate this device does not trust. " +
                "Only continue if you expect this server to use a self-signed or private " +
                "certificate.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        if (prompt.message.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = prompt.message,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface,
            )
        }

        Spacer(Modifier.height(14.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.glassStrongBottom)
                .border(1.dp, tokens.glassBorder, RoundedCornerShape(16.dp))
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Fingerprint,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "SHA-256 FINGERPRINT",
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (copied) "Copied" else "Copy",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (copied) tokens.status.connected else scheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            copyToClipboard(context, prompt.fingerprint, "Certificate fingerprint")
                            copied = true
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 84.dp)
                    .horizontalScroll(rememberScrollState()),
            ) {
                SelectionContainer {
                    Text(
                        text = prompt.fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = scheme.onSurface,
                    )
                }
            }
        }

        if (prompt.details.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showDetails = !showDetails }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (showDetails) "Hide certificate details" else "Show certificate details",
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = AppIcons.ChevronDown,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = chevronRotation },
                )
            }
            AnimatedVisibility(
                visible = showDetails,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tokens.glassStrongBottom)
                        .border(1.dp, tokens.glassBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    SelectionContainer {
                        Text(
                            text = prompt.details,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        AuroraButton(
            label = "Trust & remember",
            icon = AppIcons.Check,
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            onClick = { ConnectionController.answerCertificate(accept = true, remember = true) },
        )
        Spacer(Modifier.height(10.dp))
        AuroraButton(
            label = "Trust once",
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            style = AuroraButtonStyle.Glass,
            onClick = { ConnectionController.answerCertificate(accept = true, remember = false) },
        )
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = { ConnectionController.answerCertificate(accept = false, remember = false) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = "Cancel connection",
                color = scheme.error,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun OneTimePasswordPromptDialog(prompt: UserPrompt.OneTimePassword) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    var code by remember(prompt) { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    fun submit() {
        if (code.isNotBlank()) ConnectionController.answerOneTimePassword(code)
    }

    LaunchedEffect(prompt) {
        focusRequester.requestFocus()
        keyboard?.show()
    }

    AuroraDialog(
        onDismissRequest = { /* A decision is required; use Cancel. */ },
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
    ) {
        DialogHeader(
            icon = AppIcons.Lock,
            tint = scheme.primary,
            title = "One-time password",
            subtitle = prompt.profileName,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "${prompt.profileName} requires a one-time code to finish signing in.",
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
        )
        if (prompt.label.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = prompt.label,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface,
            )
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.glassStrongBottom)
                .border(
                    width = if (focused) 1.5.dp else 1.dp,
                    color = if (focused) scheme.primary else scheme.outlineVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 18.dp, vertical = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (code.isEmpty()) {
                Text(
                    text = "Enter code",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 22.sp,
                        letterSpacing = 6.sp,
                    ),
                    color = scheme.onSurfaceVariant.copy(alpha = 0.55f),
                )
            }
            BasicTextField(
                value = code,
                onValueChange = { value -> code = value.filter { it.isDigit() }.take(12) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 22.sp,
                    letterSpacing = 6.sp,
                    color = scheme.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
                singleLine = true,
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        }
        Spacer(Modifier.height(20.dp))
        AuroraButton(
            label = "Continue",
            icon = AppIcons.Check,
            modifier = Modifier.fillMaxWidth(),
            height = 52.dp,
            enabled = code.isNotBlank(),
            onClick = { submit() },
        )
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = { ConnectionController.answerOneTimePassword(null) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = "Cancel",
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* confirmation dialog                                                       */
/* -------------------------------------------------------------------------- */

@Composable
fun AuroraConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = AppIcons.AlertTriangle,
    destructive: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val tint = if (destructive) scheme.error else scheme.primary
    AuroraDialog(onDismissRequest = onDismiss, modifier = modifier) {
        DialogHeader(icon = icon, tint = tint, title = title)
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(22.dp))
        AuroraButton(
            label = confirmLabel,
            modifier = Modifier.fillMaxWidth(),
            height = 50.dp,
            style = if (destructive) AuroraButtonStyle.Danger else AuroraButtonStyle.Primary,
            onClick = onConfirm,
        )
        Spacer(Modifier.height(8.dp))
        AuroraButton(
            label = "Cancel",
            modifier = Modifier.fillMaxWidth(),
            height = 50.dp,
            style = AuroraButtonStyle.Glass,
            onClick = onDismiss,
        )
    }
}

/* -------------------------------------------------------------------------- */
/* helpers                                                                   */
/* -------------------------------------------------------------------------- */

internal fun copyToClipboard(context: Context, text: String, label: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        ?: return
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
}
