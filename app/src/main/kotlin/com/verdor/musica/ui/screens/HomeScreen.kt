package com.verdor.musica.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdor.musica.R
import com.verdor.musica.data.TrackEntity
import com.verdor.musica.ui.components.GeneratedCover
import com.verdor.musica.ui.theme.GlassPanel
import com.verdor.musica.ui.theme.Ink
import com.verdor.musica.ui.theme.Muted
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable

@Composable
fun HomeScreen(
    library: List<TrackEntity>,
    onOpenTrack: (TrackEntity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(stringResource(R.string.your_music), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.downloaded_offline), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))

        if (library.isEmpty()) {
            Text(stringResource(R.string.empty_library), color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
        } else {
            library.take(6).forEach { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenTrack(track) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GeneratedCover(track.coverSeed, Modifier.size(46.dp))
                    Column {
                        Text(track.name, color = Ink, fontSize = 14.sp)
                        Text(track.artist, color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Ad slot: keep this to a banner/native format. A pop-under or
        // forced-redirect script here would fight the app's own
        // navigation and likely get the app flagged by Play Protect.
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Text("PUBLICIDAD", color = Muted, fontSize = 9.5.sp)
                Text(
                    "Espacio para banner/native ad",
                    color = Muted,
                    fontSize = 11.5.sp,
                    modifier = Modifier.align(androidx.compose.ui.Alignment.Center)
                )
            }
        }
    }
}
