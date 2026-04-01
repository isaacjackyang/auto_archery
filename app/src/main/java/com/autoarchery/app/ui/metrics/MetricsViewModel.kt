package com.autoarchery.app.ui.metrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoarchery.app.AutoArcheryApplication
import com.autoarchery.app.data.local.entity.ArrowShotEntity
import com.autoarchery.app.data.local.entity.TrainingEndEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class MetricsUiState(
    val athleteName: String,
    val sessionTitle: String,
    val trainingGoal: String,
    val tags: List<String>,
    val coachSummary: String,
    val comparisonHeadline: String,
    val comparisonDetails: String,
    val sessionSummary: String,
    val endSummaries: List<String>,
    val arrowRows: List<ArrowMetricsRowUi>,
    val notes: List<String>,
    val isLoading: Boolean,
    val errorMessage: String?,
)

data class ArrowMetricsRowUi(
    val id: String,
    val left: String,
    val right: String,
)

class MetricsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val sessionRepository = (application as AutoArcheryApplication).appContainer.sessionRepository

    private val _uiState = MutableStateFlow(
        MetricsUiState(
            athleteName = "",
            sessionTitle = "",
            trainingGoal = "",
            tags = emptyList(),
            coachSummary = "",
            comparisonHeadline = "",
            comparisonDetails = "",
            sessionSummary = "",
            endSummaries = emptyList(),
            arrowRows = emptyList(),
            notes = emptyList(),
            isLoading = false,
            errorMessage = null,
        ),
    )

    val uiState: StateFlow<MetricsUiState> = _uiState.asStateFlow()

    private var boundSessionId: String? = null

    fun bindSession(sessionId: String) {
        if (boundSessionId == sessionId && (_uiState.value.sessionTitle.isNotBlank() || _uiState.value.isLoading)) {
            return
        }
        boundSessionId = sessionId
        refresh()
    }

    private fun refresh() {
        val sessionId = boundSessionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val resolvedSessionId = if (sessionId == ACTIVE_SESSION_ROUTE) {
                    sessionRepository.getLatestSession()?.id
                } else {
                    sessionId
                } ?: error("No session available")

                val detail = sessionRepository.getSessionDetail(resolvedSessionId) ?: error("Session not found")
                val visibleEnds = detail.ends.filter { trainingEnd ->
                    detail.arrowsByEnd[trainingEnd.id].orEmpty().isNotEmpty() || trainingEnd.status == "COMPLETED"
                }
                val comparison = detail.comparisonSession

                _uiState.value = MetricsUiState(
                    athleteName = detail.session.athleteName.orEmpty(),
                    sessionTitle = detail.session.title,
                    trainingGoal = detail.session.trainingGoal.orEmpty(),
                    tags = detail.session.drillTagsJson
                        ?.split("|")
                        ?.map { it.trim() }
                        ?.filter { it.isNotEmpty() }
                        .orEmpty(),
                    coachSummary = detail.session.coachSummaryText ?: buildCoachSummary(visibleEnds),
                    comparisonHeadline = if (comparison != null) {
                        "Compared with the previous session"
                    } else {
                        "No baseline session yet"
                    },
                    comparisonDetails = buildComparisonDetails(detail.session.totalScore, detail.session.avgScore, comparison?.totalScore, comparison?.avgScore),
                    sessionSummary = "${detail.session.distanceM}m • ${faceLabel(detail.session.targetFaceCode)} • ${detail.session.totalScore} / ${detail.session.totalArrows}",
                    endSummaries = visibleEnds.map { end ->
                        "End ${end.endIndex} • ${end.subtotalScore} • CTC ${formatValue(end.groupCtcMm)}mm • Accuracy ${formatValue(end.accuracyOffsetMm)}mm"
                    },
                    arrowRows = visibleEnds.flatMap { end ->
                        detail.arrowsByEnd[end.id].orEmpty().map { arrow ->
                            ArrowMetricsRowUi(
                                id = arrow.id,
                                left = "E${end.endIndex} #${arrow.arrowIndex} • ${arrow.effectiveScoreValue} • ${arrow.clockHour} o'clock",
                                right = "X/Y ${formatValue(arrow.effectiveXMm)}, ${formatValue(arrow.effectiveYMm)} • ${statusLabel(arrow)}",
                            )
                        }
                    },
                    notes = detail.notes.map { note -> note.noteText }.ifEmpty { listOf("No coach notes yet") },
                    isLoading = false,
                    errorMessage = null,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unknown error",
                )
            }
        }
    }

    private fun buildCoachSummary(ends: List<TrainingEndEntity>): String {
        val lastCompletedEnd = ends.lastOrNull { it.detectedArrowCount > 0 }
        return if (lastCompletedEnd == null) {
            "No arrows recorded yet."
        } else {
            "Latest end scored ${lastCompletedEnd.subtotalScore}, accuracy ${formatValue(lastCompletedEnd.accuracyOffsetMm)}mm, precision ${formatValue(lastCompletedEnd.precisionMeanRadiusMm)}mm."
        }
    }

    private fun buildComparisonDetails(
        currentScore: Int,
        currentAvgScore: Float,
        previousScore: Int?,
        previousAvgScore: Float?,
    ): String {
        if (previousScore == null || previousAvgScore == null) {
            return "This is the first recorded session for this athlete."
        }

        val scoreDelta = currentScore - previousScore
        val avgDelta = currentAvgScore - previousAvgScore
        return "Score ${signedInt(scoreDelta)} • Avg ${signedFloat(avgDelta)}"
    }

    private fun faceLabel(faceCode: String): String =
        when (faceCode) {
            "FACE_40CM" -> "40 cm"
            else -> "122 cm"
        }

    private fun formatValue(value: Float?): String =
        if (value == null) "-" else String.format(Locale.US, "%.1f", value)

    private fun signedInt(value: Int): String = if (value > 0) "+$value" else value.toString()

    private fun signedFloat(value: Float): String =
        if (value > 0f) String.format(Locale.US, "+%.2f", value) else String.format(Locale.US, "%.2f", value)

    private fun statusLabel(arrow: ArrowShotEntity): String =
        when {
            arrow.isManuallyCorrected || arrow.resultStatus == "CORRECTED" -> "Corrected"
            arrow.resultStatus == "CONFIRMED" -> "Confirmed"
            else -> "Review required"
        }

    private val ArrowShotEntity.effectiveScoreValue: Int
        get() = correctedScoreValue ?: scoreValue

    private val ArrowShotEntity.effectiveXMm: Float
        get() = correctedXMm ?: xMm

    private val ArrowShotEntity.effectiveYMm: Float
        get() = correctedYMm ?: yMm

    companion object {
        private const val ACTIVE_SESSION_ROUTE = "active"
    }
}
