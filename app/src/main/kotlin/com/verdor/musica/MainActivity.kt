package com.verdor.musica

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.verdor.musica.player.PlaybackService
import com.verdor.musica.player.YoutubeWebPlayer
import com.verdor.musica.ui.components.BottomNav
import com.verdor.musica.ui.components.MiniPlayerBar
import com.verdor.musica.ui.components.NavTab
import com.verdor.musica.ui.screens.*
import com.verdor.musica.ui.theme.VerdorMusicaTheme
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()
    private lateinit var googleClient: GoogleSignInClient

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Background playback + system notification: this is the service
        // that keeps the (shared, app-scoped) player alive and shows the
        // media notification even after the user leaves the app.
        startService(Intent(this, PlaybackService::class.java))

        // Android 13+ requires this permission at runtime for the media
        // notification to actually be allowed to show.
        val notifPermLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        googleClient = GoogleSignIn.getClient(
            this,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
        )

        setContent {
            VerdorMusicaTheme {
              Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
              ) {
                val context = LocalContext.current
                var screen by remember { mutableStateOf("inicio") }
                var playerOpen by remember { mutableStateOf(false) }

                val library by vm.library.collectAsStateWithLifecycle()
                val favorites by vm.favorites.collectAsStateWithLifecycle()
                val discover by vm.discover.collectAsStateWithLifecycle()
                val nowPlaying by vm.nowPlaying.collectAsStateWithLifecycle()
                val isPlaying by vm.isPlaying.collectAsStateWithLifecycle()
                val ytResults by vm.searchResultsYt.collectAsStateWithLifecycle()
                val jamendoResults by vm.searchResultsJamendo.collectAsStateWithLifecycle()
                val searchError by vm.searchError.collectAsStateWithLifecycle()
                val currentUser by vm.currentUser.collectAsStateWithLifecycle()
                val authBusy by vm.authBusy.collectAsStateWithLifecycle()
                val authError by vm.authError.collectAsStateWithLifecycle()
                val isOnline by vm.isOnline.collectAsStateWithLifecycle()
                val offlineManual by vm.offlineModeManual.collectAsStateWithLifecycle()
                val effectiveOffline by vm.effectiveOffline.collectAsStateWithLifecycle()
                val playbackError by vm.playbackError.collectAsStateWithLifecycle()
                val eqBass by vm.settings.eqBass.collectAsStateWithLifecycle(initialValue = 0f)
                val eqMid by vm.settings.eqMid.collectAsStateWithLifecycle(initialValue = 0f)
                val eqTreble by vm.settings.eqTreble.collectAsStateWithLifecycle(initialValue = 0f)

                var position by remember { mutableStateOf(0f) }
                var positionLabel by remember { mutableStateOf("0:00") }
                var durationLabel by remember { mutableStateOf("0:00") }

                var ytPlayer: YoutubeWebPlayer? by remember { mutableStateOf(null) }

                val googleLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        account.idToken?.let { vm.signInWithGoogle(it) }
                    } catch (e: ApiException) {
                        Toast.makeText(context, "No se pudo iniciar sesión con Google", Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(Unit) {
                    vm.ytPlayerControl = { play -> if (play) ytPlayer?.play() else ytPlayer?.pause() }
                    vm.ytSeekControl = { frac -> ytPlayer?.seekTo(frac) }
                    vm.ytLoadControl = { videoId -> ytPlayer?.loadVideo(videoId) }
                    vm.loadDiscover()
                }

                // Reload "Descubre" once connectivity comes back
                LaunchedEffect(isOnline) {
                    if (isOnline) vm.loadDiscover()
                }

                LaunchedEffect(playbackError) {
                    playbackError?.let {
                        Toast.makeText(context, "No se pudo reproducir: $it", Toast.LENGTH_LONG).show()
                    }
                }

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
                            Crossfade(targetState = screen, label = "screen-switch") { s ->
                            when (s) {
                                "inicio" -> HomeScreen(
                                    library = library,
                                    favorites = favorites,
                                    discover = discover,
                                    isOffline = effectiveOffline,
                                    onOpenTrack = { vm.playLocal(it); playerOpen = true },
                                    onOpenFavorite = { vm.playFavorite(it); playerOpen = true },
                                    onPlayDiscover = { vm.playJamendoStream(it); playerOpen = true }
                                )
                                "buscar" -> SearchScreen(
                                    ytResults = ytResults,
                                    jamendoResults = jamendoResults,
                                    searchError = searchError,
                                    onSearch = { src, q -> if (src == Source.YOUTUBE) vm.searchYoutube(q) else vm.searchJamendo(q) },
                                    onPlayYoutube = { vm.playYoutube(it); playerOpen = true },
                                    onPlayJamendo = { vm.playJamendoStream(it); playerOpen = true },
                                    onDownloadJamendo = { vm.downloadJamendoTrack(it) },
                                    isOffline = effectiveOffline
                                )
                                "biblioteca" -> LibraryScreen(
                                    tracks = library,
                                    favorites = favorites,
                                    onPlay = { vm.playLocal(it); playerOpen = true },
                                    onRemove = { vm.removeFromLibrary(it) },
                                    onPlayFavorite = { vm.playFavorite(it); playerOpen = true },
                                    onRemoveFavorite = { vm.removeFavorite(it.id) }
                                )
                                "ajustes" -> SettingsScreen(
                                    settings = vm.settings,
                                    onClearLibrary = { vm.clearLibrary() },
                                    userEmail = currentUser?.email,
                                    authBusy = authBusy,
                                    authError = authError,
                                    onSignIn = { e, p -> vm.signIn(e, p) },
                                    onSignUp = { e, p -> vm.signUp(e, p) },
                                    onSignOut = { vm.signOut() },
                                    onSignInWithGoogle = { googleLauncher.launch(googleClient.signInIntent) },
                                    offlineModeManual = offlineManual,
                                    onSetOfflineMode = { vm.setOfflineModeManual(it) },
                                    isOnline = isOnline
                                )
                            }
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
                    // so playback keeps running). Note: this one is NOT affected by
                    // offline mode UI — if the user taps a YouTube favorite while
                    // offline it will simply fail to load, same as any browser would.
                    Box(modifier = Modifier.size(1.dp)) {
                        AndroidView(factory = { ctx ->
                            WebView(ctx).apply {
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
                            showDownload = nowPlaying?.source == Source.JAMENDO && nowPlaying?.jamendoTrack != null,
                            isFavorite = nowPlaying?.let { np -> favorites.any { it.id == np.id } } ?: false,
                            onToggleFavorite = { vm.toggleFavoriteCurrent() }
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

@androidx.compose.runtime.Composable
private fun stringResource_(id: Int): String = androidx.compose.ui.res.stringResource(id)
