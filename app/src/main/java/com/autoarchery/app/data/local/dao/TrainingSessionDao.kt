package com.autoarchery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autoarchery.app.data.local.entity.TrainingSessionEntity

@Dao
interface TrainingSessionDao {
    @Query("SELECT * FROM training_session ORDER BY started_at DESC")
    suspend fun getAll(): List<TrainingSessionEntity>

    @Query("SELECT * FROM training_session ORDER BY started_at DESC LIMIT 1")
    suspend fun getLatest(): TrainingSessionEntity?

    @Query(
        """
        SELECT * FROM training_session
        WHERE athlete_id = :athleteId
        ORDER BY started_at DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestForAthlete(athleteId: String): TrainingSessionEntity?

    @Query("SELECT * FROM training_session WHERE id = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): TrainingSessionEntity?

    @Query(
        """
        SELECT * FROM training_session
        WHERE athlete_id = :athleteId AND id != :excludeSessionId
        ORDER BY started_at DESC
        LIMIT 1
        """,
    )
    suspend fun getPreviousForAthlete(athleteId: String, excludeSessionId: String): TrainingSessionEntity?

    @Upsert
    suspend fun upsert(session: TrainingSessionEntity)
}
