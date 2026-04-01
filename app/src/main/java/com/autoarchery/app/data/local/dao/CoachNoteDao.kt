package com.autoarchery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.autoarchery.app.data.local.entity.CoachNoteEntity

@Dao
interface CoachNoteDao {
    @Query("SELECT * FROM coach_note WHERE session_id = :sessionId ORDER BY created_at DESC")
    suspend fun getForSession(sessionId: String): List<CoachNoteEntity>

    @Upsert
    suspend fun upsert(note: CoachNoteEntity)
}

