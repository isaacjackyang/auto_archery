package com.autoarchery.app

import android.app.Application
import androidx.room.Room
import com.autoarchery.app.data.AppContainer
import com.autoarchery.app.data.local.AutoArcheryDatabase

class AutoArcheryApplication : Application() {
    override fun attachBaseContext(base: android.content.Context) {
        val code = base.getSharedPreferences("ui_locale", android.content.Context.MODE_PRIVATE).getString("language", "zh-TW")!!
        val configuration = android.content.res.Configuration(base.resources.configuration)
        configuration.setLocale(java.util.Locale.forLanguageTag(code))
        super.attachBaseContext(base.createConfigurationContext(configuration))
    }

    val database: AutoArcheryDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AutoArcheryDatabase::class.java,
            "auto-archery.db",
        ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }

    val appContainer: AppContainer by lazy {
        AppContainer(applicationContext, database)
    }

    override fun onTerminate() {
        appContainer.speechCoordinator.shutdown()
        super.onTerminate()
    }
}
