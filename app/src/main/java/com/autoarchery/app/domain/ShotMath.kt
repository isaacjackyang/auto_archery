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

data class TargetFaceSpec(
    val code: String,
    val label: String,
    val ringWidthMm: Float,
    val visibleRingCount: Int,
    val sheetHalfExtentMm: Float = ringWidthMm * visibleRingCount,
    val spotCenters: List<TargetSpotCenter> = listOf(TargetSpotCenter(0f, 0f)),
)

data class TargetSpotCenter(
    val xMm: Float,
    val yMm: Float,
)

private val supportedTargetFaceSpecs = listOf(
    TargetFaceSpec(code = "FACE_122CM", label = "122 cm", ringWidthMm = 61f, visibleRingCount = 10),
    TargetFaceSpec(code = "FACE_80CM_10_RING", label = "80 cm (10-ring)", ringWidthMm = 40f, visibleRingCount = 10),
    TargetFaceSpec(code = "FACE_80CM_5_RING", label = "80 cm inner 5", ringWidthMm = 40f, visibleRingCount = 5),
    TargetFaceSpec(code = "FACE_60CM_6_RING", label = "60 cm inner 6", ringWidthMm = 30f, visibleRingCount = 6),
    TargetFaceSpec(code = "FACE_40CM", label = "40 cm (10-ring)", ringWidthMm = 20f, visibleRingCount = 10),
    TargetFaceSpec(code = "FACE_40CM_6_RING", label = "40 cm inner 6", ringWidthMm = 20f, visibleRingCount = 6),
    TargetFaceSpec(code = "FACE_20CM_5_RING", label = "20 cm inner 5", ringWidthMm = 20f, visibleRingCount = 5),
    // Assumes a standard 25 x 65 cm vertical 3-spot sheet centered on the middle spot.
    TargetFaceSpec(
        code = "FACE_20CM_5_RING_TRIPLE",
        label = "20 cm inner 5 (triple)",
        ringWidthMm = 20f,
        visibleRingCount = 5,
        sheetHalfExtentMm = 325f,
        spotCenters = listOf(
            TargetSpotCenter(0f, -212.5f),
            TargetSpotCenter(0f, 0f),
            TargetSpotCenter(0f, 212.5f),
        ),
    ),
)

private val bandIdsByRingIndex = listOf(
    TargetScoringBandId.Score10,
    TargetScoringBandId.Score9,
    TargetScoringBandId.Score8,
    TargetScoringBandId.Score7,
    TargetScoringBandId.Score6,
    TargetScoringBandId.Score5,
    TargetScoringBandId.Score4,
    TargetScoringBandId.Score3,
    TargetScoringBandId.Score2,
    TargetScoringBandId.Score1,
)

fun targetFaceSpec(targetFaceCode: String): TargetFaceSpec =
    supportedTargetFaceSpecs.firstOrNull { it.code == targetFaceCode }
        ?: supportedTargetFaceSpecs.first { it.code == "FACE_122CM" }

fun targetFaceLabel(targetFaceCode: String): String = targetFaceSpec(targetFaceCode).label

fun targetFaceSpecs(): List<TargetFaceSpec> = supportedTargetFaceSpecs

fun targetSpotCenters(targetFaceCode: String): List<TargetSpotCenter> =
    targetFaceSpec(targetFaceCode).spotCenters

fun targetScoringBands(targetFaceCode: String): List<TargetScoringBand> {
    val spec = targetFaceSpec(targetFaceCode)
    return buildList {
        add(TargetScoringBand(TargetScoringBandId.X, scoreValue = 10, isX = true, outerRadiusRings = 0.5f))
        repeat(spec.visibleRingCount) { index ->
            add(
                TargetScoringBand(
                    id = bandIdsByRingIndex[index],
                    scoreValue = 10 - index,
                    isX = false,
                    outerRadiusRings = (index + 1).toFloat(),
                ),
            )
        }
    }
}

fun targetScoringLegendBands(targetFaceCode: String): List<TargetScoringBand> =
    targetScoringBands(targetFaceCode) + TargetScoringBand(
        id = TargetScoringBandId.Miss,
        scoreValue = 0,
        isX = false,
        outerRadiusRings = Float.POSITIVE_INFINITY,
    )

fun ringWidthMm(targetFaceCode: String): Float =
    targetFaceSpec(targetFaceCode).ringWidthMm

fun outerRingCount(targetFaceCode: String): Int = targetFaceSpec(targetFaceCode).visibleRingCount

fun visibleSpotRadiusMm(targetFaceCode: String): Float = ringWidthMm(targetFaceCode) * outerRingCount(targetFaceCode)

fun outerRadiusMm(targetFaceCode: String): Float = visibleSpotRadiusMm(targetFaceCode)

fun normalizationHalfExtentMm(targetFaceCode: String): Float = targetFaceSpec(targetFaceCode).sheetHalfExtentMm

fun normalizedToMm(normalized: Float, targetFaceCode: String): Float =
    normalized.coerceIn(-1f, 1f) * normalizationHalfExtentMm(targetFaceCode)

fun mmToNormalized(valueMm: Float, targetFaceCode: String): Float =
    (valueMm / normalizationHalfExtentMm(targetFaceCode)).coerceIn(-1f, 1f)

fun calculateShotScore(
    xMm: Float,
    yMm: Float,
    targetFaceCode: String,
    arrowDiameterMm: Float,
): ShotScoreResult {
    val scoringBands = targetScoringBands(targetFaceCode)
    val ringWidth = ringWidthMm(targetFaceCode)
    val nearestSpot = nearestTargetSpotCenter(xMm, yMm, targetFaceCode)
    val relativeX = xMm - nearestSpot.xMm
    val relativeY = yMm - nearestSpot.yMm
    val effectiveRadius = (distance(relativeX, relativeY) - arrowDiameterMm / 2f).coerceAtLeast(0f)
    val band = scoringBands.firstOrNull { scoringBand ->
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

fun relativeToSpotCenter(
    xMm: Float,
    yMm: Float,
    targetFaceCode: String,
): TargetSpotCenter {
    val nearestSpot = nearestTargetSpotCenter(xMm, yMm, targetFaceCode)
    return TargetSpotCenter(
        xMm = xMm - nearestSpot.xMm,
        yMm = yMm - nearestSpot.yMm,
    )
}

fun clockHour(xMm: Float, yMm: Float): Int {
    if (abs(xMm) <= 0.001f && abs(yMm) <= 0.001f) return 12
    val angleDegrees = Math.toDegrees(atan2(xMm.toDouble(), (-yMm).toDouble()))
    val normalizedDegrees = (angleDegrees + 360.0) % 360.0
    val hourIndex = (normalizedDegrees / 30.0).roundToInt() % 12
    return if (hourIndex == 0) 12 else hourIndex
}

fun clockHour(
    xMm: Float,
    yMm: Float,
    targetFaceCode: String,
): Int {
    val relative = relativeToSpotCenter(xMm, yMm, targetFaceCode)
    return clockHour(relative.xMm, relative.yMm)
}

fun clockLabel(xMm: Float, yMm: Float): String = "${clockHour(xMm, yMm)} o'clock"

fun clockLabel(
    xMm: Float,
    yMm: Float,
    targetFaceCode: String,
): String = "${clockHour(xMm, yMm, targetFaceCode)} o'clock"

fun scoreLabel(scoreValue: Int, isX: Boolean): String =
    if (isX) "$scoreValue X" else scoreValue.toString()

fun distance(x: Float, y: Float): Float = sqrt(x.pow(2) + y.pow(2))

private fun nearestTargetSpotCenter(
    xMm: Float,
    yMm: Float,
    targetFaceCode: String,
): TargetSpotCenter =
    targetSpotCenters(targetFaceCode).minByOrNull { spot ->
        distance(xMm - spot.xMm, yMm - spot.yMm)
    } ?: TargetSpotCenter(0f, 0f)
