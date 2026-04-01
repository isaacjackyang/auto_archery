package com.autoarchery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.autoarchery.app.data.local.entity.SessionExportEntity

@Dao
interface SessionExportDao {
    @Insert
    suspend fun insert(export: SessionExportEntity)

    @Query("SELECT * FROM session_export WHERE session_id = :sessionId ORDER BY created_at DESC")
    suspend fun getForSession(sessionId: String): List<SessionExportEntity>
}

