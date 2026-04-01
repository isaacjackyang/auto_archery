package com.autoarchery.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_preference")
data class AppPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "default_athlete_id") val defaultAthleteId: String? = null,
    @ColumnInfo(name = "display_language") val displayLanguage: String = "zh-TW",
    @ColumnInfo(name = "tts_follow_display_language") val ttsFollowDisplayLanguage: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

