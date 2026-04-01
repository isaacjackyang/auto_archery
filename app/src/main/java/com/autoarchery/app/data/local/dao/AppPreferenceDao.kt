package com.autoarchery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autoarchery.app.data.local.entity.AppPreferenceEntity

@Dao
interface AppPreferenceDao {
    @Query("SELECT * FROM app_preference WHERE id = 1")
    suspend fun getPreference(): AppPreferenceEntity?

    @Upsert
    suspend fun upsert(preference: AppPreferenceEntity)
}
