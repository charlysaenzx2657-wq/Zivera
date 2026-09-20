package com.verdor.musica.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artist: String,
    val coverSeed: String,
    val filePath: String,
    val addedAt: Long,
    val fileSizeBytes: Long
)
