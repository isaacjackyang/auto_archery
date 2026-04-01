package com.autoarchery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autoarchery.app.data.local.entity.AthleteProfileEntity

@Dao
interface AthleteDao {
    @Query("SELECT * FROM athlete_profile ORDER BY display_name ASC")
    suspend fun getAll(): List<AthleteProfileEntity>

    @Query("SELECT * FROM athlete_profile WHERE id = :athleteId LIMIT 1")
    suspend fun getById(athleteId: String): AthleteProfileEntity?

    @Query("SELECT COUNT(*) FROM athlete_profile")
    suspend fun countAll(): Int

    @Upsert
    suspend fun upsert(athlete: AthleteProfileEntity)
}
