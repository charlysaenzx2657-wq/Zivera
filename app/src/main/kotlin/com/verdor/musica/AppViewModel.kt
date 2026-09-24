package com.verdor.musica

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.verdor.musica.data.AppDatabase
import com.verdor.musica.data.AuthRepository
import com.verdor.musica.data.FavoriteEntity
import com.verdor.musica.data.SyncRepository
import com.verdor.musica.data.TrackEntity
import com.verdor.musica.network.JamendoTrack
import com.verdor.musica.network.NetworkModule
import com.verdor.musica.network.YoutubeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Source { YOUTUBE, JAMENDO }

data class NowPlaying(
    val id: String,
    val name: String,
    val artist: String,
    val coverSeed: String,
    val source: Source,
    val jamendoTrack: JamendoTrack? = null // kept to allow "download this" from the player
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as VerdorApp
    val settings = app.settingsStore
    // Shared with PlaybackService — NOT created here anymore, so playback
    // survives this ViewModel being cleared (e.g. Activity recreated).
    val playerManager = app.playerManager
    val playbackError: StateFlow<String?> = playerManager.playbackError

    private val dao = AppDatabase.get(application).trackDao()
    val library: StateFlow<List<TrackEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val favoriteDao = AppDatabase.get(application).favoriteDao()
    val favorites: StateFlow<List<FavoriteEntity>> =
        favoriteDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---- Account (Firebase Auth) + cloud sync (Firestore) ----
    private val authRepo = app.authRepository
    private val syncRepo = app.syncRepository

    val currentUser: StateFlow<FirebaseUser?> =
        authRepo.currentUser.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError
    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy

    fun signUp(email: String, password: String) = viewModelScope.launch {
        _authBusy.value = true; _authError.value = null
        authRepo.signUp(email, password)
            .onSuccess { pullCloudData() }
            .onFailure { _authError.value = it.message }
        _authBusy.value = false
    }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _authBusy.value = true; _authError.value = null
        authRepo.signIn(email, password)
            .onSuccess { pullCloudData() }
            .onFailure { _authError.value = it.message }
        _authBusy.value = false
    }

    fun signOut() = authRepo.signOut()

    fun signInWithGoogle(idToken: String) = viewModelScope.launch {
        _authBusy.value = true; _authError.value = null
        authRepo.signInWithGoogleIdToken(idToken)
            .onSuccess { pullCloudData() }
            .onFailure { _authError.value = it.message }
        _authBusy.value = false
    }

    /** Called right after login/signup: merges whatever's already saved in
     * the cloud into this device's local Room tables (doesn't touch the
     * actual downloaded audio files, only favorites + "you downloaded
     * this on another device" metadata). */
    private fun pullCloudData() = viewModelScope.launch {
        val uid = authRepo.uidOrNull() ?: return@launch
        runCatching {
            syncRepo.pullFavorites(uid).forEach { favoriteDao.insert(it) }
        }
    }

    fun isFavorite(id: String): Boolean = favorites.value.any { it.id == id }

    fun toggleFavoriteCurrent() {
        val np = _nowPlaying.value ?: return
        viewModelScope.launch {
            if (isFavorite(np.id)) {
                favoriteDao.deleteById(np.id)
                authRepo.uidOrNull()?.let { uid -> runCatching { syncRepo.deleteFavorite(uid, np.id) } }
            } else {
                val fav = FavoriteEntity(
                    id = np.id,
                    name = np.name,
                    artist = np.artist,
                    coverSeed = np.coverSeed,
                    source = if (np.source == Source.YOUTUBE) "youtube" else "jamendo",
                    addedAt = System.currentTimeMillis()
                )
                favoriteDao.insert(fav)
                authRepo.uidOrNull()?.let { uid -> runCatching { syncRepo.pushFavorite(uid, fav) } }
            }
        }
    }

    fun removeFavorite(id: String) = viewModelScope.launch {
        favoriteDao.deleteById(id)
        authRepo.uidOrNull()?.let { uid -> runCatching { syncRepo.deleteFavorite(uid, id) } }
    }

    /** Replays a favorited track. YouTube favorites carry enough metadata to
     * re-stream directly (id + title + channel is all playback needs).
     * Jamendo favorites only replay if they were also downloaded — we don't
     * keep a permanent streaming URL for undownloaded Jamendo favorites. */
    fun playFavorite(fav: FavoriteEntity) {
        if (fav.source == "youtube") {
            playYoutube(
                com.verdor.musica.network.YoutubeItem(
                    id = com.verdor.musica.network.YoutubeId(fav.id),
                    snippet = com.verdor.musica.network.YoutubeSnippet(
                        title = fav.name, channelTitle = fav.artist,
                        thumbnails = com.verdor.musica.network.YoutubeThumbnails(null, null)
                    )
                )
            )
        } else {
            val local = library.value.find { it.id == fav.id }
            if (local != null) playLocal(local)
            // else: not downloaded — nothing to stream from, UI should hint this
        }
    }

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _searchResultsYt = MutableStateFlow<List<YoutubeItem>>(emptyList())
    val searchResultsYt: StateFlow<List<YoutubeItem>> = _searchResultsYt

    private val _searchResultsJamendo = MutableStateFlow<List<JamendoTrack>>(emptyList())
    val searchResultsJamendo: StateFlow<List<JamendoTrack>> = _searchResultsJamendo

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    var ytPlayerControl: ((play: Boolean) -> Unit)? = null
    var ytSeekControl: ((Float) -> Unit)? = null
    var ytLoadControl: ((String) -> Unit)? = null

    fun searchYoutube(query: String) = viewModelScope.launch {
        _searchError.value = null
        // .first() actually suspends until the real stored/default value
        // arrives — the previous stateIn(...).value read the initial ""
        // before DataStore had a chance to emit, which is why it always
        // said "no key" even when one was configured.
        val key = settings.youtubeKey.first()
        if (key.isBlank()) { _searchError.value = "NO_KEY_YT"; return@launch }
        try {
            val res = NetworkModule.youtubeApi.search(query = query, key = key)
            _searchResultsYt.value = res.items
        } catch (e: Exception) {
            _searchError.value = e.message
        }
    }

    fun searchJamendo(query: String) = viewModelScope.launch {
        _searchError.value = null
        val id = settings.jamendoClientId.first()
        if (id.isBlank()) { _searchError.value = "NO_KEY_JAMENDO"; return@launch }
        try {
            val res = NetworkModule.jamendoApi.search(clientId = id, query = query)
            _searchResultsJamendo.value = res.results
        } catch (e: Exception) {
            _searchError.value = e.message
        }
    }

    fun playYoutube(item: YoutubeItem) {
        playerManager.exoPlayer.pause()
        _nowPlaying.value = NowPlaying(
            id = item.id.videoId, name = item.snippet.title, artist = item.snippet.channelTitle,
            coverSeed = item.id.videoId, source = Source.YOUTUBE
        )
        ytLoadControl?.invoke(item.id.videoId)
        _isPlaying.value = true
    }

    fun playJamendoStream(track: JamendoTrack) {
        ytPlayerControl?.invoke(false)
        _nowPlaying.value = NowPlaying(
            id = "jamendo_${track.id}", name = track.name, artist = track.artist_name,
            coverSeed = track.id, source = Source.JAMENDO, jamendoTrack = track
        )
        playerManager.playUrl(track.audio, track.name, track.artist_name)
        _isPlaying.value = true
    }

    fun playLocal(entity: TrackEntity) {
        ytPlayerControl?.invoke(false)
        _nowPlaying.value = NowPlaying(
            id = entity.id, name = entity.name, artist = entity.artist,
            coverSeed = entity.coverSeed, source = Source.JAMENDO
        )
        playerManager.playLocalFile(entity.filePath, entity.name, entity.artist)
        _isPlaying.value = true
    }

    fun togglePlayPause() {
        val np = _nowPlaying.value ?: return
        if (np.source == Source.YOUTUBE) {
            _isPlaying.value = !_isPlaying.value
            ytPlayerControl?.invoke(_isPlaying.value)
        } else {
            playerManager.togglePlayPause()
            _isPlaying.value = playerManager.exoPlayer.isPlaying
        }
    }

    fun downloadCurrentJamendo() = viewModelScope.launch {
        val track = _nowPlaying.value?.jamendoTrack ?: return@launch
        val entity = app.downloadRepository.downloadJamendoTrack(track)
        authRepo.uidOrNull()?.let { uid -> runCatching { syncRepo.pushDownloadMeta(uid, entity) } }
    }

    fun downloadJamendoTrack(track: JamendoTrack) = viewModelScope.launch {
        val entity = app.downloadRepository.downloadJamendoTrack(track)
        authRepo.uidOrNull()?.let { uid -> runCatching { syncRepo.pushDownloadMeta(uid, entity) } }
    }

    fun removeFromLibrary(entity: TrackEntity) = viewModelScope.launch {
        app.downloadRepository.removeTrack(entity.id, entity.filePath)
        authRepo.uidOrNull()?.let { uid -> runCatching { syncRepo.deleteDownloadMeta(uid, entity.id) } }
    }

    fun clearLibrary() = viewModelScope.launch {
        app.downloadRepository.clearAll()
    }

    fun setEq(bass: Float, mid: Float, treble: Float) {
        playerManager.setBass(bass)
        playerManager.setMid(mid)
        playerManager.setTreble(treble)
        viewModelScope.launch {
            settings.setEqBass(bass); settings.setEqMid(mid); settings.setEqTreble(treble)
        }
    }

    // ---- Connectivity + offline mode ----
    private val connectivityObserver = com.verdor.musica.data.ConnectivityObserver(application)
    val isOnline: StateFlow<Boolean> =
        connectivityObserver.isOnline.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val offlineModeManual: StateFlow<Boolean> =
        settings.offlineModeManual.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** True when the app should behave as offline: either there's really no
     * connection, or the user explicitly switched on "Modo offline" in
     * Ajustes (e.g. to save data even with internet available). */
    val effectiveOffline: StateFlow<Boolean> = kotlinx.coroutines.flow.combine(isOnline, offlineModeManual) { online, manual ->
        !online || manual
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setOfflineModeManual(enabled: Boolean) = viewModelScope.launch {
        settings.setOfflineModeManual(enabled)
    }

    // ---- Home "Descubre" (random/popular Jamendo tracks, so Home isn't empty) ----
    private val _discover = MutableStateFlow<List<JamendoTrack>>(emptyList())
    val discover: StateFlow<List<JamendoTrack>> = _discover

    fun loadDiscover() = viewModelScope.launch {
        if (effectiveOffline.value) return@launch
        val id = settings.jamendoClientId.first()
        if (id.isBlank()) return@launch
        runCatching {
            NetworkModule.jamendoApi.discover(clientId = id)
        }.onSuccess { _discover.value = it.results }
    }

    override fun onCleared() {
        super.onCleared()
        // playerManager is app-scoped now (lives in VerdorApp / PlaybackService)
        // — do NOT release it here, or background playback would stop the
        // moment this screen's ViewModel gets cleared.
    }
}
