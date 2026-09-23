package com.verdor.musica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdor.musica.R
import com.verdor.musica.data.FavoriteEntity
import com.verdor.musica.data.TrackEntity
import com.verdor.musica.ui.components.GeneratedCover
import com.verdor.musica.ui.theme.Danger
import com.verdor.musica.ui.theme.Green
import com.verdor.musica.ui.theme.Ink
import com.verdor.musica.ui.theme.Muted

@Composable
fun LibraryScreen(
    tracks: List<TrackEntity>,
    favorites: List<FavoriteEntity>,
    onPlay: (TrackEntity) -> Unit,
    onRemove: (TrackEntity) -> Unit,
    onPlayFavorite: (FavoriteEntity) -> Unit,
    onRemoveFavorite: (FavoriteEntity) -> Unit
) {
    var tab by remember { mutableStateOf("descargadas") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.nav_library), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x8C16261E))
                .padding(4.dp)
        ) {
            listOf("descargadas" to "Descargadas", "favoritos" to "Favoritos").forEach { (key, label) ->
                val active = tab == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Green else Color.Transparent)
                        .clickable { tab = key }
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(label, color = if (active) Color(0xFF05170D) else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (tab == "descargadas") {
            if (tracks.isEmpty()) {
                Text(stringResource(R.string.empty_library), color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
            LazyColumn {
                items(tracks) { track ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GeneratedCover(track.coverSeed, Modifier.size(46.dp).clickable { onPlay(track) })
                        Column(Modifier.weight(1f).clickable { onPlay(track) }) {
                            Text(track.name, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onRemove(track) }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.remove_download), tint = Danger)
                        }
                    }
                }
            }
        } else {
            if (favorites.isEmpty()) {
                Text("Nada marcado como favorito todavía. Toca el corazón en el reproductor.", color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
            LazyColumn {
                items(favorites) { fav ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GeneratedCover(fav.coverSeed, Modifier.size(46.dp).clickable { onPlayFavorite(fav) })
                        Column(Modifier.weight(1f).clickable { onPlayFavorite(fav) }) {
                            Text(fav.name, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                fav.artist + if (fav.source == "youtube") " · YouTube" else " · Jamendo",
                                color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { onRemoveFavorite(fav) }) {
                            Icon(Icons.Filled.FavoriteBorder, contentDescription = "Quitar de favoritos", tint = Danger)
                        }
                    }
                }
            }
        }
    }
}
