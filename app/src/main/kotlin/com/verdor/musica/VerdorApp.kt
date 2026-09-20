package com.verdor.musica

import android.app.Application
import com.verdor.musica.data.SettingsStore
import com.verdor.musica.download.DownloadRepository

class VerdorApp : Application() {
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var downloadRepository: DownloadRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        downloadRepository = DownloadRepository(this)
    }
}
