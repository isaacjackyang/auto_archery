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
    val acknowledge: (Boolean) -> Unit = {},
    val sourceSessionId: String? = null,
    val isCurrent: () -> Boolean = { true },
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

    @Volatile var targetFaceCode: String = "FACE_122CM"
    private var commit: PendingShotCommit? = null
    private var lastGeometry: String? = null
    private var lastFrame: FloatArray? = null
    private var stableSceneFrames = 0
    private var lastSessionId: String? = null
    private var lastAnalysisResetToken: Int = 0
    private var lastKnownShotCount: Int = 0
    private var frameCounter: Int = 0
    private var calibration: TargetCalibrationResult? = null
    private var baselineState: BaselineState? = null
    private var pendingCandidate: StableShotCandidate? = null

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

            // A single-circle calibration cannot represent a three-spot sheet.
            if (targetFaceCode == "FACE_20CM_5_RING_TRIPLE") return
            val crop = image.cropRect
            val rotated = image.imageInfo.rotationDegrees % 180 != 0
            val orientedWidth = if (rotated) crop.height() else crop.width()
            val orientedHeight = if (rotated) crop.width() else crop.height()
            val scale = 240f / maxOf(orientedWidth, orientedHeight)
            val sourceWidth = (orientedWidth * scale).toInt().coerceAtLeast(2)
            val sourceHeight = (orientedHeight * scale).toInt().coerceAtLeast(2)
            val geometry = "$crop:${image.imageInfo.rotationDegrees}:$targetFaceCode"
            if (geometry != lastGeometry) { resetAnalysisState(); lastGeometry = geometry }
            val lumaFrame = sampleLumaFrame(image, sourceWidth, sourceHeight)

            val detectedCalibration = if (calibration == null) detectCalibration(lumaFrame, sourceWidth, sourceHeight) else null
            if (detectedCalibration != null && shouldAcceptCalibration(detectedCalibration)) {
                calibration = detectedCalibration
                resetBaselineTracking()
                lastKnownShotCount = knownShotCount
                onCalibrationDetected?.invoke(detectedCalibration)
            }

            val activeCalibration = calibration ?: return
            commit?.let { pending ->
                val accepted = pending.accepted ?: return
                commit = null
                if (!accepted) return // retain the pre-shot baseline; UI pauses on rejection
                baselineState = captureBaseline(pending.frame, pending.count + 1)
                pendingCandidate = null
            }
            if (waitForCommittedCount(baselineState?.shotCount, knownShotCount, lastKnownShotCount)) return
            if (knownShotCount != lastKnownShotCount) {
                pendingCandidate = null
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

            val previous = lastFrame
            lastFrame = normalizedFrame
            if (previous == null || !sceneIsStable(previous, normalizedFrame)) {
                stableSceneFrames = 0
                pendingCandidate = null
                return
            }
            stableSceneFrames++
            if (stableSceneFrames < 3) return

            val candidate = detectIncrementalChange(activeBaseline.normalizedFrame, normalizedFrame)
            if (candidate == null) {
                pendingCandidate = null
                // Do not learn unclassified changes into the background: that loses arrows.
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

            if (stableCandidate.stableFrames >= 3) {
                val pending = PendingShotCommit(normalizedFrame, knownShotCount)
                commit = pending
                pendingCandidate = null
                val emittedToken = analysisResetToken
                onShotDetected?.invoke(DetectedShotResult(stableCandidate.xNormalized, stableCandidate.yNormalized,
                    stableCandidate.confidence, acknowledge = { accepted -> pending.acknowledge(accepted) },
                    sourceSessionId = activeSessionId,
                    isCurrent = { sessionId == activeSessionId && analysisResetToken == emittedToken && !isPaused }))
                if (onShotDetected == null) pending.acknowledge(false)
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
        commit = null
        lastFrame = null
        stableSceneFrames = 0
        calibration = null
        baselineState = null
        pendingCandidate = null
    }

    private fun resetBaselineTracking() {
        baselineState = null
        pendingCandidate = null
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
        val buffer = plane.buffer.duplicate()
        val origin = buffer.position()
        val crop = image.cropRect
        val rotation = image.imageInfo.rotationDegrees
        return FloatArray(targetWidth * targetHeight) { index ->
            val u = (index % targetWidth + 0.5f) / targetWidth
            val v = (index / targetWidth + 0.5f) / targetHeight
            val (sx, sy) = sensorCoordinates(u, v, rotation)
            val x = (crop.left + sx * crop.width()).toInt().coerceIn(crop.left, crop.right - 1)
            val y = (crop.top + sy * crop.height()).toInt().coerceIn(crop.top, crop.bottom - 1)
            (buffer.get(origin + y * plane.rowStride + x * plane.pixelStride).toInt() and 255).toFloat()
        }
    }

    internal fun sensorCoordinates(u: Float, v: Float, rotation: Int): Pair<Float, Float> = when (rotation) {
        90 -> v to (1f - u)
        180 -> (1f - u) to (1f - v)
        270 -> (1f - v) to u
        else -> u to v
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

        val result = fitEllipse(sampleBoundaryPoints(luma, width, height, centerX, centerY)) ?: return null
        // Require several concentric transitions at the selected face's known ring spacing.
        val ringCount = com.autoarchery.app.domain.targetFaceSpec(targetFaceCode).visibleRingCount
        val radius = result.radiusXPx
        var supportedRings = 0
        for (ring in 1..ringCount) {
            var support = 0
            for (step in 0 until 36) {
                val angle = step * PI.toFloat() / 18f
                val r = radius * ring / ringCount
                val inner = bilinearSample(luma, width, height, centerX + cos(angle) * (r - 1.5f), centerY + sin(angle) * (r - 1.5f))
                val outer = bilinearSample(luma, width, height, centerX + cos(angle) * (r + 1.5f), centerY + sin(angle) * (r + 1.5f))
                if (abs(inner - outer) >= 8f) support++
            }
            if (support >= 24) supportedRings++
            if (ring == ringCount && support < 28) return null
        }
        return result.takeIf { supportedRings >= min(4, ringCount) }
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
                    if (gradient >= 12f) {
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
        if (radiusX < 24f || radiusY < 24f || maxOf(radiusX, radiusY) / min(radiusX, radiusY) > 1.12f) return null

        val normalizedErrors = rotated.map { (x, y) ->
            abs((x.pow(2) / radiusX.pow(2)) + (y.pow(2) / radiusY.pow(2)) - 1f)
        }
        val meanError = normalizedErrors.average().toFloat()
        val confidence = ((points.size / 36f) * (1f - meanError.coerceIn(0f, 1f))).coerceIn(0f, 0.99f)
        if (confidence < 0.35f) return null

        return TargetCalibrationResult(
            centerXPx = centerX,
            centerYPx = centerY,
            radiusXPx = (radiusX + radiusY) / 2f,
            radiusYPx = (radiusX + radiusY) / 2f,
            rotationDeg = 0f,
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
        val offsets = FloatArray(reference.size) { reference[it] - current[it] }.sorted()
        val offset = offsets[offsets.size / 2]
        val changed = reference.indices.count { abs(reference[it] - current[it] - offset) >= 18f }
        if (changed > reference.size * 0.12f) return null
        val darkDiff = FloatArray(reference.size)
        for (index in reference.indices) {
            darkDiff[index] = (reference[index] - current[index] - offset).coerceAtLeast(0f)
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
            if (componentPixels.size > 300 || minRadius > 1f) continue
            val xs = componentPixels.map { it % gridSize }
            val ys = componentPixels.map { it / gridSize }
            val spanX = xs.max() - xs.min()
            val spanY = ys.max() - ys.min()
            if (maxOf(spanX, spanY) < 4) continue
            // Locate the two ends along the component's principal axis, never an interior point.
            val mx = xs.average().toFloat(); val my = ys.average().toFloat()
            val xx = xs.sumOf { ((it - mx) * (it - mx)).toDouble() }
            val yy = ys.sumOf { ((it - my) * (it - my)).toDouble() }
            val xy = xs.indices.sumOf { ((xs[it] - mx) * (ys[it] - my)).toDouble() }
            val axis = 0.5f * atan2((2 * xy).toFloat(), (xx - yy).toFloat())
            val projections = componentPixels.map { ((it % gridSize) - mx) * cos(axis) + ((it / gridSize) - my) * sin(axis) }
            val first = componentPixels[projections.indices.minBy { projections[it] }]
            val last = componentPixels[projections.indices.maxBy { projections[it] }]
            fun squaredRadius(i: Int) = (i % gridSize - center).pow(2) + (i / gridSize - center).pow(2)
            if (abs(squaredRadius(first) - squaredRadius(last)) < radius * radius * 0.08f) continue
            val endpoint = if (squaredRadius(first) < squaredRadius(last)) first else last
            impactX = endpoint % gridSize; impactY = endpoint / gridSize

            val confidence = (
                0.35f +
                    (componentPixels.size / 140f) +
                    (darkest / (componentPixels.size * 70f)) +
                    radialSpan
                ).coerceIn(0f, 0.65f)

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

    internal fun sceneIsStable(previous: FloatArray, current: FloatArray): Boolean {
        val deltas = previous.indices.map { previous[it] - current[it] }.sorted()
        val offset = deltas[deltas.size / 2]
        return previous.indices.count { abs(previous[it] - current[it] - offset) > 12f } < previous.size * 0.03f
    }

    private fun bilinearSample(
        luma: FloatArray,
        width: Int,
        height: Int,
        x: Float,
        y: Float,
    ): Float {
        val boundedX = x.coerceIn(0f, (width - 1).toFloat())
        val boundedY = y.coerceIn(0f, (height - 1).toFloat())
        val x0 = boundedX.toInt().coerceIn(0, width - 1)
        val y0 = boundedY.toInt().coerceIn(0, height - 1)
        val x1 = (x0 + 1).coerceIn(0, width - 1)
        val y1 = (y0 + 1).coerceIn(0, height - 1)
        val dx = boundedX - x0
        val dy = boundedY - y0

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

    private data class BaselineState(val shotCount: Int, val normalizedFrame: FloatArray)

    companion object {
        private const val NORMALIZED_GRID_SIZE = 96
    }
}
