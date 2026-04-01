package com.autoarchery.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "arrow_shot")
data class ArrowShotEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "end_id") val endId: String,
    @ColumnInfo(name = "arrow_index") val arrowIndex: Int,
    @ColumnInfo(name = "detected_at") val detectedAt: Long,
    @ColumnInfo(name = "score_value") val scoreValue: Int,
    @ColumnInfo(name = "is_x") val isX: Boolean,
    @ColumnInfo(name = "clock_hour") val clockHour: Int,
    @ColumnInfo(name = "angle_deg") val angleDeg: Float,
    @ColumnInfo(name = "radius_mm") val radiusMm: Float,
    @ColumnInfo(name = "x_mm") val xMm: Float,
    @ColumnInfo(name = "y_mm") val yMm: Float,
    @ColumnInfo(name = "confidence") val confidence: Float,
    @ColumnInfo(name = "auto_score_value") val autoScoreValue: Int,
    @ColumnInfo(name = "auto_is_x") val autoIsX: Boolean,
    @ColumnInfo(name = "auto_x_mm") val autoXMm: Float,
    @ColumnInfo(name = "auto_y_mm") val autoYMm: Float,
    @ColumnInfo(name = "corrected_score_value") val correctedScoreValue: Int? = null,
    @ColumnInfo(name = "corrected_is_x") val correctedIsX: Boolean? = null,
    @ColumnInfo(name = "corrected_x_mm") val correctedXMm: Float? = null,
    @ColumnInfo(name = "corrected_y_mm") val correctedYMm: Float? = null,
    @ColumnInfo(name = "is_manually_corrected") val isManuallyCorrected: Boolean = false,
    @ColumnInfo(name = "result_status") val resultStatus: String,
    @ColumnInfo(name = "input_method") val inputMethod: String,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "confirmed_at") val confirmedAt: Long? = null,
    @ColumnInfo(name = "announce_text") val announceText: String,
    @ColumnInfo(name = "source_snapshot_id") val sourceSnapshotId: String? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
)

