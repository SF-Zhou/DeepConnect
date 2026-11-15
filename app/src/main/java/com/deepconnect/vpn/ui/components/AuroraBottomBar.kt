package com.deepconnect.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepconnect.vpn.ui.icons.AppIcons
import com.deepconnect.vpn.ui.theme.LocalAuroraColors

/** Top-level destinations shown in the bottom navigation. */
enum class AuroraTab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Home", AppIcons.Home),
    Logs("logs", "Logs", AppIcons.Terminal),
    Settings("settings", "Settings", AppIcons.Tune),
}

/** Floating glass pill navigation with an animated selected indicator. */
@Composable
fun AuroraBottomBar(
    selected: AuroraTab,
    onSelect: (AuroraTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAuroraColors.current
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.verticalGradient(listOf(tokens.glassStrongTop, tokens.glassStrongBottom)))
                .border(1.dp, tokens.navBorder, RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AuroraTab.entries.forEach { tab ->
                val isSelected = tab == selected
                val background by animateColorAsState(
                    targetValue = if (isSelected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                    animationSpec = tween(240),
                    label = "tabBackground",
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                    animationSpec = tween(240),
                    label = "tabContent",
                )
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.94f,
                    animationSpec = tween(240),
                    label = "tabScale",
                )
                val interaction = remember { MutableInteractionSource() }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(background)
                        .clickable(
                            interactionSource = interaction,
                            indication = ripple(
                                bounded = true,
                                color = scheme.primary.copy(alpha = 0.16f),
                            ),
                            onClick = { onSelect(tab) },
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
