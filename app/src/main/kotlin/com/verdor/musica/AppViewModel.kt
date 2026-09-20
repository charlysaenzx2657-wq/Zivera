package com.verdor.musica

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.verdor.musica.data.AppDatabase
import com.verdor.musica.data.TrackEntity
import com.verdor.musica.network.JamendoTrack
import com.verdor.musica.network.NetworkModule
import com.verdor.musica.network.YoutubeItem
import com.verdor.musica.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
    val playerManager = PlayerManager(application)

    private val dao = AppDatabase.get(application).trackDao()
    val library: StateFlow<List<TrackEntity>> =
        dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
        val key = settings.youtubeKey.stateIn(viewModelScope, SharingStarted.Eagerly, "").value
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
        val id = settings.jamendoClientId.stateIn(viewModelScope, SharingStarted.Eagerly, "").value
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
        playerManager.playUrl(track.audio)
        _isPlaying.value = true
    }

    fun playLocal(entity: TrackEntity) {
        ytPlayerControl?.invoke(false)
        _nowPlaying.value = NowPlaying(
            id = entity.id, name = entity.name, artist = entity.artist,
            coverSeed = entity.coverSeed, source = Source.JAMENDO
        )
        playerManager.playLocalFile(entity.filePath)
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
        app.downloadRepository.downloadJamendoTrack(track)
    }

    fun downloadJamendoTrack(track: JamendoTrack) = viewModelScope.launch {
        app.downloadRepository.downloadJamendoTrack(track)
    }

    fun removeFromLibrary(entity: TrackEntity) = viewModelScope.launch {
        app.downloadRepository.removeTrack(entity.id, entity.filePath)
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

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
