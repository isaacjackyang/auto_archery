package com.autoarchery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autoarchery.app.data.local.entity.ArrowShotEntity

@Dao
interface ArrowShotDao {
    @Query("SELECT * FROM arrow_shot WHERE id = :shotId LIMIT 1")
    suspend fun getById(shotId: String): ArrowShotEntity?

    @Query(
        """
        SELECT * FROM arrow_shot
        WHERE end_id = :endId AND is_deleted = 0
        ORDER BY arrow_index ASC
        """,
    )
    suspend fun getForEnd(endId: String): List<ArrowShotEntity>

    @Upsert
    suspend fun upsert(arrowShot: ArrowShotEntity)
}
