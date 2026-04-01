package com.autoarchery.app.data.repository

import android.os.Build
import com.autoarchery.app.data.local.dao.ArrowShotDao
import com.autoarchery.app.data.local.dao.CoachNoteDao
import com.autoarchery.app.data.local.dao.OperationLogDao
import com.autoarchery.app.data.local.dao.SessionExportDao
import com.autoarchery.app.data.local.dao.TrainingEndDao
import com.autoarchery.app.data.local.dao.TrainingSessionDao
import com.autoarchery.app.data.local.entity.ArrowShotEntity
import com.autoarchery.app.data.local.entity.CoachNoteEntity
import com.autoarchery.app.data.local.entity.OperationLogEntity
import com.autoarchery.app.data.local.entity.TrainingEndEntity
import com.autoarchery.app.data.local.entity.TrainingSessionEntity
import com.autoarchery.app.domain.ShotScoreResult
import com.autoarchery.app.domain.calculateShotScore
import com.autoarchery.app.domain.clockHour
import com.autoarchery.app.domain.distance
import com.autoarchery.app.domain.mmToNormalized as domainMmToNormalized
import com.autoarchery.app.domain.normalizedToMm as domainNormalizedToMm
import com.autoarchery.app.domain.outerRadiusMm as domainOuterRadiusMm
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import java.util.UUID

data class SessionSetupInput(
    val athleteId: String,
    val athleteName: String,
    val distanceM: Int,
    val arrowsPerEnd: Int,
    val targetFaceCode: String,
    val trainingGoal: String,
)

data class SessionBundle(
    val session: TrainingSessionEntity,
    val ends: List<TrainingEndEntity>,
    val currentEnd: TrainingEndEntity,
    val arrows: List<ArrowShotEntity>,
)

data class SessionDetailBundle(
    val session: TrainingSessionEntity,
    val ends: List<TrainingEndEntity>,
    val arrowsByEnd: Map<String, List<ArrowShotEntity>>,
    val notes: List<CoachNoteEntity>,
    val comparisonSession: TrainingSessionEntity?,
)

data class SessionCalibrationInput(
    val centerXPx: Float,
    val centerYPx: Float,
    val radiusXPx: Float,
    val radiusYPx: Float,
    val rotationDeg: Float,
    val confidence: Float,
)

data class RecordedShotResult(
    val shotId: String,
    val scoreValue: Int,
    val isX: Boolean,
    val clockHour: Int,
    val xMm: Float,
    val yMm: Float,
    val confidence: Float,
)

class SessionRepository(
    private val trainingSessionDao: TrainingSessionDao,
    private val trainingEndDao: TrainingEndDao,
    private val arrowShotDao: ArrowShotDao,
    private val coachNoteDao: CoachNoteDao,
    private val operationLogDao: OperationLogDao,
    private val sessionExportDao: SessionExportDao,
) {
    suspend fun getLatestSession(): TrainingSessionEntity? = trainingSessionDao.getLatest()

    suspend fun getSession(sessionId: String): TrainingSessionEntity? = trainingSessionDao.getById(sessionId)

    suspend fun getRecentSessions(limit: Int = 5): List<TrainingSessionEntity> =
        trainingSessionDao.getAll().take(limit)

    suspend fun getBundle(sessionId: String): SessionBundle? {
        val session = trainingSessionDao.getById(sessionId) ?: return null
        val ends = trainingEndDao.getForSession(sessionId)
        val currentEnd = ends.lastOrNull { it.status != "COMPLETED" } ?: ends.lastOrNull() ?: return null
        val arrows = arrowShotDao.getForEnd(currentEnd.id)
        return SessionBundle(
            session = session,
            ends = ends,
            currentEnd = currentEnd,
            arrows = arrows,
        )
    }

    suspend fun getSessionDetail(sessionId: String): SessionDetailBundle? {
        val session = trainingSessionDao.getById(sessionId) ?: return null
        val ends = trainingEndDao.getForSession(sessionId)
        val arrowsByEnd = ends.associate { trainingEnd ->
            trainingEnd.id to arrowShotDao.getForEnd(trainingEnd.id)
        }
        return SessionDetailBundle(
            session = session,
            ends = ends,
            arrowsByEnd = arrowsByEnd,
            notes = coachNoteDao.getForSession(sessionId),
            comparisonSession = trainingSessionDao.getPreviousForAthlete(session.athleteId, session.id),
        )
    }

    suspend fun createSession(input: SessionSetupInput): String {
        val now = System.currentTimeMillis()
        val sessionId = UUID.randomUUID().toString()
        val previousSession = trainingSessionDao.getLatestForAthlete(input.athleteId)
        val session = TrainingSessionEntity(
            id = sessionId,
            athleteId = input.athleteId,
            title = "${input.distanceM}m ${input.athleteName}",
            athleteName = input.athleteName,
            startedAt = now,
            distanceM = input.distanceM,
            targetFaceCode = input.targetFaceCode,
            arrowsPerEnd = input.arrowsPerEnd,
            arrowDiameterMm = 5.5f,
            trainingGoal = input.trainingGoal.ifBlank { null },
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            cameraMode = "PENDING",
            comparisonBaselineSessionId = previousSession?.id,
            createdAt = now,
            updatedAt = now,
        )
        val firstEnd = TrainingEndEntity(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            endIndex = 1,
            startedAt = now,
            status = "ACTIVE",
            expectedArrowCount = input.arrowsPerEnd,
            detectedArrowCount = 0,
            subtotalScore = 0,
            xCount = 0,
            tenCount = 0,
            createdAt = now,
            updatedAt = now,
        )
        trainingSessionDao.upsert(session)
        trainingEndDao.upsert(firstEnd)
        return sessionId
    }

    suspend fun updateCameraProfile(
        sessionId: String,
        cameraMode: String,
        cameraLensId: String?,
    ) {
        val session = trainingSessionDao.getById(sessionId) ?: return
        if (session.cameraMode == cameraMode && session.cameraLensId == cameraLensId) return
        trainingSessionDao.upsert(
            session.copy(
                cameraMode = cameraMode,
                cameraLensId = cameraLensId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateCalibration(
        sessionId: String,
        calibration: SessionCalibrationInput,
    ) {
        val session = trainingSessionDao.getById(sessionId) ?: return
        trainingSessionDao.upsert(
            session.copy(
                calibrationCenterXPx = calibration.centerXPx,
                calibrationCenterYPx = calibration.centerYPx,
                calibrationRadiusXPx = calibration.radiusXPx,
                calibrationRadiusYPx = calibration.radiusYPx,
                calibrationRotationDeg = calibration.rotationDeg,
                calibrationConfidence = calibration.confidence,
                calibrationUpdatedAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun manualAddShot(sessionId: String): String? {
        val bundle = getBundle(sessionId) ?: return null
        if (bundle.arrows.size >= bundle.currentEnd.expectedArrowCount) return null

        val nextIndex = bundle.arrows.size + 1
        val now = System.currentTimeMillis()
        val shotId = UUID.randomUUID().toString()
        val xMm = (-6 + nextIndex * 2).toFloat()
        val yMm = (nextIndex * 3 - 8).toFloat()
        val score = calculateShotScore(
            xMm = xMm,
            yMm = yMm,
            targetFaceCode = bundle.session.targetFaceCode,
            arrowDiameterMm = bundle.session.arrowDiameterMm,
        )
        val shot = ArrowShotEntity(
            id = shotId,
            endId = bundle.currentEnd.id,
            arrowIndex = nextIndex,
            detectedAt = now,
            scoreValue = score.scoreValue,
            isX = score.isX,
            clockHour = clockHour(xMm, yMm),
            angleDeg = angleDeg(xMm, yMm),
            radiusMm = distance(xMm, yMm),
            xMm = xMm,
            yMm = yMm,
            confidence = 0.72f,
            autoScoreValue = score.scoreValue,
            autoIsX = score.isX,
            autoXMm = xMm,
            autoYMm = yMm,
            resultStatus = "REVIEW_REQUIRED",
            inputMethod = "MANUAL_ADD",
            announceText = "${clockHour(xMm, yMm)} o'clock, ${score.scoreValue}",
        )
        arrowShotDao.upsert(shot)
        operationLogDao.insert(
            OperationLogEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                endId = bundle.currentEnd.id,
                arrowShotId = shotId,
                operationType = "MANUAL_ADD",
                createdAt = now,
            ),
        )
        refreshEndAndSessionStats(sessionId)
        return shotId
    }

    suspend fun recordDetectedShot(
        sessionId: String,
        xNormalized: Float,
        yNormalized: Float,
        confidence: Float,
    ): RecordedShotResult? {
        val bundle = getBundle(sessionId) ?: return null
        if (bundle.arrows.size >= bundle.currentEnd.expectedArrowCount) return null

        val xMm = domainNormalizedToMm(xNormalized, bundle.session.targetFaceCode)
        val yMm = domainNormalizedToMm(yNormalized, bundle.session.targetFaceCode)
        val isDuplicate = bundle.arrows.any { arrow ->
            distance(arrow.effectiveXMm - xMm, arrow.effectiveYMm - yMm) <= bundle.session.arrowDiameterMm
        }
        if (isDuplicate) return null

        val score = calculateShotScore(
            xMm = xMm,
            yMm = yMm,
            targetFaceCode = bundle.session.targetFaceCode,
            arrowDiameterMm = bundle.session.arrowDiameterMm,
        )
        val nextIndex = bundle.arrows.size + 1
        val now = System.currentTimeMillis()
        val shotId = UUID.randomUUID().toString()
        val shot = ArrowShotEntity(
            id = shotId,
            endId = bundle.currentEnd.id,
            arrowIndex = nextIndex,
            detectedAt = now,
            scoreValue = score.scoreValue,
            isX = score.isX,
            clockHour = clockHour(xMm, yMm),
            angleDeg = angleDeg(xMm, yMm),
            radiusMm = distance(xMm, yMm),
            xMm = xMm,
            yMm = yMm,
            confidence = confidence,
            autoScoreValue = score.scoreValue,
            autoIsX = score.isX,
            autoXMm = xMm,
            autoYMm = yMm,
            resultStatus = "REVIEW_REQUIRED",
            inputMethod = "AUTO_DETECTED",
            announceText = "${clockHour(xMm, yMm)} o'clock, ${score.scoreValue}",
        )
        arrowShotDao.upsert(shot)
        operationLogDao.insert(
            OperationLogEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                endId = bundle.currentEnd.id,
                arrowShotId = shotId,
                operationType = "AUTO_DETECTED",
                createdAt = now,
            ),
        )
        refreshEndAndSessionStats(sessionId)
        return RecordedShotResult(
            shotId = shotId,
            scoreValue = score.scoreValue,
            isX = score.isX,
            clockHour = clockHour(xMm, yMm),
            xMm = xMm,
            yMm = yMm,
            confidence = confidence,
        )
    }

    suspend fun deleteLastShot(sessionId: String) {
        val bundle = getBundle(sessionId) ?: return
        val shot = bundle.arrows.lastOrNull() ?: return
        val now = System.currentTimeMillis()
        arrowShotDao.upsert(
            shot.copy(
                isDeleted = true,
                deletedAt = now,
            ),
        )
        operationLogDao.insert(
            OperationLogEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                endId = bundle.currentEnd.id,
                arrowShotId = shot.id,
                operationType = "DELETE_LAST",
                createdAt = now,
            ),
        )
        refreshEndAndSessionStats(sessionId)
    }

    suspend fun updateShotStatus(shotId: String, status: String, isEdited: Boolean = false) {
        val shot = arrowShotDao.getById(shotId) ?: return
        arrowShotDao.upsert(
            shot.copy(
                resultStatus = status,
                isManuallyCorrected = shot.isManuallyCorrected || isEdited,
                confirmedAt = if (status == "CONFIRMED" || status == "CORRECTED") System.currentTimeMillis() else shot.confirmedAt,
            ),
        )
        val end = trainingEndDao.getById(shot.endId) ?: return
        refreshEndAndSessionStats(end.sessionId)
    }

    suspend fun applyShotCorrection(
        shotId: String,
        correctedXMm: Float,
        correctedYMm: Float,
    ) {
        val shot = arrowShotDao.getById(shotId) ?: return
        val end = trainingEndDao.getById(shot.endId) ?: return
        val session = trainingSessionDao.getById(end.sessionId) ?: return
        val now = System.currentTimeMillis()
        val correctedResult = calculateShotCorrection(
            xMm = correctedXMm,
            yMm = correctedYMm,
            targetFaceCode = session.targetFaceCode,
            arrowDiameterMm = session.arrowDiameterMm,
        )
        arrowShotDao.upsert(
            shot.copy(
                correctedScoreValue = correctedResult.scoreValue,
                correctedIsX = correctedResult.isX,
                correctedXMm = correctedResult.xMm,
                correctedYMm = correctedResult.yMm,
                isManuallyCorrected = true,
                resultStatus = "CORRECTED",
                confirmedAt = now,
                announceText = "${clockHour(correctedXMm, correctedYMm)} o'clock, ${correctedResult.scoreValue}",
            ),
        )
        operationLogDao.insert(
            OperationLogEntity(
                id = UUID.randomUUID().toString(),
                sessionId = session.id,
                endId = end.id,
                arrowShotId = shot.id,
                operationType = "ADJUST_POINT",
                createdAt = now,
            ),
        )
        refreshEndAndSessionStats(session.id)
    }

    suspend fun confirmHighConfidenceShots(sessionId: String) {
        val bundle = getBundle(sessionId) ?: return
        val now = System.currentTimeMillis()
        bundle.arrows
            .filter { it.resultStatus == "REVIEW_REQUIRED" && it.confidence >= 0.7f }
            .forEach { shot ->
                arrowShotDao.upsert(
                    shot.copy(
                        resultStatus = "CONFIRMED",
                        confirmedAt = now,
                    ),
                )
            }

        operationLogDao.insert(
            OperationLogEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                endId = bundle.currentEnd.id,
                operationType = "CONFIRM_ALL_HIGH_CONFIDENCE",
                createdAt = now,
            ),
        )
        refreshEndAndSessionStats(sessionId)
    }

    suspend fun confirmCurrentEnd(sessionId: String) {
        val bundle = getBundle(sessionId) ?: return
        val now = System.currentTimeMillis()
        trainingEndDao.upsert(
            bundle.currentEnd.copy(
                status = "COMPLETED",
                completedAt = now,
                updatedAt = now,
            ),
        )
        trainingEndDao.upsert(
            TrainingEndEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                endIndex = bundle.currentEnd.endIndex + 1,
                startedAt = now,
                status = "ACTIVE",
                expectedArrowCount = bundle.session.arrowsPerEnd,
                detectedArrowCount = 0,
                subtotalScore = 0,
                xCount = 0,
                tenCount = 0,
                createdAt = now,
                updatedAt = now,
            ),
        )
        refreshEndAndSessionStats(sessionId)
    }

    private suspend fun refreshEndAndSessionStats(sessionId: String) {
        val bundle = getBundle(sessionId) ?: return
        val subtotal = bundle.arrows.sumOf { it.effectiveScoreValue }
        val tenCount = bundle.arrows.count { it.effectiveScoreValue == 10 }
        val xCount = bundle.arrows.count { it.effectiveIsX }
        val endMetrics = computeEndMetrics(bundle.arrows, bundle.session.targetFaceCode)
        val updatedEnd = bundle.currentEnd.copy(
            detectedArrowCount = bundle.arrows.size,
            subtotalScore = subtotal,
            tenCount = tenCount,
            xCount = xCount,
            groupCtcMm = endMetrics.groupCtcMm,
            accuracyOffsetMm = endMetrics.accuracyOffsetMm,
            precisionMeanRadiusMm = endMetrics.precisionMeanRadiusMm,
            cpkX = endMetrics.cpkX,
            cpkY = endMetrics.cpkY,
            reviewRequired = bundle.arrows.any { it.resultStatus == "REVIEW_REQUIRED" },
            updatedAt = System.currentTimeMillis(),
        )
        trainingEndDao.upsert(updatedEnd)

        val session = bundle.session
        val allEnds = trainingEndDao.getForSession(sessionId)
        val totalScore = allEnds.sumOf { it.subtotalScore }
        val totalArrows = allEnds.sumOf { it.detectedArrowCount }
        val avgScore = if (totalArrows == 0) 0f else totalScore.toFloat() / totalArrows.toFloat()
        trainingSessionDao.upsert(
            session.copy(
                totalEnds = allEnds.count { it.status == "COMPLETED" },
                totalArrows = totalArrows,
                totalScore = totalScore,
                avgScore = avgScore,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    private fun computeEndMetrics(
        arrows: List<ArrowShotEntity>,
        targetFaceCode: String,
    ): ComputedEndMetrics {
        if (arrows.isEmpty()) {
            return ComputedEndMetrics()
        }

        val points = arrows.map { it.effectiveXMm to it.effectiveYMm }
        val centerX = points.map { it.first }.average().toFloat()
        val centerY = points.map { it.second }.average().toFloat()
        val meanRadius = points
            .map { (x, y) -> distance(x - centerX, y - centerY) }
            .average()
            .toFloat()
        val maxDistance = points
            .flatMapIndexed { index, start ->
                points.drop(index + 1).map { end -> distance(start.first - end.first, start.second - end.second) }
            }
            .maxOrNull()

        val xValues = points.map { it.first }
        val yValues = points.map { it.second }
        val specLimitMm = domainOuterRadiusMm(targetFaceCode) / 10f

        return ComputedEndMetrics(
            groupCtcMm = maxDistance,
            accuracyOffsetMm = distance(centerX, centerY),
            precisionMeanRadiusMm = meanRadius,
            cpkX = calculateCpk(xValues, specLimitMm),
            cpkY = calculateCpk(yValues, specLimitMm),
        )
    }

    private fun calculateCpk(values: List<Float>, specLimit: Float): Float? {
        if (values.size < 2) return null

        val mean = values.average().toFloat()
        val variance = values
            .map { (it - mean).pow(2) }
            .average()
            .toFloat()
        val standardDeviation = sqrt(variance)
        if (standardDeviation <= 0.0001f) {
            return if (abs(mean) <= specLimit) 2f else 0f
        }

        return ((specLimit - abs(mean)) / (3f * standardDeviation)).coerceAtLeast(0f)
    }

    fun calculateShotCorrection(
        xMm: Float,
        yMm: Float,
        targetFaceCode: String,
        arrowDiameterMm: Float,
    ): ShotScoreResult = calculateShotScore(
        xMm = xMm,
        yMm = yMm,
        targetFaceCode = targetFaceCode,
        arrowDiameterMm = arrowDiameterMm,
    )

    fun outerRadiusMm(targetFaceCode: String): Float = domainOuterRadiusMm(targetFaceCode)

    fun normalizedToMm(value: Float, targetFaceCode: String): Float = domainNormalizedToMm(value, targetFaceCode)

    fun mmToNormalized(value: Float, targetFaceCode: String): Float = domainMmToNormalized(value, targetFaceCode)

    private fun angleDeg(xMm: Float, yMm: Float): Float =
        Math.toDegrees(atan2(yMm.toDouble(), xMm.toDouble())).toFloat()

    private data class ComputedEndMetrics(
        val groupCtcMm: Float? = null,
        val accuracyOffsetMm: Float? = null,
        val precisionMeanRadiusMm: Float? = null,
        val cpkX: Float? = null,
        val cpkY: Float? = null,
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
