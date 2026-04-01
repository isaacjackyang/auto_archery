package com.autoarchery.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "operation_log")
data class OperationLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "end_id") val endId: String? = null,
    @ColumnInfo(name = "arrow_shot_id") val arrowShotId: String? = null,
    @ColumnInfo(name = "operation_type") val operationType: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

