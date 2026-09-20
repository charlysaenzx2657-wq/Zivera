package com.verdor.musica.network

import retrofit2.http.GET
import retrofit2.http.Query

data class JamendoSearchResponse(val results: List<JamendoTrack>)
data class JamendoTrack(
    val id: String,
    val name: String,
    val artist_name: String,
    val audio: String,
    val audiodownload: String,
    val audiodownload_allowed: Boolean
)

interface JamendoApi {
    @GET("v3.0/tracks/")
    suspend fun search(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 20,
        @Query("search") query: String
    ): JamendoSearchResponse
}
