package com.deepconnect.vpn.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.deepconnect.vpn.vpn.VpnStatus

/*
 * DeepConnect design language: deep teal / emerald accents on near-black surfaces for
 * dark mode, and a soft mist-green surface in light mode. The palette intentionally
 * avoids the default Material purple so the app reads as a security / VPN tool.
 */

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FD1A5),
    onPrimary = Color(0xFF00281E),
    primaryContainer = Color(0xFF0C4A3E),
    onPrimaryContainer = Color(0xFFA7F3D0),
    inversePrimary = Color(0xFF0F766E),
    secondary = Color(0xFF82D5C6),
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF14433C),
    onSecondaryContainer = Color(0xFFB8E6DE),
    tertiary = Color(0xFF8FBFEA),
    onTertiary = Color(0xFF04263F),
    tertiaryContainer = Color(0xFF173F5A),
    onTertiaryContainer = Color(0xFFD6E9FF),
    background = Color(0xFF0B1214),
    onBackground = Color(0xFFE0EBEA),
    surface = Color(0xFF0F191C),
    onSurface = Color(0xFFE0EBEA),
    surfaceVariant = Color(0xFF1B2A2E),
    onSurfaceVariant = Color(0xFFA9BFC1),
    inverseSurface = Color(0xFFE0EBEA),
    inverseOnSurface = Color(0xFF1A2B2D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF3E565A),
    outlineVariant = Color(0xFF26383B),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2C3D40),
    surfaceDim = Color(0xFF080F11),
    surfaceContainerLowest = Color(0xFF060C0E),
    surfaceContainerLow = Color(0xFF0F191C),
    surfaceContainer = Color(0xFF131F22),
    surfaceContainerHigh = Color(0xFF18272B),
    surfaceContainerHighest = Color(0xFF1E2F33),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA7E8D8),
    onPrimaryContainer = Color(0xFF00251D),
    inversePrimary = Color(0xFF4FD1A5),
    secondary = Color(0xFF48635D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9E2),
    onSecondaryContainer = Color(0xFF0A211C),
    tertiary = Color(0xFF3D6373),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC1E8FA),
    onTertiaryContainer = Color(0xFF001F29),
    background = Color(0xFFF5FAF8),
    onBackground = Color(0xFF17211F),
    surface = Color(0xFFFBFDFC),
    onSurface = Color(0xFF17211F),
    surfaceVariant = Color(0xFFDCE7E4),
    onSurfaceVariant = Color(0xFF4A5D5A),
    inverseSurface = Color(0xFF2B3533),
    inverseOnSurface = Color(0xFFECF3F1),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF718784),
    outlineVariant = Color(0xFFC0CFCC),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFBFDFC),
    surfaceDim = Color(0xFFD9E3E1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5FAF8),
    surfaceContainer = Color(0xFFEFF5F3),
    surfaceContainerHigh = Color(0xFFE9F0EE),
    surfaceContainerHighest = Color(0xFFE3EBE9),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DeepConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography(),
        shapes = AppShapes,
        content = content,
    )
}

/** Semantic colors for the four connection states, outside the Material color roles. */
@Immutable
data class StatusColors(
    val connected: Color,
    val connecting: Color,
    val failed: Color,
    val disconnected: Color,
)

private val DarkStatusColors = StatusColors(
    connected = Color(0xFF4ADE80),
    connecting = Color(0xFFFBBF24),
    failed = Color(0xFFF87171),
    disconnected = Color(0xFF8EA4A7),
)

private val LightStatusColors = StatusColors(
    connected = Color(0xFF15803D),
    connecting = Color(0xFFB45309),
    failed = Color(0xFFB91C1C),
    disconnected = Color(0xFF64748B),
)

@Composable
fun statusColors(): StatusColors {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) DarkStatusColors else LightStatusColors
}

/** The accent color that represents [status] in the current theme. */
@Composable
fun statusColor(status: VpnStatus): Color {
    val colors = statusColors()
    return when (status) {
        is VpnStatus.Disconnected -> colors.disconnected
        is VpnStatus.Connecting, is VpnStatus.Disconnecting -> colors.connecting
        is VpnStatus.Connected -> colors.connected
        is VpnStatus.Failed -> colors.failed
    }
}
