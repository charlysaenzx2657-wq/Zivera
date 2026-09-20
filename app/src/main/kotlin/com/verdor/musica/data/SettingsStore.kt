package com.verdor.musica.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.verdor.musica.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "verdor_settings")

object Keys {
    val YT_API_KEY = stringPreferencesKey("yt_api_key")
    val JAMENDO_CLIENT_ID = stringPreferencesKey("jamendo_client_id")
    val LANGUAGE = stringPreferencesKey("language") // "auto" | "es" | "en"
    val EQ_BASS = floatPreferencesKey("eq_bass")
    val EQ_MID = floatPreferencesKey("eq_mid")
    val EQ_TREBLE = floatPreferencesKey("eq_treble")
}

class SettingsStore(private val context: Context) {

    /** What's actually saved by the user in Settings (may be blank). */
    val youtubeKeyRaw: Flow<String> = context.dataStore.data.map { it[Keys.YT_API_KEY] ?: "" }
    val jamendoClientIdRaw: Flow<String> = context.dataStore.data.map { it[Keys.JAMENDO_CLIENT_ID] ?: "" }

    /** What the rest of the app should actually use: the user's own key if
     * they entered one in Settings, otherwise the default baked in at
     * build time from local.properties / the GitHub Actions secret —
     * so the app works immediately without a trip to Settings. */
    val youtubeKey: Flow<String> = youtubeKeyRaw.map { it.ifBlank { BuildConfig.YOUTUBE_API_KEY_DEFAULT } }
    val jamendoClientId: Flow<String> = jamendoClientIdRaw.map { it.ifBlank { BuildConfig.JAMENDO_CLIENT_ID_DEFAULT } }

    val hasBuiltInYoutubeKey: Boolean get() = BuildConfig.YOUTUBE_API_KEY_DEFAULT.isNotBlank()
    val hasBuiltInJamendoKey: Boolean get() = BuildConfig.JAMENDO_CLIENT_ID_DEFAULT.isNotBlank()

    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "auto" }
    val eqBass: Flow<Float> = context.dataStore.data.map { it[Keys.EQ_BASS] ?: 0f }
    val eqMid: Flow<Float> = context.dataStore.data.map { it[Keys.EQ_MID] ?: 0f }
    val eqTreble: Flow<Float> = context.dataStore.data.map { it[Keys.EQ_TREBLE] ?: 0f }

    suspend fun setYoutubeKey(v: String) = context.dataStore.edit { it[Keys.YT_API_KEY] = v }
    suspend fun setJamendoClientId(v: String) = context.dataStore.edit { it[Keys.JAMENDO_CLIENT_ID] = v }
    suspend fun setLanguage(v: String) = context.dataStore.edit { it[Keys.LANGUAGE] = v }
    suspend fun setEqBass(v: Float) = context.dataStore.edit { it[Keys.EQ_BASS] = v }
    suspend fun setEqMid(v: Float) = context.dataStore.edit { it[Keys.EQ_MID] = v }
    suspend fun setEqTreble(v: Float) = context.dataStore.edit { it[Keys.EQ_TREBLE] = v }
}

