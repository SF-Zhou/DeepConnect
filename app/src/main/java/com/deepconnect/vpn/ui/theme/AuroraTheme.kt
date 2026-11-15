package com.deepconnect.vpn.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.deepconnect.vpn.data.AppSettings
import com.deepconnect.vpn.vpn.VpnStatus

/*
 * "Aurora Security" design language.
 *
 * Deep navy / teal aurora backdrop, emerald accents and frosted glass surfaces. The
 * Material color scheme carries the interactive roles; [AuroraColors] carries the extra
 * surface, backdrop and status tokens this app draws itself. Both light and dark are
 * first-class: light uses a mist/ivory base with the same teal character.
 */

/* -------------------------------------------------------------------------- */
/* palette                                                                    */
/* -------------------------------------------------------------------------- */

private val AuroraDarkScheme = darkColorScheme(
    primary = Color(0xFF3FE3AC),
    onPrimary = Color(0xFF01211A),
    primaryContainer = Color(0xFF0B4A3B),
    onPrimaryContainer = Color(0xFFB9F7DE),
    inversePrimary = Color(0xFF0C7C67),
    secondary = Color(0xFF6CC8E8),
    onSecondary = Color(0xFF04222B),
    secondaryContainer = Color(0xFF123A45),
    onSecondaryContainer = Color(0xFFC5EDF8),
    tertiary = Color(0xFFB3A4FF),
    onTertiary = Color(0xFF1B1140),
    tertiaryContainer = Color(0xFF2B2356),
    onTertiaryContainer = Color(0xFFE4DEFF),
    background = Color(0xFF050B10),
    onBackground = Color(0xFFE6F1F2),
    surface = Color(0xFF0A141B),
    onSurface = Color(0xFFE6F1F2),
    surfaceVariant = Color(0xFF16262F),
    onSurfaceVariant = Color(0xFF9CB2B8),
    inverseSurface = Color(0xFFE6F1F2),
    inverseOnSurface = Color(0xFF0A141B),
    error = Color(0xFFFF7B72),
    onError = Color(0xFF2A0503),
    errorContainer = Color(0xFF4A1210),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF33474F),
    outlineVariant = Color(0xFF1E3038),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF24343D),
    surfaceDim = Color(0xFF04090D),
    surfaceContainerLowest = Color(0xFF04090D),
    surfaceContainerLow = Color(0xFF0A141B),
    surfaceContainer = Color(0xFF0F1C24),
    surfaceContainerHigh = Color(0xFF142630),
    surfaceContainerHighest = Color(0xFF1A2E39),
)

private val AuroraLightScheme = lightColorScheme(
    primary = Color(0xFF0C7C67),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB6F2DF),
    onPrimaryContainer = Color(0xFF00251C),
    inversePrimary = Color(0xFF3FE3AC),
    secondary = Color(0xFF0E6C7E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBEEBF6),
    onSecondaryContainer = Color(0xFF001F26),
    tertiary = Color(0xFF5B4BC4),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE3DEFF),
    onTertiaryContainer = Color(0xFF150A47),
    background = Color(0xFFF1F7F6),
    onBackground = Color(0xFF0B1B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0B1B20),
    surfaceVariant = Color(0xFFDCEAE7),
    onSurfaceVariant = Color(0xFF4B615F),
    inverseSurface = Color(0xFF0B1B20),
    inverseOnSurface = Color(0xFFE6F1F2),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF7A938F),
    outlineVariant = Color(0xFFC3D5D1),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFD7E3E0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FCFB),
    surfaceContainer = Color(0xFFEDF5F3),
    surfaceContainerHigh = Color(0xFFE7F0EE),
    surfaceContainerHighest = Color(0xFFE0EAE8),
)

/* -------------------------------------------------------------------------- */
/* status colours                                                            */
/* -------------------------------------------------------------------------- */

/** Semantic colors for the four connection states, outside the Material color roles. */
@Immutable
data class StatusColors(
    val connected: Color,
    val connecting: Color,
    val failed: Color,
    val disconnected: Color,
) {
    fun of(status: VpnStatus): Color = when (status) {
        is VpnStatus.Disconnected -> disconnected
        is VpnStatus.Connecting, is VpnStatus.Disconnecting -> connecting
        is VpnStatus.Connected -> connected
        is VpnStatus.Failed -> failed
    }
}

private val DarkStatusColors = StatusColors(
    connected = Color(0xFF43E8A8),
    connecting = Color(0xFFFFC65C),
    failed = Color(0xFFFF7B72),
    disconnected = Color(0xFF7E96A0),
)

private val LightStatusColors = StatusColors(
    connected = Color(0xFF0A9B6C),
    connecting = Color(0xFFA96C05),
    failed = Color(0xFFB93A2C),
    disconnected = Color(0xFF5D747C),
)

/* -------------------------------------------------------------------------- */
/* theme tokens                                                              */
/* -------------------------------------------------------------------------- */

/**
 * Extra colors the custom surfaces (aurora backdrop, frosted glass cards, orb) need
 * on top of the Material scheme.
 */
@Immutable
data class AuroraColors(
    val isDark: Boolean,
    val status: StatusColors,
    val backdropTop: Color,
    val backdropBottom: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorder: Color,
    val glassStrongTop: Color,
    val glassStrongBottom: Color,
    val divider: Color,
    val cardSolid: Color,
    val navFill: Color,
    val navBorder: Color,
    /** Text/icon color to use on top of a saturated accent (orb core, primary pills). */
    val onAccent: Color,
)

private val DarkAuroraColors = AuroraColors(
    isDark = true,
    status = DarkStatusColors,
    backdropTop = Color(0xFF07131C),
    backdropBottom = Color(0xFF03080D),
    glassTop = Color(0xE6142733),
    glassBottom = Color(0xDB0A1820),
    glassBorder = Color(0x14FFFFFF),
    glassStrongTop = Color(0xF216303C),
    glassStrongBottom = Color(0xEB0C1E28),
    divider = Color(0x12FFFFFF),
    cardSolid = Color(0xFF0D1A22),
    navFill = Color(0xF20A161E),
    navBorder = Color(0x14FFFFFF),
    onAccent = Color(0xFF04120E),
)

private val LightAuroraColors = AuroraColors(
    isDark = false,
    status = LightStatusColors,
    backdropTop = Color(0xFFF7FCFB),
    backdropBottom = Color(0xFFE3EFEC),
    glassTop = Color(0xF2FFFFFF),
    glassBottom = Color(0xE6F7FCFB),
    glassBorder = Color(0x0F0C3B33),
    glassStrongTop = Color(0xFFFFFFFF),
    glassStrongBottom = Color(0xF2F4FAF8),
    divider = Color(0x120C3B33),
    cardSolid = Color(0xFFFFFFFF),
    navFill = Color(0xF7FFFFFF),
    navBorder = Color(0x140C3B33),
    onAccent = Color(0xFFFFFFFF),
)

val LocalAuroraColors = staticCompositionLocalOf { DarkAuroraColors }

/*
 * Material You support is deliberately "harmonised": the wallpaper palette is blended into
 * the DeepConnect palette instead of replacing it. The wallpaper still visibly shifts the
 * accent hues and surface tint (that is what makes the toggle useful), while the app keeps
 * its aurora/emerald identity and contrast. With dynamic colour off the pure hand-tuned
 * palette is used.
 */

private fun lerpColor(base: Color, overlay: Color, fraction: Float): Color =
    lerp(base, overlay, fraction)

/** Blends an opaque [overlay] into [base] while keeping [base]'s alpha. */
private fun tintAlpha(base: Color, overlay: Color, fraction: Float): Color =
    lerp(base.copy(alpha = 1f), overlay, fraction).copy(alpha = base.alpha)

private fun harmonizeScheme(dynamic: ColorScheme, brand: ColorScheme): ColorScheme = brand.copy(
    primary = lerpColor(brand.primary, dynamic.primary, 0.38f),
    primaryContainer = lerpColor(brand.primaryContainer, dynamic.primaryContainer, 0.34f),
    inversePrimary = lerpColor(brand.inversePrimary, dynamic.inversePrimary, 0.34f),
    secondary = lerpColor(brand.secondary, dynamic.secondary, 0.38f),
    secondaryContainer = lerpColor(brand.secondaryContainer, dynamic.secondaryContainer, 0.34f),
    tertiary = lerpColor(brand.tertiary, dynamic.tertiary, 0.34f),
    tertiaryContainer = lerpColor(brand.tertiaryContainer, dynamic.tertiaryContainer, 0.30f),
    surfaceVariant = lerpColor(brand.surfaceVariant, dynamic.surfaceVariant, 0.30f),
    surfaceContainerLowest = lerpColor(brand.surfaceContainerLowest, dynamic.surfaceContainerLowest, 0.30f),
    surfaceContainerLow = lerpColor(brand.surfaceContainerLow, dynamic.surfaceContainerLow, 0.30f),
    surfaceContainer = lerpColor(brand.surfaceContainer, dynamic.surfaceContainer, 0.28f),
    surfaceContainerHigh = lerpColor(brand.surfaceContainerHigh, dynamic.surfaceContainerHigh, 0.26f),
    surfaceContainerHighest = lerpColor(brand.surfaceContainerHighest, dynamic.surfaceContainerHighest, 0.24f),
    outline = lerpColor(brand.outline, dynamic.outline, 0.30f),
    outlineVariant = lerpColor(brand.outlineVariant, dynamic.outlineVariant, 0.28f),
)

private fun harmonizedAuroraColors(dynamic: ColorScheme, dark: Boolean): AuroraColors {
    val base = if (dark) DarkAuroraColors else LightAuroraColors
    return base.copy(
        backdropTop = tintAlpha(base.backdropTop, dynamic.surfaceContainerLow, 0.30f),
        backdropBottom = tintAlpha(base.backdropBottom, dynamic.background, 0.30f),
        glassTop = tintAlpha(base.glassTop, dynamic.surfaceContainerHigh, 0.26f),
        glassBottom = tintAlpha(base.glassBottom, dynamic.surfaceContainer, 0.26f),
        glassBorder = tintAlpha(base.glassBorder, dynamic.outlineVariant, 0.30f),
        glassStrongTop = tintAlpha(base.glassStrongTop, dynamic.surfaceContainerHighest, 0.24f),
        glassStrongBottom = tintAlpha(base.glassStrongBottom, dynamic.surfaceContainerHigh, 0.24f),
        divider = tintAlpha(base.divider, dynamic.outlineVariant, 0.30f),
        cardSolid = lerpColor(base.cardSolid, dynamic.surfaceContainerLow, 0.30f),
        navFill = tintAlpha(base.navFill, dynamic.surfaceContainer, 0.28f),
        navBorder = tintAlpha(base.navBorder, dynamic.outlineVariant, 0.30f),
    )
}

/** Shorthand for the current [AuroraColors]. */
val aurora: AuroraColors
    @Composable get() = LocalAuroraColors.current

/* -------------------------------------------------------------------------- */
/* type & shape                                                              */
/* -------------------------------------------------------------------------- */

private val AuroraTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 21.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.1.sp,
    ),
)

private val AuroraShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/** Monospace text style used for timers, fingerprints and log lines. */
val MonoTabularStyle: TextStyle
    get() = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontFeatureSettings = "tnum",
    )

/* -------------------------------------------------------------------------- */
/* theme entry point                                                         */
/* -------------------------------------------------------------------------- */

/**
 * Applies the DeepConnect "Aurora" theme.
 *
 * @param themeMode one of [AppSettings.THEME_SYSTEM], [AppSettings.THEME_LIGHT] or
 *   [AppSettings.THEME_DARK].
 * @param dynamicColor when true and the platform is Android 12+, the Material You
 *   wallpaper palette is used instead of the fixed aurora palette.
 */
@Composable
fun DeepConnectTheme(
    themeMode: String = AppSettings.THEME_SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> systemDark
    }
    val context = LocalContext.current
    val brandScheme = if (darkTheme) AuroraDarkScheme else AuroraLightScheme
    val dynamicScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        null
    }
    val materialScheme = if (dynamicScheme != null) {
        harmonizeScheme(dynamicScheme, brandScheme)
    } else {
        brandScheme
    }
    val auroraColors = remember(materialScheme, dynamicScheme, darkTheme) {
        if (dynamicScheme != null) {
            harmonizedAuroraColors(dynamicScheme, darkTheme)
        } else if (darkTheme) {
            DarkAuroraColors
        } else {
            LightAuroraColors
        }
    }

    // Keep the system bars legible even when the app forces the opposite of the OS theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalAuroraColors provides auroraColors) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = AuroraTypography,
            shapes = AuroraShapes,
            content = content,
        )
    }
}
