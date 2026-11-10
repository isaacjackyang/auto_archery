package com.autoarchery.app.ui.live

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.autoarchery.app.R
import com.autoarchery.app.vision.DetectedShotResult
import com.autoarchery.app.vision.ResolvedCameraProfile
import com.autoarchery.app.vision.TargetCalibrationResult
import com.autoarchery.app.vision.TargetVisionAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraPreviewSurface(
    modifier: Modifier = Modifier,
    sessionId: String?,
    targetFaceCode: String,
    knownShotCount: Int,
    isPaused: Boolean,
    zoomRatio: Float,
    analysisResetToken: Int,
    onCameraReadyChanged: (Boolean) -> Unit = {},
    onZoomRatioApplied: (Float) -> Unit = {},
    onCameraProfileResolved: (ResolvedCameraProfile) -> Unit = {},
    onCalibrationDetected: (TargetCalibrationResult) -> Unit = {},
    onShotDetected: (DetectedShotResult) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }
    }
    val analyzer = remember { TargetVisionAnalyzer() }
    val latestOnCameraReadyChanged by rememberUpdatedState(onCameraReadyChanged)
    val latestOnZoomRatioApplied by rememberUpdatedState(onZoomRatioApplied)
    val latestOnCameraProfileResolved by rememberUpdatedState(onCameraProfileResolved)
    val latestOnCalibrationDetected by rememberUpdatedState(onCalibrationDetected)
    val latestOnShotDetected by rememberUpdatedState(onShotDetected)

    SideEffect {
        analyzer.targetFaceCode = targetFaceCode
        analyzer.sessionId = sessionId
        analyzer.knownShotCount = knownShotCount
        analyzer.isPaused = isPaused
        analyzer.analysisResetToken = analysisResetToken
        analyzer.onCalibrationDetected = latestOnCalibrationDetected
        analyzer.onShotDetected = latestOnShotDetected
    }

    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) { hasPermission = context.hasCameraPermission() }
    var bindError by remember { mutableStateOf<String?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(boundCamera, zoomRatio) {
        val camera = boundCamera ?: return@LaunchedEffect
        val maxZoom = camera.cameraInfo.zoomState.value?.maxZoomRatio ?: zoomRatio
        val appliedZoomRatio = zoomRatio.coerceIn(1f, maxZoom)
        camera.cameraControl.setZoomRatio(appliedZoomRatio)
        latestOnZoomRatioApplied(appliedZoomRatio)
    }

    DisposableEffect(hasPermission, lifecycleOwner) {
        val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
        var isDisposed = false

        if (hasPermission) {
            previewView.post {
                if (isDisposed) return@post
                runCatching {
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener(
                        {
                            runCatching {
                                val cameraProvider = cameraProviderFuture.get()
                                val (cameraSelector, profile) = resolveCameraSelection(cameraProvider)
                                val preview = Preview.Builder().build().apply {
                                    surfaceProvider = previewView.surfaceProvider
                                }
                                val analysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .apply {
                                        setAnalyzer(cameraExecutor) { imageProxy ->
                                            analyzer.analyze(imageProxy)
                                        }
                                    }
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    analysis,
                                )
                                boundCamera = camera
                                bindError = null
                                latestOnCameraProfileResolved(profile)
                                latestOnCameraReadyChanged(true)
                            }.onFailure { throwable ->
                                boundCamera = null
                                bindError = throwable.message
                                latestOnCameraReadyChanged(false)
                            }
                        },
                        ContextCompat.getMainExecutor(context),
                    )
                }.onFailure { throwable ->
                    boundCamera = null
                    bindError = throwable.message
                    latestOnCameraReadyChanged(false)
                }
            }
        } else {
            boundCamera = null
            latestOnCameraReadyChanged(false)
        }
        onDispose {
            isDisposed = true
            boundCamera = null
            latestOnCameraReadyChanged(false)
            cameraExecutor.shutdown()
        }
    }

    if (hasPermission) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize(),
            )
            if (bindError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(bindError.orEmpty())
                }
            }
        }
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.camera_permission_hint))
                Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text(stringResource(R.string.grant_camera_access))
                }
                androidx.compose.material3.TextButton(onClick = {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + context.packageName)))
                }) { Text(stringResource(R.string.open_app_settings)) }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
private fun resolveCameraSelection(cameraProvider: ProcessCameraProvider): Pair<CameraSelector, ResolvedCameraProfile> {
    val backCandidates = cameraProvider.availableCameraInfos.mapNotNull { cameraInfo ->
        val camera2Info = Camera2CameraInfo.from(cameraInfo)
        val lensFacing = camera2Info.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
        if (lensFacing != CameraCharacteristics.LENS_FACING_BACK) {
            return@mapNotNull null
        }

        val cameraId = camera2Info.cameraId
        val focalLength = camera2Info
            .getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            ?.maxOrNull()
            ?: 0f
        CameraCandidate(
            cameraInfo = cameraInfo,
            cameraId = cameraId,
            maxFocalLength = focalLength,
        )
    }

    val selected = backCandidates.maxByOrNull { it.maxFocalLength }
    if (selected == null) {
        return CameraSelector.DEFAULT_BACK_CAMERA to ResolvedCameraProfile(
            cameraMode = "MAIN_ZOOM",
            cameraLensId = null,
            zoomRatio = 2f,
            zoomLabel = "Main zoom",
        )
    }

    val minFocalLength = backCandidates.minOfOrNull { it.maxFocalLength } ?: selected.maxFocalLength
    val isTelephoto = backCandidates.size > 1 && selected.maxFocalLength > minFocalLength * 1.35f
    val selector = CameraSelector.Builder()
        .addCameraFilter { cameraInfos ->
            cameraInfos.filter { info ->
                Camera2CameraInfo.from(info).cameraId == selected.cameraId
            }
        }
        .build()

    val profile = if (isTelephoto) {
        ResolvedCameraProfile(
            cameraMode = "TELEPHOTO",
            cameraLensId = selected.cameraId,
            zoomRatio = 1f,
            zoomLabel = "Telephoto",
        )
    } else {
        ResolvedCameraProfile(
            cameraMode = "MAIN_ZOOM",
            cameraLensId = selected.cameraId,
            zoomRatio = 2f,
            zoomLabel = "Main zoom x2.0",
        )
    }
    return selector to profile
}

private data class CameraCandidate(
    val cameraInfo: CameraInfo,
    val cameraId: String,
    val maxFocalLength: Float,
)

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
