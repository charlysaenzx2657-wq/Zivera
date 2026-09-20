package com.verdor.musica.player

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Loads YouTube's own IFrame Player inside a WebView. This is the
 * officially supported way to embed YouTube playback on Android since
 * Google deprecated the native YouTube Android Player API. The audio
 * plays inside YouTube's player and is never accessible to our code —
 * that's intentional and is why the equalizer can't touch it.
 */
class YoutubeWebPlayer(private val webView: WebView) {

    fun loadVideo(videoId: String) {
        val html = """
            <!DOCTYPE html><html><body style="margin:0;background:#000">
            <div id="player"></div>
            <script src="https://www.youtube.com/iframe_api"></script>
            <script>
              var player;
              function onYouTubeIframeAPIReady() {
                player = new YT.Player('player', {
                  height: '220', width: '220', videoId: '$videoId',
                  playerVars: { autoplay: 1, playsinline: 1, controls: 0 },
                  events: {
                    onStateChange: function(e){ Android.onStateChange(e.data); },
                    onReady: function(){ Android.onReady(); }
                  }
                });
              }
              function ytPlay(){ if(player) player.playVideo(); }
              function ytPause(){ if(player) player.pauseVideo(); }
              function ytSeek(sec){ if(player) player.seekTo(sec, true); }
              function ytGetCurrentTime(){ return player ? player.getCurrentTime() : 0; }
              function ytGetDuration(){ return player ? player.getDuration() : 0; }
            </script>
            </body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL("https://www.youtube.com", html, "text/html", "utf-8", null)
    }

    fun play() = webView.evaluateJavascript("ytPlay()", null)
    fun pause() = webView.evaluateJavascript("ytPause()", null)
    fun seekTo(seconds: Float) = webView.evaluateJavascript("ytSeek($seconds)", null)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun rememberYoutubeWebPlayer(
    onStateChange: (Int) -> Unit,
    onReady: () -> Unit
): Pair<WebView, YoutubeWebPlayer>? {
    var result: Pair<WebView, YoutubeWebPlayer>? = null
    AndroidView(factory = { ctx ->
        WebView(ctx).apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            webViewClient = WebViewClient()
            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun onStateChange(state: Int) = onStateChange(state)
                @android.webkit.JavascriptInterface
                fun onReady() = onReady()
            }, "Android")
            result = this to YoutubeWebPlayer(this)
        }
    }, modifier = Modifier)
    return result
}
