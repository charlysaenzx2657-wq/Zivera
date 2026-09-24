package com.verdor.musica.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.verdor.musica.MainActivity
import com.verdor.musica.VerdorApp

/**
 * Hosts a MediaSession wrapping the SAME ExoPlayer instance the rest of the
 * app already uses (via VerdorApp.playerManager — an application-scoped
 * singleton, not tied to any one screen's lifecycle). Because it's the
 * same player, this needs no extra "remote control" plumbing: the system
 * builds the notification and transport controls directly from real
 * playback state, and background/lock-screen playback keeps working
 * because the service (not just the Activity) is what's now holding a
 * reference to the player.
 */
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val playerManager = (application as VerdorApp).playerManager
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, playerManager.exoPlayer)
            .setSessionActivity(openAppIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /** Only stop the underlying player if nothing is actively playing —
     * this is what lets music keep going after the user leaves the app. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val session = mediaSession ?: return
        if (!session.player.playWhenReady || session.player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }
}
