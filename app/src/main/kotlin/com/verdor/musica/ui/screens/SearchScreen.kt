package com.verdor.musica.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdor.musica.R
import com.verdor.musica.Source
import com.verdor.musica.network.JamendoTrack
import com.verdor.musica.network.YoutubeItem
import com.verdor.musica.ui.components.GeneratedCover
import com.verdor.musica.ui.theme.Green
import com.verdor.musica.ui.theme.Ink
import com.verdor.musica.ui.theme.Muted

@Composable
fun SearchScreen(
    ytResults: List<YoutubeItem>,
    jamendoResults: List<JamendoTrack>,
    searchError: String?,
    onSearch: (Source, String) -> Unit,
    onPlayYoutube: (YoutubeItem) -> Unit,
    onPlayJamendo: (JamendoTrack) -> Unit,
    onDownloadJamendo: (JamendoTrack) -> Unit,
    isOffline: Boolean
) {
    var source by remember { mutableStateOf(Source.YOUTUBE) }
    var query by remember { mutableStateOf("") }

    if (isOffline) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Sin conexión", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "El modo offline está activo. Buscar necesita internet — puedes seguir escuchando lo que ya descargaste desde Biblioteca.",
                color = Muted, fontSize = 13.sp, lineHeight = 19.sp
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.nav_search), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))

        // Source toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(androidx.compose.ui.graphics.Color(0x8C16261E))
                .padding(4.dp)
        ) {
            listOf(Source.YOUTUBE to stringResource(R.string.source_youtube), Source.JAMENDO to stringResource(R.string.source_jamendo)).forEach { (src, label) ->
                val active = src == source
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) Green else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { source = src }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (active) androidx.compose.ui.graphics.Color(0xFF05170D) else Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (source == Source.YOUTUBE) stringResource(R.string.note_youtube) else stringResource(R.string.note_jamendo),
            color = Muted, fontSize = 11.5.sp
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Muted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (query.isNotBlank()) onSearch(source, query) }),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        when {
            searchError == "NO_KEY_YT" -> Text(stringResource(R.string.need_key_youtube), color = Muted, fontSize = 13.sp)
            searchError == "NO_KEY_JAMENDO" -> Text(stringResource(R.string.need_key_jamendo), color = Muted, fontSize = 13.sp)
            searchError != null -> Text("${stringResource(R.string.no_results)} ($searchError)", color = Muted, fontSize = 13.sp)
        }

        LazyColumn {
            if (source == Source.YOUTUBE) {
                items(ytResults) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlayYoutube(item) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GeneratedCover(item.id.videoId, Modifier.size(46.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.snippet.title, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.snippet.channelTitle, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            } else {
                items(jamendoResults) { track ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GeneratedCover(track.id, Modifier.size(46.dp).clickable { onPlayJamendo(track) })
                        Column(Modifier.weight(1f).clickable { onPlayJamendo(track) }) {
                            Text(track.name, color = Ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist_name, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (track.audiodownload_allowed) {
                            IconButton(onClick = { onDownloadJamendo(track) }) {
                                Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.download), tint = Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}
