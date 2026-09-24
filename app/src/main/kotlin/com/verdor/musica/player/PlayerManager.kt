package com.verdor.musica.player

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps ExoPlayer for Jamendo streams + downloaded local files, with a
 * real Android Equalizer effect attached to the player's audio session.
 *
 * This instance now lives at the Application level (see VerdorApp) and is
 * shared with PlaybackService, so the same player that the UI controls is
 * also the one exposed through a MediaSession for background playback and
 * the system media notification.
 *
 * Deliberately does NOT touch YouTube playback: that runs in a separate
 * WebView (see YoutubeWebPlayer) whose audio the OS never exposes to an
 * app-level Equalizer, by design of the platform.
 */
class PlayerManager(context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ true
        )
        .setHandleAudioBecomingNoisy(true) // pause automatically on headphone unplug
        .build()

    private var equalizer: Equalizer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    /** Surfaced to the UI so a failed stream shows a real message instead
     * of just silently doing nothing (which is what made "no reproduce la
     * música" hard to diagnose before). */
    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError

    private var bassBand: Short = 0
    private var midBand: Short = 0
    private var trebleBand: Short = 0

    init {
        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }
            override fun onPlaybackStateChanged(state: Int) {
                _durationMs.value = exoPlayer.duration.coerceAtLeast(0)
            }
            override fun onPlayerError(error: PlaybackException) {
                _playbackError.value = error.errorCodeName + ": " + (error.message ?: "error desconocido")
            }
        })
        rebuildEqualizer()
    }

    private fun rebuildEqualizer() {
        try {
            equalizer?.release()
            equalizer = Equalizer(0, exoPlayer.audioSessionId).apply {
                enabled = true
                val bands = numberOfBands
                bassBand = 0
                trebleBand = (bands - 1).toShort()
                midBand = (bands / 2).toShort()
            }
        } catch (e: Exception) {
            equalizer = null
        }
    }

    fun playUrl(url: String, title: String = "", artist: String = "") {
        _playbackError.value = null
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .build()
            )
            .build()
        exoPlayer.setMediaItem(item)
        exoPlayer.prepare()
        exoPlayer.play()
        rebuildEqualizer()
    }

    fun playLocalFile(path: String, title: String = "", artist: String = "") {
        playUrl("file://$path", title, artist)
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun seekToFraction(fraction: Float) {
        val dur = exoPlayer.duration
        if (dur > 0) exoPlayer.seekTo((dur * fraction).toLong())
    }

    fun pollPosition() {
        _positionMs.value = exoPlayer.currentPosition.coerceAtLeast(0)
    }

    fun setBass(db: Float) = setBandLevel(bassBand, db)
    fun setMid(db: Float) = setBandLevel(midBand, db)
    fun setTreble(db: Float) = setBandLevel(trebleBand, db)

    private fun setBandLevel(band: Short, db: Float) {
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange
            val milliBels = (db * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
            eq.setBandLevel(band, milliBels.toShort())
        } catch (_: Exception) { }
    }

    fun release() {
        equalizer?.release()
        exoPlayer.release()
    }
}
