package com.verdor.musica.player

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Wraps ExoPlayer for Jamendo streams + downloaded local files, with a
 * real Android Equalizer effect attached to the player's audio session.
 * This is genuine DSP on the actual audio output — not decorative.
 *
 * Deliberately does NOT touch YouTube playback: that runs in a separate
 * WebView (see YoutubeWebPlayer) whose audio the OS never exposes to an
 * app-level Equalizer, by design of the platform.
 */
class PlayerManager(context: Context) {

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()
    private var equalizer: Equalizer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs

    // Equalizer bands, roughly mapped: band 0 ~ bass, middle band(s) ~ mid, last band ~ treble
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
        })
        try {
            equalizer = Equalizer(0, exoPlayer.audioSessionId).apply {
                enabled = true
                val bands = numberOfBands
                bassBand = 0
                trebleBand = (bands - 1).toShort()
                midBand = (bands / 2).toShort()
            }
        } catch (e: Exception) {
            equalizer = null // some devices/emulators lack this effect; app still plays audio fine
        }
    }

    fun playUrl(url: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(url))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun playLocalFile(path: String) {
        playUrl("file://$path")
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

    /** dB range roughly -12..+12, matching the web app's sliders */
    fun setBass(db: Float) = setBandLevel(bassBand, db)
    fun setMid(db: Float) = setBandLevel(midBand, db)
    fun setTreble(db: Float) = setBandLevel(trebleBand, db)

    private fun setBandLevel(band: Short, db: Float) {
        val eq = equalizer ?: return
        try {
            val range = eq.bandLevelRange // in millibels, e.g. [-1500, 1500]
            val milliBels = (db * 100).toInt().coerceIn(range[0].toInt(), range[1].toInt())
            eq.setBandLevel(band, milliBels.toShort())
        } catch (_: Exception) { /* ignore unsupported device effects */ }
    }

    fun release() {
        equalizer?.release()
        exoPlayer.release()
    }
}
