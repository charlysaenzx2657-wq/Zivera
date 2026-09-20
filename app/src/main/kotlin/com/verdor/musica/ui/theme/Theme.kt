package com.verdor.musica.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VerdorColorScheme = darkColorScheme(
    background = Bg,
    surface = Surface,
    primary = Green,
    onPrimary = Bg,
    onBackground = Ink,
    onSurface = Ink,
    secondary = GreenDim
)

@Composable
fun VerdorMusicaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VerdorColorScheme,
        typography = VerdorTypography,
        content = content
    )
}
