package com.verdor.musica.download

import android.content.Context
import com.verdor.musica.data.AppDatabase
import com.verdor.musica.data.TrackEntity
import com.verdor.musica.network.JamendoTrack
import com.verdor.musica.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

class DownloadRepository(private val context: Context) {

    private val dao = AppDatabase.get(context).trackDao()

    suspend fun downloadJamendoTrack(track: JamendoTrack): TrackEntity = withContext(Dispatchers.IO) {
        val url = track.audiodownload.ifBlank { track.audio }
        val request = Request.Builder().url(url).build()
        val response = NetworkModule.downloadClient.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")

        val dir = File(context.filesDir, "tracks").apply { mkdirs() }
        val outFile = File(dir, "jamendo_${track.id}.mp3")
        response.body?.byteStream()?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }

        val entity = TrackEntity(
                id = "jamendo_${track.id}",
                name = track.name,
                artist = track.artist_name,
                coverSeed = track.id,
                filePath = outFile.absolutePath,
                addedAt = System.currentTimeMillis(),
                fileSizeBytes = outFile.length()
            )
        dao.insert(entity)
        entity
    }

    suspend fun removeTrack(id: String, filePath: String) = withContext(Dispatchers.IO) {
        File(filePath).delete()
        dao.deleteById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        val all = dao.getAll()
        all.forEach { File(it.filePath).delete() }
        dao.clearAll()
    }
}
