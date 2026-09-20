package com.verdor.musica.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private fun hashStr(s: String): Int {
    var h = 0
    for (c in s) h = 31 * h + c.code
    return abs(h)
}

private val hues = listOf(142, 152, 96, 168, 120, 84, 160)

/** Generates deterministic abstract cover art from a seed string —
 * same seed always renders the same "cover", but nothing is pulled
 * from the internet, so there's no copyright concern. */
@Composable
fun GeneratedCover(seed: String, modifier: Modifier = Modifier) {
    val h = hashStr(seed)
    val hue1 = hues[h % hues.size]
    val hue2 = (hue1 + 24 + (h % 20)) % 200

    val c1 = Color.hsv(hue1.toFloat(), 0.6f, 0.85f)
    val c2 = Color.hsv(hue2.toFloat(), 0.55f, 0.25f)

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
    ) {
        drawRect(brush = Brush.linearGradient(listOf(c1, c2)))
        val blobs = 2 + (h % 3)
        repeat(blobs) { i ->
            val bx = ((h shr (i * 3)) % 100) / 100f * size.width
            val by = ((h shr (i * 5)) % 100) / 100f * size.height
            val br = size.minDimension * (0.18f + ((h shr (i * 2)) % 30) / 100f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.hsv(((hue1 + i * 30) % 360).toFloat(), 0.8f, 0.9f, alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(bx, by),
                    radius = br
                ),
                radius = br,
                center = Offset(bx, by)
            )
        }
    }
}
