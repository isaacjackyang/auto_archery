package com.autoarchery.app.domain

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ShotScoreResult(
    val xMm: Float,
    val yMm: Float,
    val scoreValue: Int,
    val isX: Boolean,
)

enum class TargetScoringBandId {
    X,
    Score10,
    Score9,
    Score8,
    Score7,
    Score6,
    Score5,
    Score4,
    Score3,
    Score2,
    Score1,
    Miss,
}

data class TargetScoringBand(
    val id: TargetScoringBandId,
    val scoreValue: Int,
    val isX: Boolean,
    val outerRadiusRings: Float,
)

fun targetScoringBands(): List<TargetScoringBand> =
    listOf(
        TargetScoringBand(TargetScoringBandId.X, scoreValue = 10, isX = true, outerRadiusRings = 0.5f),
        TargetScoringBand(TargetScoringBandId.Score10, scoreValue = 10, isX = false, outerRadiusRings = 1f),
        TargetScoringBand(TargetScoringBandId.Score9, scoreValue = 9, isX = false, outerRadiusRings = 2f),
        TargetScoringBand(TargetScoringBandId.Score8, scoreValue = 8, isX = false, outerRadiusRings = 3f),
        TargetScoringBand(TargetScoringBandId.Score7, scoreValue = 7, isX = false, outerRadiusRings = 4f),
        TargetScoringBand(TargetScoringBandId.Score6, scoreValue = 6, isX = false, outerRadiusRings = 5f),
        TargetScoringBand(TargetScoringBandId.Score5, scoreValue = 5, isX = false, outerRadiusRings = 6f),
        TargetScoringBand(TargetScoringBandId.Score4, scoreValue = 4, isX = false, outerRadiusRings = 7f),
        TargetScoringBand(TargetScoringBandId.Score3, scoreValue = 3, isX = false, outerRadiusRings = 8f),
        TargetScoringBand(TargetScoringBandId.Score2, scoreValue = 2, isX = false, outerRadiusRings = 9f),
        TargetScoringBand(TargetScoringBandId.Score1, scoreValue = 1, isX = false, outerRadiusRings = 10f),
    )

fun targetScoringLegendBands(): List<TargetScoringBand> =
    targetScoringBands() + TargetScoringBand(
        id = TargetScoringBandId.Miss,
        scoreValue = 0,
        isX = false,
        outerRadiusRings = Float.POSITIVE_INFINITY,
    )

fun ringWidthMm(targetFaceCode: String): Float =
    when (targetFaceCode) {
        "FACE_40CM" -> 20f
        else -> 61f
    }

fun outerRadiusMm(targetFaceCode: String): Float = ringWidthMm(targetFaceCode) * 10f

fun normalizedToMm(normalized: Float, targetFaceCode: String): Float =
    normalized.coerceIn(-1f, 1f) * outerRadiusMm(targetFaceCode)

fun mmToNormalized(valueMm: Float, targetFaceCode: String): Float =
    (valueMm / outerRadiusMm(targetFaceCode)).coerceIn(-1f, 1f)

fun calculateShotScore(
    xMm: Float,
    yMm: Float,
    targetFaceCode: String,
    arrowDiameterMm: Float,
): ShotScoreResult {
    val ringWidth = ringWidthMm(targetFaceCode)
    val effectiveRadius = (distance(xMm, yMm) - arrowDiameterMm / 2f).coerceAtLeast(0f)
    val band = targetScoringBands().firstOrNull { scoringBand ->
        effectiveRadius <= scoringBand.outerRadiusRings * ringWidth
    }
    val score = band?.scoreValue ?: 0
    val isX = band?.isX == true
    return ShotScoreResult(
        xMm = xMm,
        yMm = yMm,
        scoreValue = score,
        isX = isX,
    )
}

fun clockHour(xMm: Float, yMm: Float): Int {
    if (abs(xMm) <= 0.001f && abs(yMm) <= 0.001f) return 12
    val angleDegrees = Math.toDegrees(atan2(xMm.toDouble(), (-yMm).toDouble()))
    val normalizedDegrees = (angleDegrees + 360.0) % 360.0
    val hourIndex = (normalizedDegrees / 30.0).roundToInt() % 12
    return if (hourIndex == 0) 12 else hourIndex
}

fun clockLabel(xMm: Float, yMm: Float): String = "${clockHour(xMm, yMm)} o'clock"

fun scoreLabel(scoreValue: Int, isX: Boolean): String =
    if (isX) "$scoreValue X" else scoreValue.toString()

fun distance(x: Float, y: Float): Float = sqrt(x.pow(2) + y.pow(2))
