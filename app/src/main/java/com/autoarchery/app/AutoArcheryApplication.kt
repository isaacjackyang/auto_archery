package com.autoarchery.app

import android.app.Application
import androidx.room.Room
import com.autoarchery.app.data.AppContainer
import com.autoarchery.app.data.local.AutoArcheryDatabase

class AutoArcheryApplication : Application() {
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
