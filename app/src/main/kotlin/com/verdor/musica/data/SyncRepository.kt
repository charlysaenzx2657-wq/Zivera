package com.verdor.musica.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

/** Syncs small metadata to the cloud (favorites list, and which tracks a
 * user has downloaded) so it's visible across devices. The actual audio
 * files stay local to each device — re-downloading on a new device is
 * cheap and avoids needing paid Firebase Storage for large binary files. */
class SyncRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun favoritesCol(uid: String) = db.collection("users").document(uid).collection("favorites")
    private fun downloadsCol(uid: String) = db.collection("users").document(uid).collection("downloads")

    suspend fun pushFavorite(uid: String, fav: FavoriteEntity) {
        favoritesCol(uid).document(fav.id).set(fav).await()
    }

    suspend fun deleteFavorite(uid: String, id: String) {
        favoritesCol(uid).document(id).delete().await()
    }

    suspend fun pullFavorites(uid: String): List<FavoriteEntity> =
        favoritesCol(uid).get().await().documents.mapNotNull { it.toObject<FavoriteEntity>() }

    /** Metadata only — id/name/artist/coverSeed, no file path or blob. */
    suspend fun pushDownloadMeta(uid: String, track: TrackEntity) {
        val meta = mapOf(
            "id" to track.id, "name" to track.name, "artist" to track.artist,
            "coverSeed" to track.coverSeed, "addedAt" to track.addedAt
        )
        downloadsCol(uid).document(track.id).set(meta).await()
    }

    suspend fun deleteDownloadMeta(uid: String, id: String) {
        downloadsCol(uid).document(id).delete().await()
    }

    suspend fun pullDownloadIds(uid: String): List<String> =
        downloadsCol(uid).get().await().documents.map { it.id }
}
