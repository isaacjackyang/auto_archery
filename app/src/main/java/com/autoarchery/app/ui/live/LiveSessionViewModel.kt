package com.autoarchery.app.ui.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoarchery.app.AutoArcheryApplication
import com.autoarchery.app.R
import com.autoarchery.app.data.local.entity.ArrowShotEntity
import com.autoarchery.app.data.repository.RecordedShotResult
import com.autoarchery.app.data.repository.SessionCalibrationInput
import com.autoarchery.app.data.repository.SessionSetupInput
import com.autoarchery.app.domain.clockHour
import com.autoarchery.app.domain.clockLabel
import com.autoarchery.app.domain.mmToNormalized
import com.autoarchery.app.domain.scoreLabel
import com.autoarchery.app.ui.model.AthleteChoiceUi
import com.autoarchery.app.ui.model.CalibrationState
import com.autoarchery.app.ui.model.EndSummaryUi
import com.autoarchery.app.ui.model.LiveSessionUiState
import com.autoarchery.app.ui.model.ReviewFilter
import com.autoarchery.app.ui.model.SelectedShotUi
import com.autoarchery.app.ui.model.SessionSetupUi
import com.autoarchery.app.ui.model.ShotAdjustmentUi
import com.autoarchery.app.ui.model.ShotMarkerUi
import com.autoarchery.app.ui.model.ShotRowUi
import com.autoarchery.app.ui.model.ShotStatus
import com.autoarchery.app.vision.DetectedShotResult
import com.autoarchery.app.vision.ResolvedCameraProfile
import com.autoarchery.app.vision.TargetCalibrationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

class LiveSessionViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as AutoArcheryApplication).appContainer
    private val athleteRepository = appContainer.athleteRepository
    private val preferenceRepository = appContainer.preferenceRepository
    private val sessionRepository = appContainer.sessionRepository
    private val speechCoordinator = appContainer.speechCoordinator

    private val _uiState = MutableStateFlow(defaultState())
    val uiState: StateFlow<LiveSessionUiState> = _uiState.asStateFlow()

    private var boundRouteSessionId: String = ACTIVE_SESSION_ROUTE
    private var isCameraReady: Boolean = false
    private var isRecordingDetection: Boolean = false
    private var resolvedCameraProfile: ResolvedCameraProfile? = null

    fun bindSession(sessionId: String) {
        if (boundRouteSessionId == sessionId && (_uiState.value.sessionId != null || _uiState.value.isSessionSetupVisible)) {
            return
        }
        boundRouteSessionId = sessionId
        disarmRecognition(
            nextCalibrationState = if (isCameraReady) CalibrationState.NeedsCalibration else CalibrationState.NotReady,
            clearCalibrationLabel = true,
        )
        refresh()
    }

    fun selectShot(shotId: String) {
        _uiState.value = _uiState.value.copy(selectedShotId = shotId).recalculate()
    }

    fun openAdjustSheet() {
        val selectedShot = _uiState.value.selectedShot ?: return
        _uiState.value = _uiState.value.copy(
            isAdjustSheetVisible = true,
            adjustmentDraft = selectedShot.toAdjustmentDraft(),
        ).recalculate()
    }

    fun closeAdjustSheet() {
        _uiState.value = _uiState.value.copy(
            isAdjustSheetVisible = false,
            adjustmentDraft = null,
        ).recalculate()
    }

    fun saveAdjustments() {
        val adjustmentDraft = _uiState.value.adjustmentDraft ?: return
        viewModelScope.launch {
            sessionRepository.applyShotCorrection(
                shotId = adjustmentDraft.id,
                correctedXMm = adjustmentDraft.xMm,
                correctedYMm = adjustmentDraft.yMm,
            )
            _uiState.value = _uiState.value.copy(
                isAdjustSheetVisible = false,
                adjustmentDraft = null,
            )
            refresh(preselectedShotId = adjustmentDraft.id)
        }
    }

    fun updateAdjustmentPosition(xNormalized: Float, yNormalized: Float) {
        val adjustmentDraft = _uiState.value.adjustmentDraft ?: return
        val correctedXMm = sessionRepository.normalizedToMm(xNormalized.coerceIn(-1f, 1f), _uiState.value.targetFaceCode)
        val correctedYMm = sessionRepository.normalizedToMm(yNormalized.coerceIn(-1f, 1f), _uiState.value.targetFaceCode)
        val correction = sessionRepository.calculateShotCorrection(
            xMm = correctedXMm,
            yMm = correctedYMm,
            targetFaceCode = _uiState.value.targetFaceCode,
            arrowDiameterMm = _uiState.value.arrowDiameterMm,
        )
        _uiState.value = _uiState.value.copy(
            adjustmentDraft = adjustmentDraft.copy(
                xNormalized = sessionRepository.mmToNormalized(correctedXMm, _uiState.value.targetFaceCode),
                yNormalized = sessionRepository.mmToNormalized(correctedYMm, _uiState.value.targetFaceCode),
                xMm = correctedXMm,
                yMm = correctedYMm,
                previewScoreValue = correction.scoreValue,
                previewScoreLabel = scoreLabel(correction.scoreValue, correction.isX),
                previewClockLabel = clockLabel(correctedXMm, correctedYMm, _uiState.value.targetFaceCode),
                isDirty = true,
            ),
        ).recalculate()
    }

    fun confirmSelectedShot() {
        val selectedShotId = _uiState.value.selectedShotId ?: return
        viewModelScope.launch {
            sessionRepository.updateShotStatus(selectedShotId, status = "CONFIRMED")
            refresh(preselectedShotId = selectedShotId)
        }
    }

    fun replaySelectedShot() {
        val selectedShot = _uiState.value.selectedShot ?: return
        viewModelScope.launch {
            speakShot(
                scoreValue = selectedShot.scoreValue,
                isX = selectedShot.isX,
                clockHour = clockHour(selectedShot.xMm, selectedShot.yMm, _uiState.value.targetFaceCode),
            )
        }
    }

    fun onCameraReadyChanged(isReady: Boolean) {
        val readinessChanged = isCameraReady != isReady
        isCameraReady = isReady
        val nextState = when {
            !isReady -> CalibrationState.NotReady
            readinessChanged -> CalibrationState.NeedsCalibration
            _uiState.value.calibrationState == CalibrationState.Calibrated -> CalibrationState.Calibrated
            else -> CalibrationState.NeedsCalibration
        }
        _uiState.value = _uiState.value.copy(
            calibrationState = nextState,
            calibrationLabel = calibrationLabel(nextState, null),
            isRecognitionArmed = false,
            recognitionLabel = recognitionLabel(
                isCameraReady = isReady,
                isArmed = false,
                calibrationState = nextState,
            ),
            analysisResetToken = if (readinessChanged) _uiState.value.analysisResetToken + 1 else _uiState.value.analysisResetToken,
        )
    }

    fun onCameraProfileResolved(profile: ResolvedCameraProfile) {
        resolvedCameraProfile = profile
        val currentState = _uiState.value
        val nextZoomRatio = if (currentState.zoomLabel == WAITING_FOR_CAMERA_LABEL) {
            profile.zoomRatio
        } else {
            currentState.zoomRatio.coerceAtLeast(profile.zoomRatio)
        }
        _uiState.value = currentState.copy(
            zoomRatio = nextZoomRatio,
            zoomLabel = formatZoomLabel(profile, nextZoomRatio),
        )
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            sessionRepository.updateCameraProfile(
                sessionId = sessionId,
                cameraMode = profile.cameraMode,
                cameraLensId = profile.cameraLensId,
            )
        }
    }

    fun onCalibrationDetected(result: TargetCalibrationResult) {
        val sessionId = _uiState.value.sessionId ?: return
        _uiState.value = _uiState.value.copy(
            calibrationState = CalibrationState.Calibrated,
            calibrationLabel = calibrationLabel(CalibrationState.Calibrated, result.confidence),
            recognitionLabel = recognitionLabel(
                isCameraReady = isCameraReady,
                isArmed = _uiState.value.isRecognitionArmed,
                calibrationState = CalibrationState.Calibrated,
            ),
        )
        viewModelScope.launch {
            sessionRepository.updateCalibration(
                sessionId = sessionId,
                calibration = SessionCalibrationInput(
                    centerXPx = result.centerXPx,
                    centerYPx = result.centerYPx,
                    radiusXPx = result.radiusXPx,
                    radiusYPx = result.radiusYPx,
                    rotationDeg = result.rotationDeg,
                    confidence = result.confidence,
                ),
            )
        }
    }

    fun onShotDetected(result: DetectedShotResult) {
        val sessionId = _uiState.value.sessionId ?: return
        if (_uiState.value.isPaused || isRecordingDetection) return

        isRecordingDetection = true
        viewModelScope.launch {
            runCatching {
                sessionRepository.recordDetectedShot(
                    sessionId = sessionId,
                    xNormalized = result.xNormalized,
                    yNormalized = result.yNormalized,
                    confidence = result.confidence,
                )
            }.onSuccess { recordedShot ->
                if (recordedShot != null) {
                    speakShot(recordedShot)
                    refresh(preselectedShotId = recordedShot.shotId)
                }
            }.also {
                isRecordingDetection = false
            }
        }
    }

    fun togglePaused() {
        _uiState.value = _uiState.value.copy(isPaused = !_uiState.value.isPaused)
    }

    fun increaseDigitalZoom() {
        if (!isCameraReady || _uiState.value.sessionId == null) return
        val nextZoomRatio = (_uiState.value.zoomRatio + DIGITAL_ZOOM_STEP).coerceAtMost(MAX_REQUESTED_ZOOM_RATIO)
        val nextCalibrationState = CalibrationState.NeedsCalibration
        _uiState.value = _uiState.value.copy(
            zoomRatio = nextZoomRatio,
            zoomLabel = formatZoomLabel(resolvedCameraProfile, nextZoomRatio),
            calibrationState = nextCalibrationState,
            calibrationLabel = calibrationLabel(nextCalibrationState, null),
            isRecognitionArmed = false,
            recognitionLabel = recognitionLabel(
                isCameraReady = isCameraReady,
                isArmed = false,
                calibrationState = nextCalibrationState,
            ),
            analysisResetToken = _uiState.value.analysisResetToken + 1,
        )
    }

    fun decreaseDigitalZoom() {
        if (!isCameraReady || _uiState.value.sessionId == null) return
        val minimumZoomRatio = resolvedCameraProfile?.zoomRatio ?: 1f
        val nextZoomRatio = (_uiState.value.zoomRatio - DIGITAL_ZOOM_STEP).coerceAtLeast(minimumZoomRatio)
        if (abs(nextZoomRatio - _uiState.value.zoomRatio) < 0.01f) return
        val nextCalibrationState = CalibrationState.NeedsCalibration
        _uiState.value = _uiState.value.copy(
            zoomRatio = nextZoomRatio,
            zoomLabel = formatZoomLabel(resolvedCameraProfile, nextZoomRatio),
            calibrationState = nextCalibrationState,
            calibrationLabel = calibrationLabel(nextCalibrationState, null),
            isRecognitionArmed = false,
            recognitionLabel = recognitionLabel(
                isCameraReady = isCameraReady,
                isArmed = false,
                calibrationState = nextCalibrationState,
            ),
            analysisResetToken = _uiState.value.analysisResetToken + 1,
        )
    }

    fun onZoomRatioApplied(appliedZoomRatio: Float) {
        val currentState = _uiState.value
        if (abs(currentState.zoomRatio - appliedZoomRatio) < 0.01f) {
            val formattedLabel = formatZoomLabel(resolvedCameraProfile, appliedZoomRatio)
            if (currentState.zoomLabel == formattedLabel) return
            _uiState.value = currentState.copy(zoomLabel = formattedLabel)
            return
        }
        _uiState.value = currentState.copy(
            zoomRatio = appliedZoomRatio,
            zoomLabel = formatZoomLabel(resolvedCameraProfile, appliedZoomRatio),
        )
    }

    fun setReviewFilter(filter: ReviewFilter) {
        _uiState.value = _uiState.value.copy(reviewFilter = filter).recalculate()
    }

    fun manualAddShot() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            val createdShotId = sessionRepository.manualAddShot(sessionId)
            refresh(preselectedShotId = createdShotId)
        }
    }

    fun deleteLastShot() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            sessionRepository.deleteLastShot(sessionId)
            refresh()
        }
    }

    fun clearCurrentEndShots() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            sessionRepository.clearCurrentEndShots(sessionId)
            refresh()
        }
    }

    fun reanalyzeSelectedShot() {
        val selectedShotId = _uiState.value.selectedShotId ?: return
        viewModelScope.launch {
            sessionRepository.updateShotStatus(selectedShotId, status = "REVIEW_REQUIRED")
            refresh(preselectedShotId = selectedShotId)
        }
    }

    fun confirmHighConfidenceShots() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            sessionRepository.confirmHighConfidenceShots(sessionId)
            refresh()
        }
    }

    fun confirmCurrentEnd() {
        val sessionId = _uiState.value.sessionId ?: return
        viewModelScope.launch {
            sessionRepository.confirmCurrentEnd(sessionId)
            val nextState = if (isCameraReady) CalibrationState.NeedsCalibration else CalibrationState.NotReady
            _uiState.value = _uiState.value.copy(
                calibrationState = nextState,
                calibrationLabel = calibrationLabel(nextState, null),
                isRecognitionArmed = false,
                recognitionLabel = recognitionLabel(
                    isCameraReady = isCameraReady,
                    isArmed = false,
                    calibrationState = nextState,
                ),
                analysisResetToken = _uiState.value.analysisResetToken + 1,
            )
            refresh()
        }
    }

    fun armRecognition() {
        if (!isCameraReady || _uiState.value.sessionId == null || _uiState.value.calibrationState != CalibrationState.Calibrated) return
        _uiState.value = _uiState.value.copy(
            isRecognitionArmed = true,
            recognitionLabel = recognitionLabel(
                isCameraReady = isCameraReady,
                isArmed = true,
                calibrationState = _uiState.value.calibrationState,
            ),
            analysisResetToken = _uiState.value.analysisResetToken + 1,
        )
    }

    fun resetCalibration() {
        val nextState = if (isCameraReady) CalibrationState.NeedsCalibration else CalibrationState.NotReady
        disarmRecognition(nextCalibrationState = nextState, clearCalibrationLabel = true)
    }

    fun openSessionSetup() {
        _uiState.value = _uiState.value.copy(isSessionSetupVisible = true)
        refresh()
    }

    fun closeSessionSetup() {
        if (_uiState.value.sessionId != null) {
            _uiState.value = _uiState.value.copy(isSessionSetupVisible = false)
        }
    }

    fun updateSetupAthlete(athleteId: String) {
        _uiState.value = _uiState.value.copy(
            sessionSetup = _uiState.value.sessionSetup.copy(
                selectedAthleteId = athleteId,
                canCreate = true,
            ),
        )
    }

    fun updateSetupDistance(distanceM: Int) {
        _uiState.value = _uiState.value.copy(
            sessionSetup = _uiState.value.sessionSetup.copy(selectedDistanceM = distanceM),
        )
    }

    fun updateSetupArrowsPerEnd(arrowsPerEnd: Int) {
        _uiState.value = _uiState.value.copy(
            sessionSetup = _uiState.value.sessionSetup.copy(selectedArrowsPerEnd = arrowsPerEnd),
        )
    }

    fun updateSetupTargetFace(targetFace: String) {
        _uiState.value = _uiState.value.copy(
            sessionSetup = _uiState.value.sessionSetup.copy(selectedTargetFace = targetFace),
        )
    }

    fun updateTrainingGoal(trainingGoal: String) {
        _uiState.value = _uiState.value.copy(
            sessionSetup = _uiState.value.sessionSetup.copy(trainingGoal = trainingGoal),
        )
    }

    fun createSession() {
        val setup = _uiState.value.sessionSetup
        val athleteId = setup.selectedAthleteId ?: return
        viewModelScope.launch {
            val athlete = athleteRepository.getById(athleteId) ?: return@launch
            val sessionId = sessionRepository.createSession(
                SessionSetupInput(
                    athleteId = athlete.id,
                    athleteName = athlete.displayName,
                    distanceM = setup.selectedDistanceM,
                    arrowsPerEnd = setup.selectedArrowsPerEnd,
                    targetFaceCode = setup.selectedTargetFace,
                    trainingGoal = setup.trainingGoal,
                ),
            )
            boundRouteSessionId = sessionId
            preferenceRepository.setDefaultAthlete(athlete.id)
            val calibrationState = if (isCameraReady) CalibrationState.NeedsCalibration else CalibrationState.NotReady
            _uiState.value = _uiState.value.copy(
                isSessionSetupVisible = false,
                isRecognitionArmed = false,
                recognitionLabel = recognitionLabel(
                    isCameraReady = isCameraReady,
                    isArmed = false,
                    calibrationState = calibrationState,
                ),
                analysisResetToken = _uiState.value.analysisResetToken + 1,
                calibrationState = calibrationState,
                calibrationLabel = calibrationLabel(calibrationState, null),
            )
            refresh()
        }
    }

    private fun refresh(preselectedShotId: String? = _uiState.value.selectedShotId) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                ensureSeedData()

                val preference = preferenceRepository.getPreference()
                val athletes = athleteRepository.getAll()
                val selectedAthleteId = _uiState.value.sessionSetup.selectedAthleteId ?: preference.defaultAthleteId ?: athletes.firstOrNull()?.id
                val session = if (boundRouteSessionId == ACTIVE_SESSION_ROUTE) {
                    sessionRepository.getLatestSession()
                } else {
                    sessionRepository.getSession(boundRouteSessionId)
                }

                if (session == null) {
                    val calibrationState = if (isCameraReady) CalibrationState.NeedsCalibration else CalibrationState.NotReady
                    val currentState = _uiState.value
                    _uiState.value = defaultState().copy(
                        isLoading = false,
                        isSessionSetupVisible = true,
                        zoomRatio = currentState.zoomRatio,
                        zoomLabel = currentState.zoomLabel,
                        isRecognitionArmed = false,
                        recognitionLabel = recognitionLabel(
                            isCameraReady = isCameraReady,
                            isArmed = false,
                            calibrationState = calibrationState,
                        ),
                        analysisResetToken = currentState.analysisResetToken,
                        calibrationState = calibrationState,
                        calibrationLabel = calibrationLabel(calibrationState, null),
                        sessionSetup = _uiState.value.sessionSetup.copy(
                            athleteOptions = athletes.map { athlete ->
                                AthleteChoiceUi(
                                    id = athlete.id,
                                    name = athlete.displayName,
                                    isDefault = athlete.id == preference.defaultAthleteId,
                                )
                            },
                            selectedAthleteId = selectedAthleteId,
                            canCreate = selectedAthleteId != null,
                        ),
                    )
                    return@launch
                }

                val bundle = sessionRepository.getBundle(session.id) ?: return@launch
                val mappedShots = bundle.arrows.map { shot ->
                    val status = mapShotStatus(shot.resultStatus, shot.confidence, shot.isManuallyCorrected)
                    val effectiveXMm = shot.effectiveXMm
                    val effectiveYMm = shot.effectiveYMm
                    val effectiveScoreValue = shot.effectiveScoreValue
                    val effectiveIsX = shot.effectiveIsX
                    ShotRowUi(
                        id = shot.id,
                        label = "Arrow ${shot.arrowIndex}",
                        scoreValue = effectiveScoreValue,
                        isX = effectiveIsX,
                        scoreLabel = scoreLabel(effectiveScoreValue, effectiveIsX),
                        clockHour = clockHour(effectiveXMm, effectiveYMm, session.targetFaceCode),
                        clockLabel = clockLabel(effectiveXMm, effectiveYMm, session.targetFaceCode),
                        xMm = effectiveXMm,
                        yMm = effectiveYMm,
                        xNormalized = mmToNormalized(effectiveXMm, session.targetFaceCode),
                        yNormalized = mmToNormalized(effectiveYMm, session.targetFaceCode),
                        status = status,
                        isEdited = shot.isManuallyCorrected,
                    )
                }
                val mappedMarkers = bundle.arrows.map { shot ->
                    val status = mapShotStatus(shot.resultStatus, shot.confidence, shot.isManuallyCorrected)
                    ShotMarkerUi(
                        id = shot.id,
                        xNormalized = mmToNormalized(shot.effectiveXMm, session.targetFaceCode),
                        yNormalized = mmToNormalized(shot.effectiveYMm, session.targetFaceCode),
                        status = status,
                    )
                }

                val activeAthlete = athletes.firstOrNull { it.id == session.athleteId }
                val endsCount = max(bundle.ends.size, 1)
                val calibrationState = when {
                    !isCameraReady -> CalibrationState.NotReady
                    _uiState.value.calibrationState == CalibrationState.NeedsCalibration && _uiState.value.sessionId == session.id -> CalibrationState.NeedsCalibration
                    session.calibrationUpdatedAt != null -> CalibrationState.Calibrated
                    else -> CalibrationState.NeedsCalibration
                }
                val selectedShotId = preselectedShotId?.takeIf { selectedId -> mappedShots.any { it.id == selectedId } }
                    ?: mappedShots.lastOrNull()?.id

                val currentState = _uiState.value
                _uiState.value = currentState.copy(
                    sessionId = session.id,
                    sessionTitle = session.title,
                    athleteName = activeAthlete?.displayName ?: session.athleteName.orEmpty(),
                    targetFaceCode = session.targetFaceCode,
                    arrowDiameterMm = session.arrowDiameterMm,
                    currentEndIndex = bundle.currentEnd.endIndex,
                    totalEnds = endsCount,
                    currentArrowIndex = bundle.arrows.size + 1,
                    arrowsPerEnd = bundle.currentEnd.expectedArrowCount,
                    zoomLabel = if (resolvedCameraProfile == null) {
                        currentState.zoomLabel
                    } else {
                        formatZoomLabel(resolvedCameraProfile, currentState.zoomRatio)
                    },
                    shots = mappedShots,
                    markers = mappedMarkers,
                    selectedShotId = selectedShotId,
                    endSummary = EndSummaryUi(
                        summaryLine = "Score ${bundle.currentEnd.subtotalScore} • X ${bundle.currentEnd.xCount} • 10s ${bundle.currentEnd.tenCount}",
                        detailLine = "CTC ${bundle.currentEnd.groupCtcMm ?: 0f}mm • Accuracy ${bundle.currentEnd.accuracyOffsetMm ?: 0f}mm",
                    ),
                    calibrationState = calibrationState,
                    calibrationLabel = calibrationLabel(calibrationState, session.calibrationConfidence),
                    recognitionLabel = recognitionLabel(
                        isCameraReady = isCameraReady,
                        isArmed = currentState.isRecognitionArmed,
                        calibrationState = calibrationState,
                    ),
                    isLoading = false,
                    isSessionSetupVisible = currentState.isSessionSetupVisible,
                    sessionSetup = currentState.sessionSetup.copy(
                        athleteOptions = athletes.map { athlete ->
                            AthleteChoiceUi(
                                id = athlete.id,
                                name = athlete.displayName,
                                isDefault = athlete.id == preference.defaultAthleteId,
                            )
                        },
                        selectedAthleteId = selectedAthleteId,
                        canCreate = selectedAthleteId != null,
                    ),
                ).recalculate()
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unknown error",
                )
            }
        }
    }

    private suspend fun speakShot(recordedShot: RecordedShotResult) {
        speakShot(
            scoreValue = recordedShot.scoreValue,
            isX = recordedShot.isX,
            clockHour = recordedShot.clockHour,
        )
    }

    private suspend fun speakShot(
        scoreValue: Int,
        isX: Boolean,
        clockHour: Int,
    ) {
        val preference = preferenceRepository.getPreference()
        val languageCode = if (preference.ttsFollowDisplayLanguage) preference.displayLanguage else DEFAULT_TTS_LANGUAGE
        val announcement = when {
            languageCode.startsWith("zh", ignoreCase = true) && isX -> "${clockHour} 點鐘方向，X，10 分"
            languageCode.startsWith("zh", ignoreCase = true) -> "${clockHour} 點鐘方向，${scoreValue} 分"
            isX -> "$clockHour o'clock, X, 10 points"
            else -> "$clockHour o'clock, $scoreValue points"
        }
        speechCoordinator.speak(announcement, languageCode)
    }

    private suspend fun ensureSeedData() {
        val preference = preferenceRepository.getPreference()
        val seedAthlete = athleteRepository.ensureSeedAthlete()
        if (preference.defaultAthleteId == null) {
            preferenceRepository.setDefaultAthlete(seedAthlete.id)
        }
    }

    private fun mapShotStatus(resultStatus: String, confidence: Float, isEdited: Boolean): ShotStatus =
        when {
            isEdited || resultStatus == "CORRECTED" -> ShotStatus.Edited
            resultStatus == "CONFIRMED" -> ShotStatus.Confirmed
            confidence < 0.7f -> ShotStatus.LowConfidence
            else -> ShotStatus.Pending
        }

    private fun LiveSessionUiState.recalculate(): LiveSessionUiState {
        val pending = shots.count { it.status == ShotStatus.Pending }
        val lowConfidence = shots.count { it.status == ShotStatus.LowConfidence }
        val filteredShots = shots.filterBy(reviewFilter)
        val filteredMarkers = markers
            .filterMarkers(reviewFilter)
            .withDraftPreview(adjustmentDraft)
        val selectedShot = shots.firstOrNull { it.id == selectedShotId }
        val previewDraft = adjustmentDraft?.takeIf { it.id == selectedShotId }

        return copy(
            pendingCount = pending,
            lowConfidenceCount = lowConfidence,
            filteredShots = filteredShots,
            filteredMarkers = filteredMarkers,
            selectedShot = selectedShot?.let { shot ->
                if (previewDraft != null) {
                    SelectedShotUi(
                        id = shot.id,
                        label = shot.label,
                        scoreValue = previewDraft.previewScoreValue,
                        isX = previewDraft.previewScoreLabel.contains("X"),
                        scoreLabel = previewDraft.previewScoreLabel,
                        clockLabel = previewDraft.previewClockLabel,
                        detailLabel = "${previewDraft.previewClockLabel} • ${shot.status.label}",
                        xMm = previewDraft.xMm,
                        yMm = previewDraft.yMm,
                        xNormalized = previewDraft.xNormalized,
                        yNormalized = previewDraft.yNormalized,
                    )
                } else {
                    SelectedShotUi(
                        id = shot.id,
                        label = shot.label,
                        scoreValue = shot.scoreValue,
                        isX = shot.isX,
                        scoreLabel = shot.scoreLabel,
                        clockLabel = shot.clockLabel,
                        detailLabel = "${shot.clockLabel} • ${shot.status.label}",
                        xMm = shot.xMm,
                        yMm = shot.yMm,
                        xNormalized = shot.xNormalized,
                        yNormalized = shot.yNormalized,
                    )
                }
            },
            currentArrowIndex = max(1, shots.size + 1),
        )
    }

    private fun List<ShotRowUi>.filterBy(filter: ReviewFilter): List<ShotRowUi> =
        when (filter) {
            ReviewFilter.All -> this
            ReviewFilter.PendingOnly -> filter { it.status == ShotStatus.Pending }
            ReviewFilter.LowConfidenceOnly -> filter { it.status == ShotStatus.LowConfidence }
        }

    private fun List<ShotMarkerUi>.filterMarkers(filter: ReviewFilter): List<ShotMarkerUi> =
        when (filter) {
            ReviewFilter.All -> this
            ReviewFilter.PendingOnly -> filter { it.status == ShotStatus.Pending }
            ReviewFilter.LowConfidenceOnly -> filter { it.status == ShotStatus.LowConfidence }
        }

    private fun List<ShotMarkerUi>.withDraftPreview(draft: ShotAdjustmentUi?): List<ShotMarkerUi> {
        if (draft == null) return this
        return map { marker ->
            if (marker.id == draft.id) {
                marker.copy(
                    xNormalized = draft.xNormalized,
                    yNormalized = draft.yNormalized,
                )
            } else {
                marker
            }
        }
    }

    private fun defaultState(): LiveSessionUiState =
        LiveSessionUiState(
            sessionId = null,
            sessionTitle = "New Session",
            athleteName = "",
            targetFaceCode = "FACE_122CM",
            arrowDiameterMm = 5.5f,
            currentEndIndex = 1,
            totalEnds = 1,
            currentArrowIndex = 1,
            arrowsPerEnd = 6,
            isPaused = false,
            zoomRatio = 1f,
            zoomLabel = WAITING_FOR_CAMERA_LABEL,
            isRecognitionArmed = false,
            recognitionLabel = recognitionLabel(
                isCameraReady = false,
                isArmed = false,
                calibrationState = CalibrationState.NotReady,
            ),
            analysisResetToken = 0,
            reviewFilter = ReviewFilter.All,
            pendingCount = 0,
            lowConfidenceCount = 0,
            selectedShotId = null,
            selectedShot = null,
            shots = emptyList(),
            filteredShots = emptyList(),
            markers = emptyList(),
            filteredMarkers = emptyList(),
            endSummary = null,
            isAdjustSheetVisible = false,
            adjustmentDraft = null,
            calibrationState = CalibrationState.NotReady,
            calibrationLabel = calibrationLabel(CalibrationState.NotReady, null),
            isLoading = false,
            isSessionSetupVisible = false,
            sessionSetup = SessionSetupUi(
                athleteOptions = emptyList(),
                selectedAthleteId = null,
                selectedDistanceM = 70,
                selectedArrowsPerEnd = 6,
                selectedTargetFace = "FACE_122CM",
                trainingGoal = "",
                canCreate = false,
            ),
            errorMessage = null,
        )

    companion object {
        private const val ACTIVE_SESSION_ROUTE = "active"
        private const val DEFAULT_TTS_LANGUAGE = "zh-TW"
        private const val DIGITAL_ZOOM_STEP = 0.25f
        private const val MAX_REQUESTED_ZOOM_RATIO = 8f
        private const val WAITING_FOR_CAMERA_LABEL = "Waiting for camera"
    }

    private fun calibrationLabel(state: CalibrationState, confidence: Float?): String =
        when (state) {
            CalibrationState.NotReady -> "Camera not ready"
            CalibrationState.NeedsCalibration -> "Needs calibration"
            CalibrationState.Calibrated -> {
                if (confidence != null) {
                    "Calibrated ${(confidence * 100f).roundToInt()}%"
                } else {
                    "Calibrated"
                }
            }
        }

    private fun recognitionLabel(
        isCameraReady: Boolean,
        isArmed: Boolean,
        calibrationState: CalibrationState,
    ): String =
        when {
            !isCameraReady -> getApplication<Application>().getString(R.string.recognition_camera_not_ready)
            isArmed -> getApplication<Application>().getString(R.string.recognition_active)
            calibrationState != CalibrationState.Calibrated -> getApplication<Application>().getString(R.string.recognition_waiting_calibration)
            else -> getApplication<Application>().getString(R.string.recognition_waiting_set)
        }

    private fun disarmRecognition(
        nextCalibrationState: CalibrationState,
        clearCalibrationLabel: Boolean,
    ) {
        _uiState.value = _uiState.value.copy(
            calibrationState = nextCalibrationState,
            calibrationLabel = if (clearCalibrationLabel) calibrationLabel(nextCalibrationState, null) else _uiState.value.calibrationLabel,
            isRecognitionArmed = false,
            recognitionLabel = recognitionLabel(
                isCameraReady = isCameraReady,
                isArmed = false,
                calibrationState = nextCalibrationState,
            ),
            analysisResetToken = _uiState.value.analysisResetToken + 1,
        )
    }

    private fun formatZoomLabel(
        profile: ResolvedCameraProfile?,
        zoomRatio: Float,
    ): String {
        val zoomText = zoomRatio.displayZoomRatio()
        return when (profile?.cameraMode) {
            "TELEPHOTO" -> "Telephoto $zoomText"
            "MAIN_ZOOM" -> "Main zoom $zoomText"
            else -> "Zoom $zoomText"
        }
    }

    private fun Float.displayZoomRatio(): String =
        buildString {
            append(roundToIntQuarter(this@displayZoomRatio))
            append('x')
        }

    private fun roundToIntQuarter(value: Float): String {
        val quarterSteps = (value * 4f).roundToInt()
        val wholePart = quarterSteps / 4
        val remainder = quarterSteps % 4
        return when (remainder) {
            0 -> wholePart.toString()
            1 -> "$wholePart.25"
            2 -> "$wholePart.5"
            3 -> "$wholePart.75"
            else -> value.toString()
        }
    }

    private fun SelectedShotUi.toAdjustmentDraft(): ShotAdjustmentUi =
        ShotAdjustmentUi(
            id = id,
            label = label,
            xNormalized = xNormalized,
            yNormalized = yNormalized,
            xMm = xMm,
            yMm = yMm,
            previewScoreValue = scoreValue,
            previewScoreLabel = scoreLabel,
            previewClockLabel = clockLabel,
            status = ShotStatus.Edited,
            isDirty = false,
        )

    private val ArrowShotEntity.effectiveScoreValue: Int
        get() = correctedScoreValue ?: scoreValue

    private val ArrowShotEntity.effectiveIsX: Boolean
        get() = correctedIsX ?: isX

    private val ArrowShotEntity.effectiveXMm: Float
        get() = correctedXMm ?: xMm

    private val ArrowShotEntity.effectiveYMm: Float
        get() = correctedYMm ?: yMm
}
