package com.verdor.musica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdor.musica.R
import com.verdor.musica.data.FavoriteEntity
import com.verdor.musica.data.TrackEntity
import com.verdor.musica.network.JamendoTrack
import com.verdor.musica.ui.components.GeneratedCover
import com.verdor.musica.ui.theme.Green
import com.verdor.musica.ui.theme.GreenDim
import com.verdor.musica.ui.theme.Ink
import com.verdor.musica.ui.theme.Muted

@Composable
fun HomeScreen(
    library: List<TrackEntity>,
    favorites: List<FavoriteEntity>,
    discover: List<JamendoTrack>,
    isOffline: Boolean,
    onOpenTrack: (TrackEntity) -> Unit,
    onOpenFavorite: (FavoriteEntity) -> Unit,
    onPlayDiscover: (JamendoTrack) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(stringResource(R.string.your_music), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)

        if (isOffline) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Modo offline activado — mostrando solo tu música descargada.",
                color = Muted, fontSize = 12.sp
            )
        }

        // ---- Favorites shortcut ----
        if (favorites.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Tus favoritas", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites.take(10)) { fav ->
                    Column(
                        modifier = Modifier.width(96.dp).clickable { onOpenFavorite(fav) }
                    ) {
                        GeneratedCover(fav.coverSeed, Modifier.size(96.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(fav.name, color = Ink, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(fav.artist, color = Muted, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // ---- Downloaded library ----
        Spacer(Modifier.height(20.dp))
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

        // ---- Discover (needs internet) ----
        if (!isOffline) {
            Spacer(Modifier.height(20.dp))
            Text("Descubre", color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            if (discover.isEmpty()) {
                Text(
                    "Agrega tu Client ID de Jamendo para que aparezca música aquí.",
                    color = Muted, fontSize = 12.5.sp
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(discover) { track ->
                        Column(
                            modifier = Modifier.width(120.dp).clickable { onPlayDiscover(track) }
                        ) {
                            GeneratedCover(track.id, Modifier.size(120.dp))
                            Spacer(Modifier.height(6.dp))
                            Text(track.name, color = Ink, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist_name, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // Ad slot — banner/native format only, see README before wiring a script here
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(androidx.compose.ui.graphics.Color(0x8C16261E))
                .padding(12.dp)
        ) {
            Text("PUBLICIDAD", color = Muted, fontSize = 9.5.sp)
            Text(
                "Espacio para banner/native ad",
                color = Muted, fontSize = 11.5.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
