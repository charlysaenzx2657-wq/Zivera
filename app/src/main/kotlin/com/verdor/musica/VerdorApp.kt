package com.verdor.musica

import android.app.Application
import com.verdor.musica.data.AuthRepository
import com.verdor.musica.data.SettingsStore
import com.verdor.musica.data.SyncRepository
import com.verdor.musica.download.DownloadRepository

class VerdorApp : Application() {
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var downloadRepository: DownloadRepository
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var syncRepository: SyncRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        downloadRepository = DownloadRepository(this)
        authRepository = AuthRepository()
        syncRepository = SyncRepository()
    }
}
