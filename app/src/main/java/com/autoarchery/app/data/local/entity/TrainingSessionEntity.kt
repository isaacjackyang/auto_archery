package com.autoarchery.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_session")
data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "athlete_id") val athleteId: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "athlete_name") val athleteName: String? = null,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long? = null,
    @ColumnInfo(name = "bow_type") val bowType: String = "RECURVE",
    @ColumnInfo(name = "distance_m") val distanceM: Int,
    @ColumnInfo(name = "target_face_code") val targetFaceCode: String,
    @ColumnInfo(name = "arrows_per_end") val arrowsPerEnd: Int,
    @ColumnInfo(name = "arrow_diameter_mm") val arrowDiameterMm: Float,
    @ColumnInfo(name = "training_goal") val trainingGoal: String? = null,
    @ColumnInfo(name = "drill_tags_json") val drillTagsJson: String? = null,
    @ColumnInfo(name = "location_name") val locationName: String? = null,
    @ColumnInfo(name = "environment_type") val environmentType: String? = null,
    @ColumnInfo(name = "equipment_note") val equipmentNote: String? = null,
    @ColumnInfo(name = "environment_note") val environmentNote: String? = null,
    @ColumnInfo(name = "device_model") val deviceModel: String,
    @ColumnInfo(name = "camera_mode") val cameraMode: String,
    @ColumnInfo(name = "camera_lens_id") val cameraLensId: String? = null,
    @ColumnInfo(name = "calibration_center_x_px") val calibrationCenterXPx: Float? = null,
    @ColumnInfo(name = "calibration_center_y_px") val calibrationCenterYPx: Float? = null,
    @ColumnInfo(name = "calibration_radius_x_px") val calibrationRadiusXPx: Float? = null,
    @ColumnInfo(name = "calibration_radius_y_px") val calibrationRadiusYPx: Float? = null,
    @ColumnInfo(name = "calibration_rotation_deg") val calibrationRotationDeg: Float? = null,
    @ColumnInfo(name = "calibration_confidence") val calibrationConfidence: Float? = null,
    @ColumnInfo(name = "calibration_updated_at") val calibrationUpdatedAt: Long? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "comparison_baseline_session_id") val comparisonBaselineSessionId: String? = null,
    @ColumnInfo(name = "coach_summary_text") val coachSummaryText: String? = null,
    @ColumnInfo(name = "total_ends") val totalEnds: Int = 0,
    @ColumnInfo(name = "total_arrows") val totalArrows: Int = 0,
    @ColumnInfo(name = "total_score") val totalScore: Int = 0,
    @ColumnInfo(name = "avg_score") val avgScore: Float = 0f,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
