package com.autoarchery.app.vision

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.autoarchery.app.domain.distance
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class ResolvedCameraProfile(
    val cameraMode: String,
    val cameraLensId: String?,
    val zoomRatio: Float,
    val zoomLabel: String,
)

data class TargetCalibrationResult(
    val centerXPx: Float,
    val centerYPx: Float,
    val radiusXPx: Float,
    val radiusYPx: Float,
    val rotationDeg: Float,
    val confidence: Float,
)

data class DetectedShotResult(
    val xNormalized: Float,
    val yNormalized: Float,
    val confidence: Float,
)

class TargetVisionAnalyzer : ImageAnalysis.Analyzer {
    @Volatile
    var sessionId: String? = null

    @Volatile
    var isPaused: Boolean = false

    @Volatile
    var knownShotCount: Int = 0

    @Volatile
    var analysisResetToken: Int = 0

    @Volatile
    var onCalibrationDetected: ((TargetCalibrationResult) -> Unit)? = null

    @Volatile
    var onShotDetected: ((DetectedShotResult) -> Unit)? = null

    private var lastSessionId: String? = null
    private var lastAnalysisResetToken: Int = 0
    private var lastKnownShotCount: Int = 0
    private var frameCounter: Int = 0
    private var calibration: TargetCalibrationResult? = null
    private var baselineState: BaselineState? = null
    private var pendingCandidate: StableShotCandidate? = null
    private var awaitingShotCommitFrames: Int = 0

    override fun analyze(image: ImageProxy) {
        try {
            val activeSessionId = sessionId ?: return
            if (activeSessionId != lastSessionId) {
                resetState(activeSessionId)
            }
            if (analysisResetToken != lastAnalysisResetToken) {
                lastAnalysisResetToken = analysisResetToken
                resetAnalysisState()
            }

            frameCounter += 1
            if (frameCounter % 2 != 0) return

            val sourceWidth = min(image.width, 160)
            val sourceHeight = min(image.height, 120)
            val lumaFrame = sampleLumaFrame(image, sourceWidth, sourceHeight)

            val detectedCalibration = detectCalibration(lumaFrame, sourceWidth, sourceHeight)
            if (detectedCalibration != null && shouldAcceptCalibration(detectedCalibration)) {
                calibration = detectedCalibration
                resetBaselineTracking()
                lastKnownShotCount = knownShotCount
                onCalibrationDetected?.invoke(detectedCalibration)
            }

            val activeCalibration = calibration ?: return
            if (knownShotCount != lastKnownShotCount) {
                pendingCandidate = null
                awaitingShotCommitFrames = 0
                if (baselineState?.shotCount != knownShotCount) {
                    baselineState = null
                }
                lastKnownShotCount = knownShotCount
            }

            if (isPaused) return

            val normalizedFrame = buildNormalizedTarget(lumaFrame, sourceWidth, sourceHeight, activeCalibration)
            val activeBaseline = resolveActiveBaseline(normalizedFrame)
            if (activeBaseline == null) {
                return
            }

            if (activeBaseline.shotCount > knownShotCount) {
                if (awaitingShotCommitFrames > 0) {
                    awaitingShotCommitFrames -= 1
                } else {
                    baselineState = captureBaseline(normalizedFrame, knownShotCount)
                }
                return
            }

            val candidate = detectIncrementalChange(activeBaseline.normalizedFrame, normalizedFrame)
            if (candidate == null) {
                pendingCandidate = null
                baselineState = activeBaseline.refresh(normalizedFrame, alpha = BASELINE_BLEND_ALPHA)
                return
            }

            val stableCandidate = pendingCandidate?.takeIf { it.isNear(candidate) }?.let { existing ->
                existing.copy(
                    xNormalized = candidate.xNormalized,
                    yNormalized = candidate.yNormalized,
                    confidence = maxOf(existing.confidence, candidate.confidence),
                    stableFrames = existing.stableFrames + 1,
                )
            } ?: StableShotCandidate(
                xNormalized = candidate.xNormalized,
                yNormalized = candidate.yNormalized,
                confidence = candidate.confidence,
                stableFrames = 1,
            )
            pendingCandidate = stableCandidate

            if (stableCandidate.stableFrames >= 2) {
                onShotDetected?.invoke(
                    DetectedShotResult(
                        xNormalized = stableCandidate.xNormalized,
                        yNormalized = stableCandidate.yNormalized,
                        confidence = stableCandidate.confidence,
                    ),
                )
                baselineState = captureBaseline(normalizedFrame, knownShotCount + 1)
                pendingCandidate = null
                awaitingShotCommitFrames = SHOT_COMMIT_GRACE_FRAMES
            }
        } finally {
            image.close()
        }
    }

    private fun resetState(activeSessionId: String) {
        lastSessionId = activeSessionId
        lastAnalysisResetToken = analysisResetToken
        resetAnalysisState()
    }

    private fun resetAnalysisState() {
        lastKnownShotCount = knownShotCount
        frameCounter = 0
        calibration = null
        baselineState = null
        pendingCandidate = null
        awaitingShotCommitFrames = 0
    }

    private fun resetBaselineTracking() {
        baselineState = null
        pendingCandidate = null
        awaitingShotCommitFrames = 0
    }

    private fun resolveActiveBaseline(normalizedFrame: FloatArray): BaselineState? {
        val currentBaseline = baselineState
        if (currentBaseline == null || currentBaseline.shotCount < knownShotCount) {
            baselineState = captureBaseline(normalizedFrame, knownShotCount)
            return null
        }
        return currentBaseline
    }

    private fun captureBaseline(
        normalizedFrame: FloatArray,
        shotCount: Int,
    ): BaselineState = BaselineState(
        shotCount = shotCount,
        kind = if (shotCount == 0) BaselineKind.Initial else BaselineKind.Incremental,
        normalizedFrame = normalizedFrame,
    )

    private fun shouldAcceptCalibration(candidate: TargetCalibrationResult): Boolean {
        val current = calibration ?: return candidate.confidence >= 0.45f
        val centerDelta = distance(candidate.centerXPx - current.centerXPx, candidate.centerYPx - current.centerYPx)
        val radiusDelta = abs(candidate.radiusXPx - current.radiusXPx) + abs(candidate.radiusYPx - current.radiusYPx)
        return candidate.confidence > current.confidence + 0.03f || centerDelta > 6f || radiusDelta > 8f
    }

    private fun sampleLumaFrame(image: ImageProxy, targetWidth: Int, targetHeight: Int): FloatArray {
        val plane = image.planes.first()
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val widthScale = image.width.toFloat() / targetWidth.toFloat()
        val heightScale = image.height.toFloat() / targetHeight.toFloat()
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return FloatArray(targetWidth * targetHeight) { index ->
            val x = index % targetWidth
            val y = index / targetWidth
            val sourceX = (x * widthScale).toInt().coerceIn(0, image.width - 1)
            val sourceY = (y * heightScale).toInt().coerceIn(0, image.height - 1)
            val planeIndex = sourceY * rowStride + sourceX * pixelStride
            (bytes[planeIndex].toInt() and 0xFF).toFloat()
        }
    }

    private fun detectCalibration(
        luma: FloatArray,
        width: Int,
        height: Int,
    ): TargetCalibrationResult? {
        var centerX = width / 2f
        var centerY = height / 2f

        repeat(2) {
            val boundaryPoints = sampleBoundaryPoints(luma, width, height, centerX, centerY)
            if (boundaryPoints.size < 18) {
                return null
            }
            val ellipse = fitEllipse(boundaryPoints) ?: return null
            centerX = ellipse.centerXPx
            centerY = ellipse.centerYPx
        }

        return fitEllipse(sampleBoundaryPoints(luma, width, height, centerX, centerY))
    }

    private fun sampleBoundaryPoints(
        luma: FloatArray,
        width: Int,
        height: Int,
        centerX: Float,
        centerY: Float,
    ): List<Pair<Float, Float>> {
        val points = mutableListOf<Pair<Float, Float>>()
        val minRadius = min(width, height) * 0.18f
        val maxRadius = min(width, height) * 0.48f
        for (step in 0 until 36) {
            val angle = step * 10f * PI.toFloat() / 180f
            var bestRadius = -1f
            var bestGradient = 0f
            var previousLuma = 0f
            var initialized = false
            var radius = minRadius
            while (radius <= maxRadius) {
                val x = (centerX + cos(angle) * radius).toInt().coerceIn(0, width - 1)
                val y = (centerY + sin(angle) * radius).toInt().coerceIn(0, height - 1)
                val currentLuma = luma[y * width + x]
                if (initialized) {
                    val gradient = abs(currentLuma - previousLuma)
                    if (gradient > bestGradient) {
                        bestGradient = gradient
                        bestRadius = radius
                    }
                }
                previousLuma = currentLuma
                initialized = true
                radius += 1f
            }

            if (bestRadius > 0f && bestGradient >= 8f) {
                points += (centerX + cos(angle) * bestRadius) to (centerY + sin(angle) * bestRadius)
            }
        }
        return points
    }

    private fun fitEllipse(points: List<Pair<Float, Float>>): TargetCalibrationResult? {
        if (points.size < 18) return null

        val centerX = points.map { it.first }.average().toFloat()
        val centerY = points.map { it.second }.average().toFloat()
        val centered = points.map { (x, y) -> (x - centerX) to (y - centerY) }
        val covXX = centered.map { it.first * it.first }.average().toFloat()
        val covYY = centered.map { it.second * it.second }.average().toFloat()
        val covXY = centered.map { it.first * it.second }.average().toFloat()
        val rotation = 0.5f * atan2(2f * covXY, covXX - covYY)
        val cosTheta = cos(rotation)
        val sinTheta = sin(rotation)
        val rotated = centered.map { (x, y) ->
            val xr = x * cosTheta + y * sinTheta
            val yr = -x * sinTheta + y * cosTheta
            xr to yr
        }
        val radiusX = sqrt(2f * rotated.map { it.first * it.first }.average().toFloat())
        val radiusY = sqrt(2f * rotated.map { it.second * it.second }.average().toFloat())
        if (radiusX < 12f || radiusY < 12f) return null

        val normalizedErrors = rotated.map { (x, y) ->
            abs((x.pow(2) / radiusX.pow(2)) + (y.pow(2) / radiusY.pow(2)) - 1f)
        }
        val meanError = normalizedErrors.average().toFloat()
        val confidence = ((points.size / 36f) * (1f - meanError.coerceIn(0f, 1f))).coerceIn(0f, 0.99f)
        if (confidence < 0.35f) return null

        return TargetCalibrationResult(
            centerXPx = centerX,
            centerYPx = centerY,
            radiusXPx = radiusX,
            radiusYPx = radiusY,
            rotationDeg = Math.toDegrees(rotation.toDouble()).toFloat(),
            confidence = confidence,
        )
    }

    private fun buildNormalizedTarget(
        luma: FloatArray,
        width: Int,
        height: Int,
        calibration: TargetCalibrationResult,
    ): FloatArray {
        val gridSize = NORMALIZED_GRID_SIZE
        val output = FloatArray(gridSize * gridSize)
        val rotationRad = Math.toRadians(calibration.rotationDeg.toDouble()).toFloat()
        val cosTheta = cos(rotationRad)
        val sinTheta = sin(rotationRad)
        val center = (gridSize - 1) / 2f
        val radius = center

        for (y in 0 until gridSize) {
            val normalizedY = (y - center) / radius
            for (x in 0 until gridSize) {
                val normalizedX = (x - center) / radius
                val imageX = calibration.centerXPx +
                    (normalizedX * calibration.radiusXPx * cosTheta) -
                    (normalizedY * calibration.radiusYPx * sinTheta)
                val imageY = calibration.centerYPx +
                    (normalizedX * calibration.radiusXPx * sinTheta) +
                    (normalizedY * calibration.radiusYPx * cosTheta)
                output[y * gridSize + x] = bilinearSample(luma, width, height, imageX, imageY)
            }
        }
        return output
    }

    private fun detectIncrementalChange(
        reference: FloatArray,
        current: FloatArray,
    ): DetectedShotResult? {
        val gridSize = NORMALIZED_GRID_SIZE
        val darkDiff = FloatArray(reference.size)
        for (index in reference.indices) {
            darkDiff[index] = (reference[index] - current[index]).coerceAtLeast(0f)
        }

        val visited = BooleanArray(reference.size)
        var bestComponent: ShotComponent? = null
        val center = (gridSize - 1) / 2f
        val radius = center

        for (index in darkDiff.indices) {
            if (visited[index]) continue
            if (darkDiff[index] < 18f) continue

            val startX = index % gridSize
            val startY = index / gridSize
            val queue = ArrayDeque<Int>()
            queue.add(index)
            visited[index] = true
            val componentPixels = mutableListOf<Int>()
            var minRadius = Float.MAX_VALUE
            var maxRadius = 0f
            var darkest = 0f
            var impactX = startX
            var impactY = startY

            while (queue.isNotEmpty()) {
                val currentIndex = queue.removeFirst()
                componentPixels += currentIndex
                val x = currentIndex % gridSize
                val y = currentIndex / gridSize
                val normalizedX = (x - center) / radius
                val normalizedY = (y - center) / radius
                val pointRadius = sqrt(normalizedX.pow(2) + normalizedY.pow(2))
                if (pointRadius > 1.08f) continue

                val diff = darkDiff[currentIndex]
                if (pointRadius < minRadius) {
                    minRadius = pointRadius
                    impactX = x
                    impactY = y
                }
                if (pointRadius > maxRadius) {
                    maxRadius = pointRadius
                }
                darkest += diff

                for (neighbor in neighbors(currentIndex, gridSize)) {
                    if (visited[neighbor]) continue
                    if (darkDiff[neighbor] < 18f) continue
                    visited[neighbor] = true
                    queue.add(neighbor)
                }
            }

            if (componentPixels.size < 8) continue
            val radialSpan = maxRadius - minRadius
            if (maxRadius < 0.70f || radialSpan < 0.14f) continue

            val confidence = (
                0.35f +
                    (componentPixels.size / 140f) +
                    (darkest / (componentPixels.size * 70f)) +
                    radialSpan
                ).coerceIn(0f, 0.98f)

            val candidate = ShotComponent(
                xNormalized = ((impactX - center) / radius).coerceIn(-1f, 1f),
                yNormalized = ((impactY - center) / radius).coerceIn(-1f, 1f),
                confidence = confidence,
            )
            if (bestComponent == null || candidate.confidence > bestComponent.confidence) {
                bestComponent = candidate
            }
        }

        return bestComponent?.let {
            DetectedShotResult(
                xNormalized = it.xNormalized,
                yNormalized = it.yNormalized,
                confidence = it.confidence,
            )
        }
    }

    private fun bilinearSample(
        luma: FloatArray,
        width: Int,
        height: Int,
        x: Float,
        y: Float,
    ): Float {
        val x0 = x.toInt().coerceIn(0, width - 1)
        val y0 = y.toInt().coerceIn(0, height - 1)
        val x1 = (x0 + 1).coerceIn(0, width - 1)
        val y1 = (y0 + 1).coerceIn(0, height - 1)
        val dx = x - x0
        val dy = y - y0

        val top = luma[y0 * width + x0] * (1f - dx) + luma[y0 * width + x1] * dx
        val bottom = luma[y1 * width + x0] * (1f - dx) + luma[y1 * width + x1] * dx
        return top * (1f - dy) + bottom * dy
    }

    private fun neighbors(index: Int, gridSize: Int): List<Int> {
        val x = index % gridSize
        val y = index / gridSize
        val result = ArrayList<Int>(8)
        for (deltaY in -1..1) {
            for (deltaX in -1..1) {
                if (deltaX == 0 && deltaY == 0) continue
                val nextX = x + deltaX
                val nextY = y + deltaY
                if (nextX in 0 until gridSize && nextY in 0 until gridSize) {
                    result += nextY * gridSize + nextX
                }
            }
        }
        return result
    }

    private data class StableShotCandidate(
        val xNormalized: Float,
        val yNormalized: Float,
        val confidence: Float,
        val stableFrames: Int,
    ) {
        fun isNear(other: DetectedShotResult): Boolean =
            distance(xNormalized - other.xNormalized, yNormalized - other.yNormalized) <= 0.08f
    }

    private data class ShotComponent(
        val xNormalized: Float,
        val yNormalized: Float,
        val confidence: Float,
    )

    private data class BaselineState(
        val shotCount: Int,
        val kind: BaselineKind,
        val normalizedFrame: FloatArray,
    ) {
        fun refresh(
            currentFrame: FloatArray,
            alpha: Float,
        ): BaselineState = copy(
            normalizedFrame = FloatArray(normalizedFrame.size) { index ->
                val effectiveAlpha = when (kind) {
                    BaselineKind.Initial -> alpha * INITIAL_BASELINE_ADAPT_FACTOR
                    BaselineKind.Incremental -> alpha
                }
                (normalizedFrame[index] * (1f - effectiveAlpha)) + (currentFrame[index] * effectiveAlpha)
            },
        )
    }

    private enum class BaselineKind {
        Initial,
        Incremental,
    }

    companion object {
        private const val NORMALIZED_GRID_SIZE = 96
        private const val BASELINE_BLEND_ALPHA = 0.05f
        private const val INITIAL_BASELINE_ADAPT_FACTOR = 0.6f
        private const val SHOT_COMMIT_GRACE_FRAMES = 12
    }
}
