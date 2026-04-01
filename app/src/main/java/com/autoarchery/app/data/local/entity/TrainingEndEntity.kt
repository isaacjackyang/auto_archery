package com.autoarchery.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_end")
data class TrainingEndEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "end_index") val endIndex: Int,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "expected_arrow_count") val expectedArrowCount: Int,
    @ColumnInfo(name = "detected_arrow_count") val detectedArrowCount: Int,
    @ColumnInfo(name = "subtotal_score") val subtotalScore: Int,
    @ColumnInfo(name = "x_count") val xCount: Int,
    @ColumnInfo(name = "ten_count") val tenCount: Int,
    @ColumnInfo(name = "group_ctc_mm") val groupCtcMm: Float? = null,
    @ColumnInfo(name = "accuracy_offset_mm") val accuracyOffsetMm: Float? = null,
    @ColumnInfo(name = "precision_mean_radius_mm") val precisionMeanRadiusMm: Float? = null,
    @ColumnInfo(name = "cpk_x") val cpkX: Float? = null,
    @ColumnInfo(name = "cpk_y") val cpkY: Float? = null,
    @ColumnInfo(name = "review_required") val reviewRequired: Boolean = false,
    @ColumnInfo(name = "coach_summary_text") val coachSummaryText: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

