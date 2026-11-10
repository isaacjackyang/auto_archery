package com.autoarchery.app.ui.metrics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoarchery.app.AutoArcheryApplication
import com.autoarchery.app.data.local.entity.ArrowShotEntity
import com.autoarchery.app.data.local.entity.TrainingEndEntity
import com.autoarchery.app.domain.targetFaceLabel
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

    fun refreshLanguage() { refresh() }

    private fun refresh() {
        val sessionId = boundSessionId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                val resolvedSessionId = if (sessionId == ACTIVE_SESSION_ROUTE) {
                    sessionRepository.getLatestSession()?.id
                } else {
                    sessionId
                } ?: error(getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_8))

                val detail = sessionRepository.getSessionDetail(resolvedSessionId) ?: error(getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_9))
                val visibleEnds = detail.ends.filter { trainingEnd ->
                    detail.arrowsByEnd[trainingEnd.id].orEmpty().isNotEmpty() || trainingEnd.status == "COMPLETED"
                }
                val comparison = detail.comparisonSession?.takeIf { it.distanceM == detail.session.distanceM && it.targetFaceCode == detail.session.targetFaceCode }

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
                        getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_0)
                    } else {
                        getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_1)
                    },
                    comparisonDetails = buildComparisonDetails(detail.session.totalScore, detail.session.avgScore, comparison?.totalScore, comparison?.avgScore),
                    sessionSummary = getApplication<Application>().getString(com.autoarchery.app.R.string.metric_overview, detail.session.distanceM, targetFaceLabel(detail.session.targetFaceCode), detail.session.totalScore, detail.session.totalArrows, formatValue(detail.session.avgScore)),
                    endSummaries = visibleEnds.map { end ->
                        getApplication<Application>().getString(com.autoarchery.app.R.string.metric_end, end.endIndex, end.subtotalScore, formatValue(end.groupCtcMm), formatValue(end.accuracyOffsetMm))
                    },
                    arrowRows = visibleEnds.flatMap { end ->
                        detail.arrowsByEnd[end.id].orEmpty().map { arrow ->
                            ArrowMetricsRowUi(
                                id = arrow.id,
                                left = getApplication<Application>().getString(com.autoarchery.app.R.string.metric_arrow, end.endIndex, arrow.arrowIndex, arrow.effectiveScoreValue, arrow.clockHour),
                                right = "X/Y ${formatValue(arrow.effectiveXMm)}, ${formatValue(arrow.effectiveYMm)} • ${statusLabel(arrow)}",
                            )
                        }
                    },
                    notes = detail.notes.map { note -> note.noteText }.ifEmpty { listOf(getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_2)) },
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
            getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_3)
        } else {
            getApplication<Application>().getString(com.autoarchery.app.R.string.metric_recent, lastCompletedEnd.subtotalScore, formatValue(lastCompletedEnd.accuracyOffsetMm), formatValue(lastCompletedEnd.precisionMeanRadiusMm))
        }
    }

    private fun buildComparisonDetails(
        currentScore: Int,
        currentAvgScore: Float,
        previousScore: Int?,
        previousAvgScore: Float?,
    ): String {
        if (previousScore == null || previousAvgScore == null) {
            return getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_4)
        }

        val scoreDelta = currentScore - previousScore
        val avgDelta = currentAvgScore - previousAvgScore
        return getApplication<Application>().getString(com.autoarchery.app.R.string.metric_delta, signedInt(scoreDelta), signedFloat(avgDelta))
    }

    private fun formatValue(value: Float?): String =
        if (value == null) "-" else String.format(Locale.US, "%.1f", value)

    private fun signedInt(value: Int): String = if (value > 0) "+$value" else value.toString()

    private fun signedFloat(value: Float): String =
        if (value > 0f) String.format(Locale.US, "+%.2f", value) else String.format(Locale.US, "%.2f", value)

    private fun statusLabel(arrow: ArrowShotEntity): String =
        when {
            arrow.isManuallyCorrected || arrow.resultStatus == "CORRECTED" -> getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_5)
            arrow.resultStatus == "CONFIRMED" -> getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_6)
            else -> getApplication<Application>().getString(com.autoarchery.app.R.string.metric_text_7)
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
