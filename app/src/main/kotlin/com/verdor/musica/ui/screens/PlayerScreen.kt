package com.verdor.musica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdor.musica.NowPlaying
import com.verdor.musica.R
import com.verdor.musica.Source
import com.verdor.musica.ui.components.GeneratedCover
import com.verdor.musica.ui.theme.Green
import com.verdor.musica.ui.theme.Ink
import com.verdor.musica.ui.theme.Muted

@Composable
fun PlayerScreen(
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    positionFraction: Float,
    positionLabel: String,
    durationLabel: String,
    bass: Float, mid: Float, treble: Float,
    onEqChange: (Float, Float, Float) -> Unit,
    onTogglePlay: () -> Unit,
    onSeek: (Float) -> Unit,
    onClose: () -> Unit,
    onDownload: () -> Unit,
    showDownload: Boolean
) {
    val isYoutube = nowPlaying?.source == Source.YOUTUBE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1410))
            .padding(horizontal = 26.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Ink) }
            Text(
                if (isYoutube) "YOUTUBE · STREAMING" else "JAMENDO",
                color = Muted, fontSize = 10.5.sp
            )
            Spacer(Modifier.width(24.dp))
        }

        Spacer(Modifier.weight(1f))

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            GeneratedCover(nowPlaying?.coverSeed ?: "empty", Modifier.size(230.dp))
        }

        Spacer(Modifier.height(18.dp))
        Text(
            nowPlaying?.name ?: stringResource(R.string.nothing_playing),
            color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
            nowPlaying?.artist ?: "—", color = Muted, fontSize = 13.5.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(18.dp))
        Slider(
            value = positionFraction.coerceIn(0f, 1f),
            onValueChange = onSeek,
            colors = SliderDefaults.colors(thumbColor = Green, activeTrackColor = Green, inactiveTrackColor = Color(0xFF1D2D24))
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(positionLabel, color = Muted, fontSize = 11.sp)
            Text(durationLabel, color = Muted, fontSize = 11.sp)
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDownload) {
                IconButton(onClick = onDownload) { Icon(Icons.Filled.Download, contentDescription = null, tint = Ink) }
                Spacer(Modifier.width(12.dp))
            }
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(50)).background(Green)
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF05170D)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // Equalizer — real DSP for Jamendo/local; visibly disabled for YouTube
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x8C16261E))
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.equalizer), color = Ink, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            if (isYoutube) {
                Text(stringResource(R.string.eq_disabled_note), color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                EqColumn(stringResource(R.string.bass), bass, isYoutube) { onEqChange(it, mid, treble) }
                EqColumn(stringResource(R.string.mid), mid, isYoutube) { onEqChange(bass, it, treble) }
                EqColumn(stringResource(R.string.treble), treble, isYoutube) { onEqChange(bass, mid, it) }
            }
        }
    }
}

@Composable
private fun EqColumn(label: String, value: Float, disabled: Boolean, onChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.height(90.dp).width(36.dp), contentAlignment = Alignment.Center) {
            Slider(
                value = value,
                onValueChange = { if (!disabled) onChange(it) },
                valueRange = -12f..12f,
                enabled = !disabled,
                modifier = Modifier
                    .width(90.dp)
                    .rotate(-90f)
            )
        }
        Text(label, color = Muted, fontSize = 10.5.sp)
    }
}
