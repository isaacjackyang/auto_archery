package com.autoarchery.app.ui.model

import androidx.compose.ui.graphics.Color

data class LiveSessionUiState(
    val sessionId: String?,
    val sessionTitle: String,
    val athleteName: String,
    val targetFaceCode: String,
    val arrowDiameterMm: Float,
    val currentEndIndex: Int,
    val totalEnds: Int,
    val currentArrowIndex: Int,
    val arrowsPerEnd: Int,
    val isPaused: Boolean,
    val zoomRatio: Float,
    val zoomLabel: String,
    val isRecognitionArmed: Boolean,
    val recognitionLabel: String,
    val analysisResetToken: Int,
    val reviewFilter: ReviewFilter,
    val pendingCount: Int,
    val lowConfidenceCount: Int,
    val selectedShotId: String?,
    val selectedShot: SelectedShotUi?,
    val shots: List<ShotRowUi>,
    val filteredShots: List<ShotRowUi>,
    val markers: List<ShotMarkerUi>,
    val filteredMarkers: List<ShotMarkerUi>,
    val endSummary: EndSummaryUi?,
    val isAdjustSheetVisible: Boolean,
    val adjustmentDraft: ShotAdjustmentUi?,
    val calibrationState: CalibrationState,
    val calibrationLabel: String,
    val isLoading: Boolean,
    val isSessionSetupVisible: Boolean,
    val sessionSetup: SessionSetupUi,
    val errorMessage: String?,
) {
    val currentEndLabel: String
        get() = "End $currentEndIndex/$totalEnds"

    val currentArrowLabel: String
        get() = "Arrow ${currentArrowIndex.coerceAtMost(arrowsPerEnd)}/$arrowsPerEnd"
}

data class SessionSetupUi(
    val athleteOptions: List<AthleteChoiceUi>,
    val selectedAthleteId: String?,
    val selectedDistanceM: Int,
    val selectedArrowsPerEnd: Int,
    val selectedTargetFace: String,
    val trainingGoal: String,
    val canCreate: Boolean,
)

data class AthleteChoiceUi(
    val id: String,
    val name: String,
    val isDefault: Boolean,
)

data class SelectedShotUi(
    val id: String,
    val label: String,
    val scoreValue: Int,
    val isX: Boolean,
    val scoreLabel: String,
    val clockLabel: String,
    val detailLabel: String,
    val xMm: Float,
    val yMm: Float,
    val xNormalized: Float,
    val yNormalized: Float,
)

data class ShotRowUi(
    val id: String,
    val label: String,
    val scoreValue: Int,
    val isX: Boolean,
    val scoreLabel: String,
    val clockHour: Int,
    val clockLabel: String,
    val xMm: Float,
    val yMm: Float,
    val xNormalized: Float,
    val yNormalized: Float,
    val status: ShotStatus,
    val isEdited: Boolean,
)

data class ShotMarkerUi(
    val id: String,
    val xNormalized: Float,
    val yNormalized: Float,
    val status: ShotStatus,
)

data class EndSummaryUi(
    val summaryLine: String,
    val detailLine: String,
)

data class ShotAdjustmentUi(
    val id: String,
    val label: String,
    val xNormalized: Float,
    val yNormalized: Float,
    val xMm: Float,
    val yMm: Float,
    val previewScoreValue: Int,
    val previewScoreLabel: String,
    val previewClockLabel: String,
    val status: ShotStatus,
    val isDirty: Boolean,
)

enum class CalibrationState {
    NotReady,
    NeedsCalibration,
    Calibrated,
}

enum class ReviewFilter {
    All,
    PendingOnly,
    LowConfidenceOnly,
}

enum class ShotStatus(
    val label: String,
    val color: Color,
) {
    Pending("Pending", Color(0xFFE67E22)),
    Confirmed("Confirmed", Color(0xFF2E7D32)),
    Edited("Edited", Color(0xFF1565C0)),
    LowConfidence("Low confidence", Color(0xFFC62828)),
}
