package com.deepconnect.vpn.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.deepconnect.vpn.ui.ConnectionPhase
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.phase
import com.deepconnect.vpn.ui.theme.LocalAuroraColors
import com.deepconnect.vpn.vpn.VpnStatus
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The connection hero orb.
 *
 * Hand-drawn with radial gradients instead of blur so it is cheap enough for API 26 and
 * scales crisply. Rings slowly rotate; the glow pulses; a sweeping arc marks a busy
 * handshake; the centre holds the state icon.
 */
@Composable
fun ConnectionOrb(
    status: VpnStatus,
    modifier: Modifier = Modifier,
    size: Dp = 236.dp,
) {
    val tokens = LocalAuroraColors.current
    val phase = status.phase()
    val accent = tokens.status.of(status)
    val coreTop = if (tokens.isDark) {
        lerp(accent, Color.White, 0.12f)
    } else {
        lerp(accent, Color.White, 0.20f)
    }
    val coreBottom = if (tokens.isDark) {
        lerp(accent, Color.Black, 0.44f)
    } else {
        lerp(accent, Color.Black, 0.22f)
    }
    val iconTint = tokens.onAccent
    val busy = phase == ConnectionPhase.Connecting || phase == ConnectionPhase.Disconnecting

    val transition = rememberInfiniteTransition(label = "orb")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
        ),
        label = "orbSpin",
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_400, easing = LinearEasing),
        ),
        label = "orbSweep",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbPulse",
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = this.size.minDimension / 2f
            val coreRadius = radius * 0.60f
            val orbitRadius = radius * 0.875f

            // Outer halo — two blended radial discs, gently breathing.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.34f * pulse),
                        accent.copy(alpha = 0.12f * pulse),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )

            // Static concentric rings.
            listOf(0.985f, 0.905f, 0.825f).forEach { fraction ->
                drawCircle(
                    color = accent.copy(alpha = 0.10f),
                    radius = radius * fraction,
                    center = center,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }

            // Slow rotating dashed ring.
            rotate(degrees = spin, pivot = center) {
                drawCircle(
                    color = accent.copy(alpha = 0.38f),
                    radius = orbitRadius,
                    center = center,
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 15f)),
                    ),
                )
            }

            // Orbiting particle on the ring.
            val radians = (spin * PI / 180.0)
            drawCircle(
                color = accent.copy(alpha = 0.85f),
                radius = 2.1.dp.toPx(),
                center = Offset(
                    center.x + (cos(radians) * orbitRadius).toFloat(),
                    center.y + (sin(radians) * orbitRadius).toFloat(),
                ),
            )

            if (busy) {
                rotate(degrees = sweep, pivot = center) {
                    drawArc(
                        color = accent,
                        startAngle = -90f,
                        sweepAngle = 88f,
                        useCenter = false,
                        topLeft = Offset(center.x - orbitRadius, center.y - orbitRadius),
                        size = Size(orbitRadius * 2f, orbitRadius * 2f),
                        style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }

            // Core sphere.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coreTop, coreBottom),
                    center = Offset(center.x - coreRadius * 0.34f, center.y - coreRadius * 0.40f),
                    radius = coreRadius * 1.55f,
                ),
                radius = coreRadius,
                center = center,
            )
            drawCircle(
                color = accent.copy(alpha = 0.65f),
                radius = coreRadius,
                center = center,
                style = Stroke(width = 1.2.dp.toPx()),
            )
            // Specular highlight along the upper edge of the sphere.
            drawArc(
                color = Color.White.copy(alpha = if (tokens.isDark) 0.26f else 0.42f),
                startAngle = 196f,
                sweepAngle = 92f,
                useCenter = false,
                topLeft = Offset(center.x - coreRadius * 0.86f, center.y - coreRadius * 0.86f),
                size = Size(coreRadius * 1.72f, coreRadius * 1.72f),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round),
            )
        }

        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                (fadeIn(tween(260)) + scaleIn(initialScale = 0.82f, animationSpec = tween(320)))
                    .togetherWith(fadeOut(tween(160)))
            },
            label = "orbIcon",
        ) { current ->
            Icon(
                imageVector = current.icon(),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size * 0.27f),
            )
        }
    }
}

private fun ConnectionPhase.icon(): ImageVector = when (this) {
    ConnectionPhase.Disconnected -> AppIcons.Power
    ConnectionPhase.Connecting -> AppIcons.Shield
    ConnectionPhase.Connected -> AppIcons.ShieldCheck
    ConnectionPhase.Disconnecting -> AppIcons.Power
    ConnectionPhase.Failed -> AppIcons.AlertTriangle
}
