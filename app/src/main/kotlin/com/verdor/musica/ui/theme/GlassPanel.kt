package com.verdor.musica.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid-glass surface: a frosted, semi-transparent panel with a slow
 * diagonal sheen. Deliberately never fully opaque (alpha ~0.55) nor
 * fully transparent, and the animated part only moves a gradient
 * offset — never the blur radius itself — which is what keeps this
 * smooth on mid-range phones instead of janky.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    content: @Composable Box.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "sheen")
    val sheenOffset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sheenOffset"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0x8C16261E))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(cornerRadius))
            .blur(0.dp) // backdrop blur of content *behind* this isn't available pre-API33
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.09f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.04f)
                        ),
                        start = Offset(sheenOffset * 400f, 0f),
                        end = Offset(sheenOffset * 400f + 400f, 400f)
                    )
                )
        )
        content()
    }
}
