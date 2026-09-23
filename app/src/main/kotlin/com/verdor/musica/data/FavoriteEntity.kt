package com.verdor.musica.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A favorited track can come from YouTube (streaming only) or Jamendo/local
 * (playable offline too). Kept separate from TrackEntity because a favorite
 * doesn't require the audio file to be downloaded — just remembered. */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    val coverSeed: String,
    val source: String, // "youtube" | "jamendo"
    val addedAt: Long
)
