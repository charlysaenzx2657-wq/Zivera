package com.verdor.musica.network

import retrofit2.http.GET
import retrofit2.http.Query

data class YoutubeSearchResponse(val items: List<YoutubeItem>)
data class YoutubeItem(val id: YoutubeId, val snippet: YoutubeSnippet)
data class YoutubeId(val videoId: String)
data class YoutubeSnippet(
    val title: String,
    val channelTitle: String,
    val thumbnails: YoutubeThumbnails
)
data class YoutubeThumbnails(val medium: YoutubeThumb?, val default: YoutubeThumb?)
data class YoutubeThumb(val url: String)

interface YoutubeApi {
    @GET("youtube/v3/search")
    suspend fun search(
        @Query("part") part: String = "snippet",
        @Query("type") type: String = "video",
        @Query("videoCategoryId") category: String = "10", // Music
        @Query("maxResults") maxResults: Int = 20,
        @Query("q") query: String,
        @Query("key") key: String
    ): YoutubeSearchResponse
}
