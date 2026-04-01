package com.autoarchery.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_export")
data class SessionExportEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "export_format") val exportFormat: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

