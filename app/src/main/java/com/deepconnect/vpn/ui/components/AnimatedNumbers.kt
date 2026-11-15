package com.deepconnect.vpn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Monospace statistic whose value eases between updates instead of jumping.
 *
 * The animation runs on a Float because traffic counters fit comfortably in a float and
 * that lets us keep a single animation spec for both directions.
 */
@Composable
fun AnimatedStatText(
    value: Long,
    format: (Long) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMillis = 620),
        label = "statValue",
    )
    Text(
        text = format(animated.toLong().coerceAtLeast(0L)),
        modifier = modifier,
        style = style.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            fontFeatureSettings = "tnum",
        ),
        color = color,
        maxLines = 1,
    )
}
