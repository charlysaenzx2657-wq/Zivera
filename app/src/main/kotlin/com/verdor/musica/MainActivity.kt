package com.verdor.musica

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.verdor.musica.player.YoutubeWebPlayer
import com.verdor.musica.ui.components.BottomNav
import com.verdor.musica.ui.components.MiniPlayerBar
import com.verdor.musica.ui.components.NavTab
import com.verdor.musica.ui.screens.*
import com.verdor.musica.ui.theme.VerdorMusicaTheme
import android.webkit.WebSettings
import android.annotation.SuppressLint
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VerdorMusicaTheme {
              Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
              ) {
                var screen by remember { mutableStateOf("inicio") }
                var playerOpen by remember { mutableStateOf(false) }

                val library by vm.library.collectAsStateWithLifecycle()
                val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
                val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
                val ytResults by vm.searchResultsYt.collectAsStateWithLifecycle()
                val jamendoResults by vm.searchResultsJamendo.collectAsStateWithLifecycle()
                val searchError by vm.searchError.collectAsStateWithLifecycle()
                val eqBass by vm.settings.eqBass.collectAsStateWithLifecycle(initialValue = 0f)
                val eqMid by vm.settings.eqMid.collectAsStateWithLifecycle(initialValue = 0f)
                val eqTreble by vm.settings.eqTreble.collectAsStateWithLifecycle(initialValue = 0f)

                var position by remember { mutableStateOf(0f) }
                var positionLabel by remember { mutableStateOf("0:00") }
                var durationLabel by remember { mutableStateOf("0:00") }

                // Hidden WebView hosting the YouTube IFrame player. Kept
                // off-screen (1dp) — its own player UI is never shown,
                // our Compose UI is the only visible player surface.
                var ytPlayer: YoutubeWebPlayer? by remember { mutableStateOf(null) }

                LaunchedEffect(Unit) {
                    vm.ytPlayerControl = { play -> if (play) ytPlayer?.play() else ytPlayer?.pause() }
                    vm.ytSeekControl = { frac -> ytPlayer?.seekTo(frac) }
                    vm.ytLoadControl = { videoId -> ytPlayer?.loadVideo(videoId) }
                }

                // Lightweight polling loop for progress bar (works for both engines)
                LaunchedEffect(nowPlaying, isPlaying) {
                    while (true) {
                        kotlinx.coroutines.delay(500)
                        vm.playerManager.pollPosition()
                        val dur = vm.playerManager.exoPlayer.duration.coerceAtLeast(0)
                        val pos = vm.playerManager.exoPlayer.currentPosition.coerceAtLeast(0)
                        if (nowPlaying?.source == Source.JAMENDO && dur > 0) {
                            position = pos.toFloat() / dur
                            positionLabel = formatMs(pos)
                            durationLabel = formatMs(dur)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            when (screen) {
                                "inicio" -> HomeScreen(library = library, onOpenTrack = { vm.playLocal(it); playerOpen = true })
                                "buscar" -> SearchScreen(
                                    ytResults = ytResults,
                                    jamendoResults = jamendoResults,
                                    searchError = searchError,
                                    onSearch = { src, q -> if (src == Source.YOUTUBE) vm.searchYoutube(q) else vm.searchJamendo(q) },
                                    onPlayYoutube = { vm.playYoutube(it); playerOpen = true },
                                    onPlayJamendo = { vm.playJamendoStream(it); playerOpen = true },
                                    onDownloadJamendo = { vm.downloadJamendoTrack(it) }
                                )
                                "biblioteca" -> LibraryScreen(
                                    tracks = library,
                                    onPlay = { vm.playLocal(it); playerOpen = true },
                                    onRemove = { vm.removeFromLibrary(it) }
                                )
                                "ajustes" -> SettingsScreen(settings = vm.settings, onClearLibrary = { vm.clearLibrary() })
                            }
                        }

                        if (nowPlaying != null) {
                            MiniPlayerBar(
                                title = nowPlaying!!.name,
                                artist = nowPlaying!!.artist,
                                coverSeed = nowPlaying!!.coverSeed,
                                isPlaying = isPlaying,
                                onTogglePlay = { vm.togglePlayPause() },
                                onOpen = { playerOpen = true }
                            )
                        }

                        BottomNav(
                            tabs = listOf(
                                NavTab("inicio", stringResource_(R.string.nav_home), Icons.Filled.Home),
                                NavTab("buscar", stringResource_(R.string.nav_search), Icons.Filled.Search),
                                NavTab("biblioteca", stringResource_(R.string.nav_library), Icons.Filled.LibraryMusic),
                                NavTab("ajustes", stringResource_(R.string.nav_settings), Icons.Filled.Settings)
                            ),
                            selected = screen,
                            onSelect = { screen = it }
                        )
                    }

                    // Hidden YouTube WebView host (1dp, off-screen visually but attached
                    // so playback keeps running)
                    Box(modifier = Modifier.size(1.dp)) {
                        AndroidView(factory = { ctx ->
                            WebView(ctx).apply {
                                // Force software rendering: without this, this WebView's
                                // hardware layer (SurfaceView) can punch through the whole
                                // window on some OEM skins (seen on Motorola devices),
                                // painting the entire screen black even though it's sized
                                // at 1dp and Compose is drawing everything else correctly
                                // underneath. This is the actual fix for the black screen.
                                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                settings.javaScriptEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                ytPlayer = YoutubeWebPlayer(this)
                            }
                        })
                    }

                    AnimatedVisibility(
                        visible = playerOpen,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        PlayerScreen(
                            nowPlaying = nowPlaying,
                            isPlaying = isPlaying,
                            positionFraction = position,
                            positionLabel = positionLabel,
                            durationLabel = durationLabel,
                            bass = eqBass, mid = eqMid, treble = eqTreble,
                            onEqChange = { b, m, t -> vm.setEq(b, m, t) },
                            onTogglePlay = { vm.togglePlayPause() },
                            onSeek = { frac ->
                                position = frac
                                if (nowPlaying?.source == Source.YOUTUBE) vm.ytSeekControl?.invoke(frac)
                                else vm.playerManager.seekToFraction(frac)
                            },
                            onClose = { playerOpen = false },
                            onDownload = { vm.downloadCurrentJamendo() },
                            showDownload = nowPlaying?.source == Source.JAMENDO && nowPlaying?.jamendoTrack != null
                        )
                    }
                }
              }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

// Small helper so Composables above can call stringResource outside a @Composable
// context-sensitive spot without extra imports noise.
@androidx.compose.runtime.Composable
private fun stringResource_(id: Int): String = androidx.compose.ui.res.stringResource(id)
