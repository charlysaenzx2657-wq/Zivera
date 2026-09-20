package com.verdor.musica.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.verdor.musica.R
import com.verdor.musica.data.TrackEntity
import com.verdor.musica.ui.components.GeneratedCover
import com.verdor.musica.ui.theme.Danger
import com.verdor.musica.ui.theme.Ink
import com.verdor.musica.ui.theme.Muted

@Composable
fun LibraryScreen(
    tracks: List<TrackEntity>,
    onPlay: (TrackEntity) -> Unit,
    onRemove: (TrackEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(stringResource(R.string.nav_library), color = Ink, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.downloaded_offline), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))

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
    }
}
