package com.autoarchery.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "coach_note")
data class CoachNoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String? = null,
    @ColumnInfo(name = "end_id") val endId: String? = null,
    @ColumnInfo(name = "arrow_shot_id") val arrowShotId: String? = null,
    @ColumnInfo(name = "note_level") val noteLevel: String,
    @ColumnInfo(name = "note_text") val noteText: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

