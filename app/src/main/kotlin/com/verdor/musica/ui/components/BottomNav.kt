package com.verdor.musica.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdor.musica.ui.theme.Green
import com.verdor.musica.ui.theme.Muted

data class NavTab(val key: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/** A gold/amber secondary accent so the app isn't 100% one green note —
 * used only here, sparingly, as the little glow behind the active tab. */
private val Amber = Color(0xFFF2B33D)

@Composable
fun BottomNav(
    tabs: List<NavTab>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 18.dp, shape = RoundedCornerShape(percent = 50), spotColor = Color.Black)
                .clip(RoundedCornerShape(percent = 50)) // fully rounded "pill" nav bar
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xF20A1410), Color(0xF2101C16), Color(0xF20A1410))
                    )
                )
                .padding(vertical = 10.dp, horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            tabs.forEach { tab ->
                val isActive = tab.key == selected
                val scale by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.9f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "navTabScale"
                )
                Box(
                    modifier = Modifier
                        .scale(scale)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (isActive) Brush.horizontalGradient(listOf(Green, Amber.copy(alpha = 0.35f)))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onSelect(tab.key) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            tab.icon,
                            contentDescription = tab.label,
                            tint = if (isActive) Color(0xFF05170D) else Muted,
                            modifier = Modifier.size(22.dp)
                        )
                        if (isActive) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tab.label, fontSize = 10.sp,
                                color = Color(0xFF05170D), fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
