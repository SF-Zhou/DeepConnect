package com.deepconnect.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.theme.LocalAuroraColors

/* -------------------------------------------------------------------------- */
/* top bar                                                                   */
/* -------------------------------------------------------------------------- */

/** Transparent app bar with the status-bar inset handled internally. */
@Composable
fun AuroraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 64.dp)
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(4.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/* -------------------------------------------------------------------------- */
/* segmented control                                                         */
/* -------------------------------------------------------------------------- */

/**
 * Custom pill segmented control (the Material segmented button is still
 * experimental and does not match the glass language).
 */
@Composable
fun <T> AuroraSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
) {
    val tokens = LocalAuroraColors.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(tokens.glassStrongBottom)
            .border(1.dp, tokens.glassBorder, CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) scheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                animationSpec = androidx.compose.animation.core.tween(220),
                label = "segmentBackground",
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                animationSpec = androidx.compose.animation.core.tween(220),
                label = "segmentContent",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(background)
                    .clickable(onClick = { onSelect(option) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Horizontally scrollable row of choice pills (used for log levels). */
@Composable
fun <T> ChoicePillRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
) {
    val tokens = LocalAuroraColors.current
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val background by animateColorAsState(
                targetValue = if (isSelected) {
                    scheme.primary.copy(alpha = 0.18f)
                } else {
                    tokens.glassStrongTop
                },
                label = "pillBackground",
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(background)
                    .border(
                        1.dp,
                        if (isSelected) scheme.primary.copy(alpha = 0.35f) else tokens.glassBorder,
                        CircleShape,
                    )
                    .clickable(onClick = { onSelect(option) })
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* setting rows                                                              */
/* -------------------------------------------------------------------------- */

@Composable
fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    SettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        enabled = enabled,
        onClick = if (enabled) ({ onCheckedChange(!checked) }) else null,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = Color.Transparent,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = LocalAuroraColors.current.glassStrongBottom,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                ),
            )
        },
    )
}

@Composable
fun SettingNavRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    onClick: () -> Unit,
) {
    SettingRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        value = value,
        modifier = modifier,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        },
    )
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.primary.copy(alpha = if (tokens.isDark) 0.14f else 0.10f))
                .border(1.dp, scheme.primary.copy(alpha = 0.16f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) scheme.onSurface else scheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(10.dp))
            trailing()
        }
    }
}

/* -------------------------------------------------------------------------- */
/* grouped section card                                                      */
/* -------------------------------------------------------------------------- */

/** Glass card with an icon + title header, used to group settings and form fields. */
@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = contentPadding,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(scheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(10.dp))
        content()
    }
}

/* -------------------------------------------------------------------------- */
/* text fields                                                               */
/* -------------------------------------------------------------------------- */

@Composable
fun AuroraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = scheme.onSurface,
            unfocusedTextColor = scheme.onSurface,
            disabledTextColor = scheme.onSurfaceVariant,
            errorTextColor = scheme.onSurface,
            focusedContainerColor = tokens.glassStrongBottom,
            unfocusedContainerColor = tokens.glassStrongBottom,
            errorContainerColor = scheme.errorContainer.copy(alpha = 0.12f),
            cursorColor = scheme.primary,
            focusedBorderColor = scheme.primary.copy(alpha = 0.75f),
            unfocusedBorderColor = scheme.outlineVariant,
            errorBorderColor = scheme.error,
            focusedLabelColor = scheme.primary,
            unfocusedLabelColor = scheme.onSurfaceVariant,
            errorLabelColor = scheme.error,
            focusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            unfocusedPlaceholderColor = scheme.onSurfaceVariant.copy(alpha = 0.7f),
            focusedSupportingTextColor = scheme.onSurfaceVariant,
            unfocusedSupportingTextColor = scheme.onSurfaceVariant,
            errorSupportingTextColor = scheme.error,
        ),
        supportingText = supportingText?.let {
            {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
    )
}

/**
 * Read-only field that opens a dropdown menu. Used for protocol / reported OS pickers so
 * the form keeps the same visual language as its text fields.
 */
@Composable
fun AuroraDropdownField(
    label: String,
    selectedId: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    helper: String? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedId }?.second ?: selectedId
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.glassStrongBottom)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = AppIcons.ChevronDown,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (id, optionLabel) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = optionLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (id == selectedId) scheme.primary else scheme.onSurface,
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(id)
                        },
                    )
                }
            }
        }
        if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 5.dp),
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* misc                                                                      */
/* -------------------------------------------------------------------------- */

/** Small "kbd"-style monospace value used for library versions. */
@Composable
fun CodeValue(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        color = color,
        modifier = modifier,
    )
}

/** Round brand mark shown in the home top bar and the About card. */
@Composable
fun BrandMark(size: Dp = 40.dp) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primary.copy(alpha = 0.28f),
                        scheme.secondary.copy(alpha = 0.16f),
                    ),
                ),
            )
            .border(1.dp, scheme.primary.copy(alpha = 0.35f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.ShieldCheck,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}
