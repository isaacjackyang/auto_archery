package com.autoarchery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autoarchery.app.data.local.entity.TrainingEndEntity

@Dao
interface TrainingEndDao {
    @Query("SELECT * FROM training_end WHERE id = :endId LIMIT 1")
    suspend fun getById(endId: String): TrainingEndEntity?

    @Query("SELECT * FROM training_end WHERE session_id = :sessionId ORDER BY end_index ASC")
    suspend fun getForSession(sessionId: String): List<TrainingEndEntity>

    @Query(
        """
        SELECT * FROM training_end
        WHERE session_id = :sessionId
        ORDER BY end_index DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestForSession(sessionId: String): TrainingEndEntity?

    @Upsert
    suspend fun upsert(trainingEnd: TrainingEndEntity)
}
