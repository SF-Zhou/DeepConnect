package com.deepconnect.vpn.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.profileInitials
import com.deepconnect.vpn.ui.theme.LocalAuroraColors

/* -------------------------------------------------------------------------- */
/* backdrop                                                                  */
/* -------------------------------------------------------------------------- */

/**
 * Full-screen aurora backdrop: a deep vertical gradient with a few soft radial glows.
 *
 * Uses `Brush.radialGradient` discs instead of blur so it stays cheap and works on API 26.
 */
@Composable
fun AuroraBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val tokens = LocalAuroraColors.current
    // The aurora glows keep their brand hues even under Material You dynamic colour so the
    // atmosphere stays recognisably DeepConnect; only the interactive roles follow the theme.
    val glowA = if (tokens.isDark) Color(0xFF35E3AE) else Color(0xFF12A88C)
    val glowB = if (tokens.isDark) Color(0xFF9C8CFF) else Color(0xFF6E5BD8)
    val glowC = if (tokens.isDark) Color(0xFF6CC8E8) else Color(0xFF2E9BC0)
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(tokens.backdropTop, tokens.backdropBottom),
                    startY = 0f,
                    endY = h,
                ),
            )
            drawGlow(
                color = glowA,
                center = Offset(w * 0.14f, h * 0.05f),
                radius = w * 1.00f,
                alpha = if (tokens.isDark) 0.30f else 0.20f,
            )
            drawGlow(
                color = glowB,
                center = Offset(w * 0.98f, h * 0.28f),
                radius = w * 0.80f,
                alpha = if (tokens.isDark) 0.20f else 0.12f,
            )
            drawGlow(
                color = glowC,
                center = Offset(w * 0.30f, h * 0.96f),
                radius = w * 0.90f,
                alpha = if (tokens.isDark) 0.14f else 0.09f,
            )
            // Gentle vignette so the content edges stay calm.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, tokens.backdropBottom.copy(alpha = 0.65f)),
                    startY = h * 0.62f,
                    endY = h,
                ),
            )
        }
        content()
    }
}

private fun DrawScope.drawGlow(color: Color, center: Offset, radius: Float, alpha: Float) {
    if (radius <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = alpha),
                color.copy(alpha = alpha * 0.42f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

/* -------------------------------------------------------------------------- */
/* surfaces                                                                  */
/* -------------------------------------------------------------------------- */

/** Frosted glass card: gradient fill + hairline border, no elevation shadows. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    strong: Boolean = false,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    onClick: (() -> Unit)? = null,
    opaqueBase: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAuroraColors.current
    val fill = Brush.verticalGradient(
        colors = if (strong) {
            listOf(tokens.glassStrongTop, tokens.glassStrongBottom)
        } else {
            listOf(tokens.glassTop, tokens.glassBottom)
        },
    )
    Column(
        modifier = modifier
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (opaqueBase) Modifier.background(tokens.cardSolid) else Modifier)
            .background(fill)
            .border(1.dp, borderColor ?: tokens.glassBorder, shape)
            .padding(contentPadding),
        content = content,
    )
}

/* -------------------------------------------------------------------------- */
/* buttons                                                                  */
/* -------------------------------------------------------------------------- */

enum class AuroraButtonStyle { Primary, Glass, Danger }

/** Large pill call-to-action with a springy press animation. */
@Composable
fun AuroraButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: AuroraButtonStyle = AuroraButtonStyle.Primary,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    val tokens = LocalAuroraColors.current
    val scheme = MaterialTheme.colorScheme
    val fill: Brush
    val contentColor: Color
    val borderColor: Color
    when (style) {
        AuroraButtonStyle.Primary -> {
            fill = Brush.horizontalGradient(
                listOf(scheme.primary, lerp(scheme.primary, scheme.secondary, 0.55f)),
            )
            contentColor = scheme.onPrimary
            borderColor = Color.Transparent
        }

        AuroraButtonStyle.Glass -> {
            fill = Brush.verticalGradient(listOf(tokens.glassStrongTop, tokens.glassStrongBottom))
            contentColor = scheme.onSurface
            borderColor = tokens.glassBorder
        }

        AuroraButtonStyle.Danger -> {
            fill = Brush.verticalGradient(listOf(tokens.glassStrongTop, tokens.glassStrongBottom))
            contentColor = scheme.error
            borderColor = scheme.error.copy(alpha = 0.30f)
        }
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "buttonScale",
    )

    Box(
        modifier = modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = ripple(bounded = true, color = contentColor.copy(alpha = 0.18f)),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                letterSpacing = 0.2.sp,
            )
        }
    }
}

/** Compact icon-only glass button with a guaranteed 48dp touch target. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 42.dp,
    enabled: Boolean = true,
) {
    val tokens = LocalAuroraColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "iconScale",
    )
    Box(
        modifier = modifier.size(size + 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .alpha(if (enabled) 1f else 0.4f)
                .clip(CircleShape)
                .background(tokens.glassStrongTop)
                .border(1.dp, tokens.glassBorder, CircleShape)
                .clickable(
                    enabled = enabled,
                    interactionSource = interaction,
                    indication = ripple(bounded = true, color = tint.copy(alpha = 0.20f)),
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

/** Circular floating action button with a brand gradient and springy press feedback. */
@Composable
fun AuroraCircleFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "fabScale",
    )
    Box(
        modifier = modifier
            .size(60.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(scheme.primary, lerp(scheme.primary, scheme.secondary, 0.6f)),
                ),
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = scheme.onPrimary.copy(alpha = 0.22f)),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = scheme.onPrimary,
            modifier = Modifier.size(26.dp),
        )
    }
}

/* -------------------------------------------------------------------------- */
/* small building blocks                                                     */
/* -------------------------------------------------------------------------- */

/** Uppercase section eyebrow. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = modifier,
    )
}

/** Small pill used for metadata (protocol, TOTP, default, connected…). */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emphasized: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val tokens = LocalAuroraColors.current
    val background = if (emphasized) accent.copy(alpha = 0.16f) else tokens.glassStrongTop
    val contentColor = if (emphasized) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .border(
                1.dp,
                if (emphasized) accent.copy(alpha = 0.22f) else tokens.glassBorder,
                CircleShape,
            )
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Hairline divider tuned for the glass surfaces. */
@Composable
fun GlassDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        color = LocalAuroraColors.current.divider,
    )
}

/** Pulsing dot used to signal a live connection. */
@Composable
fun LiveDot(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 8.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        drawCircle(color = color.copy(alpha = 0.22f), radius = this.size.minDimension * 0.5f)
        drawCircle(color = color, radius = this.size.minDimension * 0.27f)
        drawCircle(
            color = color.copy(alpha = 0.40f),
            radius = this.size.minDimension * 0.40f,
            style = Stroke(width = 1.dp.toPx()),
        )
    }
}

/** Round avatar with a per-profile aurora tint. */
@Composable
fun ProfileAvatar(
    name: String,
    seed: Long,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val tokens = LocalAuroraColors.current
    val palettes = listOf(
        scheme.primary to scheme.secondary,
        scheme.secondary to scheme.tertiary,
        scheme.tertiary to scheme.primary,
    )
    val tints = palettes[((seed.hashCode() and 0x7fffffff) % palettes.size)]
    val initials = profileInitials(name)
    val shape = RoundedCornerShape(size * 0.36f)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(tints.first.copy(alpha = 0.28f), tints.second.copy(alpha = 0.16f)),
                ),
            )
            .border(1.dp, tints.first.copy(alpha = 0.26f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isNotEmpty()) {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (tokens.isDark) tints.first else scheme.primary,
            )
        } else {
            Icon(
                imageVector = AppIcons.Shield,
                contentDescription = null,
                tint = if (tokens.isDark) tints.first else scheme.primary,
                modifier = Modifier.size(size * 0.44f),
            )
        }
    }
}
