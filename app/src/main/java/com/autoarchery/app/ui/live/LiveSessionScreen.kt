package com.autoarchery.app.ui.live

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoarchery.app.R
import com.autoarchery.app.domain.TargetScoringBandId
import com.autoarchery.app.domain.normalizationHalfExtentMm
import com.autoarchery.app.domain.targetFaceSpec
import com.autoarchery.app.domain.targetSpotCenters
import com.autoarchery.app.domain.targetScoringBands
import com.autoarchery.app.domain.visibleSpotRadiusMm
import com.autoarchery.app.domain.targetFaceSpecs as supportedTargetFaceSpecs
import com.autoarchery.app.ui.model.CalibrationState
import com.autoarchery.app.ui.model.LiveSessionUiState
import com.autoarchery.app.ui.model.ReviewFilter
import com.autoarchery.app.ui.model.SessionSetupUi
import com.autoarchery.app.ui.model.ShotAdjustmentUi
import com.autoarchery.app.ui.model.ShotMarkerUi
import com.autoarchery.app.ui.model.ShotRowUi
import java.util.Locale

@Composable
fun LiveSessionRoute(
    sessionId: String,
    onOpenMetrics: (String) -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: LiveSessionViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val localeTag = androidx.compose.ui.platform.LocalConfiguration.current.locales.toLanguageTags()
    androidx.compose.runtime.LaunchedEffect(localeTag) { viewModel.refreshLanguage() }


    LaunchedEffect(sessionId) {
        viewModel.bindSession(sessionId)
    }

    val deletedShot by viewModel.deletedShot.collectAsStateWithLifecycle()
    val snackbar = androidx.compose.runtime.remember { androidx.compose.material3.SnackbarHostState() }
    val deletedMessage = stringResource(R.string.arrow_deleted)
    val undoLabel = stringResource(R.string.undo_action)
    LaunchedEffect(deletedShot) {
        val id = deletedShot ?: return@LaunchedEffect
        if (snackbar.showSnackbar(deletedMessage, undoLabel, withDismissAction = true,
                duration = androidx.compose.material3.SnackbarDuration.Long) == androidx.compose.material3.SnackbarResult.ActionPerformed) {
            viewModel.undoDelete(id)
        }
        if (viewModel.deletedShot.value == id) viewModel.deletedShot.value = null
    }
    LiveSessionScreen(
        snackbar = snackbar,
        uiState = uiState,
        onOpenMetrics = onOpenMetrics,
        onOpenSettings = onOpenSettings,
        onSelectShot = viewModel::selectShot,
        onAdjustSelected = viewModel::openAdjustSheet,
        onDismissAdjustSheet = viewModel::closeAdjustSheet,
        onSaveAdjustments = viewModel::saveAdjustments,
        onUpdateAdjustmentPosition = viewModel::updateAdjustmentPosition,
        onConfirmSelected = viewModel::confirmSelectedShot,
        onReplaySelected = viewModel::replaySelectedShot,
        onTogglePaused = viewModel::togglePaused,
        onApplyFilter = viewModel::setReviewFilter,
        onManualAdd = viewModel::manualAddShot,
        onDeleteLast = viewModel::deleteLastShot,
        onReanalyze = viewModel::reanalyzeSelectedShot,
        onConfirmHighConfidence = viewModel::confirmHighConfidenceShots,
        onConfirmEnd = viewModel::confirmCurrentEnd,
        onArmRecognition = viewModel::armRecognition,
        onResetCalibration = viewModel::resetCalibration,
        onIncreaseDigitalZoom = viewModel::increaseDigitalZoom,
        onDecreaseDigitalZoom = viewModel::decreaseDigitalZoom,
        onClearCurrentEnd = viewModel::clearCurrentEndShots,
        onZoomRatioApplied = viewModel::onZoomRatioApplied,
        onCameraReadyChanged = viewModel::onCameraReadyChanged,
        onCameraProfileResolved = viewModel::onCameraProfileResolved,
        onCalibrationDetected = viewModel::onCalibrationDetected,
        onShotDetected = viewModel::onShotDetected,
        onOpenSessionSetup = viewModel::openSessionSetup,
        onDismissSessionSetup = viewModel::closeSessionSetup,
        onSetupAthleteChanged = viewModel::updateSetupAthlete,
        onSetupDistanceChanged = viewModel::updateSetupDistance,
        onSetupArrowsPerEndChanged = viewModel::updateSetupArrowsPerEnd,
        onSetupTargetFaceChanged = viewModel::updateSetupTargetFace,
        onTrainingGoalChanged = viewModel::updateTrainingGoal,
        onCreateSession = viewModel::createSession,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LiveSessionScreen(
    snackbar: androidx.compose.material3.SnackbarHostState,
    uiState: LiveSessionUiState,
    onOpenMetrics: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onSelectShot: (String) -> Unit,
    onAdjustSelected: () -> Unit,
    onDismissAdjustSheet: () -> Unit,
    onSaveAdjustments: () -> Unit,
    onUpdateAdjustmentPosition: (Float, Float) -> Unit,
    onConfirmSelected: () -> Unit,
    onReplaySelected: () -> Unit,
    onTogglePaused: () -> Unit,
    onApplyFilter: (ReviewFilter) -> Unit,
    onManualAdd: () -> Unit,
    onDeleteLast: () -> Unit,
    onReanalyze: () -> Unit,
    onConfirmHighConfidence: () -> Unit,
    onConfirmEnd: () -> Unit,
    onArmRecognition: () -> Unit,
    onResetCalibration: () -> Unit,
    onIncreaseDigitalZoom: () -> Unit,
    onDecreaseDigitalZoom: () -> Unit,
    onClearCurrentEnd: () -> Unit,
    onZoomRatioApplied: (Float) -> Unit,
    onCameraReadyChanged: (Boolean) -> Unit,
    onCameraProfileResolved: (com.autoarchery.app.vision.ResolvedCameraProfile) -> Unit,
    onCalibrationDetected: (com.autoarchery.app.vision.TargetCalibrationResult) -> Unit,
    onShotDetected: (com.autoarchery.app.vision.DetectedShotResult) -> Unit,
    onOpenSessionSetup: () -> Unit,
    onDismissSessionSetup: () -> Unit,
    onSetupAthleteChanged: (String) -> Unit,
    onSetupDistanceChanged: (Int) -> Unit,
    onSetupArrowsPerEndChanged: (Int) -> Unit,
    onSetupTargetFaceChanged: (String) -> Unit,
    onTrainingGoalChanged: (String) -> Unit,
    onCreateSession: () -> Unit,
) {
    var discardAdjustment by rememberSaveable { mutableStateOf(false) }
    val dismissAdjustment: () -> Unit = {
        if (uiState.adjustmentDraft?.isDirty == true) discardAdjustment = true else onDismissAdjustSheet()
    }
    if (discardAdjustment) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { discardAdjustment = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_message)) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { discardAdjustment = false; onDismissAdjustSheet() }) { Text(stringResource(R.string.discard_action)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { discardAdjustment = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    val configuration = LocalConfiguration.current
    val useDialogForSessionSetup = configuration.screenWidthDp > configuration.screenHeightDp

    var destructiveAction by rememberSaveable(uiState.sessionId) { mutableStateOf<String?>(null) }
    if (destructiveAction != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { destructiveAction = null },
            title = { Text(stringResource(if (destructiveAction == "clear") R.string.confirm_clear_title else R.string.confirm_delete_title)) },
            text = { Text(stringResource(R.string.confirm_delete_message)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val action = destructiveAction
                    destructiveAction = null
                    if (action == "clear") onClearCurrentEnd() else onDeleteLast()
                }) { Text(stringResource(R.string.confirm_remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { destructiveAction = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        topBar = {
            Column {
            CompactSessionHeader(
                title = if (uiState.sessionId == null) {
                    stringResource(R.string.new_session)
                } else {
                    "${uiState.athleteName} • ${uiState.sessionTitle}"
                },
                hasSession = uiState.sessionId != null,
                onOpenSessionSetup = onOpenSessionSetup,
                onOpenMetrics = { uiState.sessionId?.let(onOpenMetrics) },
                onOpenSettings = onOpenSettings,
            )
            val hint = when {
                uiState.sessionId == null -> R.string.step_create
                uiState.calibrationState == CalibrationState.NotReady -> R.string.step_camera
                uiState.calibrationState == CalibrationState.NeedsCalibration -> R.string.step_calibrate
                uiState.isPaused -> R.string.step_paused
                uiState.pendingCount > 0 || uiState.lowConfidenceCount > 0 -> R.string.step_review
                !uiState.isRecognitionArmed -> R.string.step_arm
                else -> R.string.step_wait
            }
            Text(stringResource(hint), modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.bodyMedium)
            }

        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            val wideLayout = maxWidth >= 840.dp && configuration.fontScale <= 1.3f

            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CameraPanel(
                        uiState = uiState,
                        onTogglePaused = onTogglePaused,
                        onArmRecognition = onArmRecognition,
                        onResetCalibration = onResetCalibration,
                        onIncreaseDigitalZoom = onIncreaseDigitalZoom,
                        onDecreaseDigitalZoom = onDecreaseDigitalZoom,
                        onClearCurrentEnd = { destructiveAction = "clear" },
                        onNextRound = onConfirmEnd,
                        onZoomRatioApplied = onZoomRatioApplied,
                        onCameraReadyChanged = onCameraReadyChanged,
                        onCameraProfileResolved = onCameraProfileResolved,
                        onCalibrationDetected = onCalibrationDetected,
                        onShotDetected = onShotDetected,
                        isWideLayout = true,
                        modifier = Modifier.weight(2f).fillMaxHeight(),
                    )
                    TargetReviewPanel(
                        uiState = uiState,
                        onSelectShot = onSelectShot,
                        onAdjustSelected = onAdjustSelected,
                        onConfirmSelected = onConfirmSelected,
                        onReplaySelected = onReplaySelected,
                        onApplyFilter = onApplyFilter,
                        onManualAdd = onManualAdd,
                        onDeleteLast = onDeleteLast,
                        onReanalyze = onReanalyze,
                        onConfirmHighConfidence = onConfirmHighConfidence,
                        onConfirmEnd = onConfirmEnd,
                        isWideLayout = true,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CameraPanel(
                        uiState = uiState,
                        onTogglePaused = onTogglePaused,
                        onArmRecognition = onArmRecognition,
                        onResetCalibration = onResetCalibration,
                        onIncreaseDigitalZoom = onIncreaseDigitalZoom,
                        onDecreaseDigitalZoom = onDecreaseDigitalZoom,
                        onClearCurrentEnd = { destructiveAction = "clear" },
                        onNextRound = onConfirmEnd,
                        onZoomRatioApplied = onZoomRatioApplied,
                        onCameraReadyChanged = onCameraReadyChanged,
                        onCameraProfileResolved = onCameraProfileResolved,
                        onCalibrationDetected = onCalibrationDetected,
                        onShotDetected = onShotDetected,
                        isWideLayout = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TargetReviewPanel(
                        uiState = uiState,
                        onSelectShot = onSelectShot,
                        onAdjustSelected = onAdjustSelected,
                        onConfirmSelected = onConfirmSelected,
                        onReplaySelected = onReplaySelected,
                        onApplyFilter = onApplyFilter,
                        onManualAdd = onManualAdd,
                        onDeleteLast = onDeleteLast,
                        onReanalyze = onReanalyze,
                        onConfirmHighConfidence = onConfirmHighConfidence,
                        onConfirmEnd = onConfirmEnd,
                        isWideLayout = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    if (uiState.isAdjustSheetVisible && uiState.selectedShot != null && uiState.adjustmentDraft != null) {
        ModalBottomSheet(onDismissRequest = dismissAdjustment) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                val useSplitLayout = maxWidth >= 560.dp

                if (useSplitLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(stringResource(R.string.adjust), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("${uiState.selectedShot.label}: ${uiState.selectedShot.scoreLabel}")
                            Text(uiState.selectedShot.detailLabel)
                            Text(stringResource(R.string.adjust_drag_hint))
                            Text(
                                stringResource(
                                    R.string.adjust_coordinates,
                                    formatMm(uiState.adjustmentDraft.xMm),
                                    formatMm(uiState.adjustmentDraft.yMm),
                                ),
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = dismissAdjustment) { Text(stringResource(R.string.close)) }
                                Button(
                                    onClick = onSaveAdjustments,
                                    enabled = uiState.adjustmentDraft.isDirty,
                                ) {
                                    Text(stringResource(R.string.save))
                                }
                            }
                        }
                        DigitalTargetCard(
                            markers = uiState.markers,
                            selectedShotId = uiState.selectedShot.id,
                            targetFaceCode = uiState.targetFaceCode,
                            modifier = Modifier
                                .weight(1f)
                                .height(320.dp),
                            adjustmentDraft = uiState.adjustmentDraft,
                            onUpdateAdjustmentPosition = onUpdateAdjustmentPosition,
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(stringResource(R.string.adjust), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${uiState.selectedShot.label}: ${uiState.selectedShot.scoreLabel}")
                        Text(uiState.selectedShot.detailLabel)
                        Text(stringResource(R.string.adjust_drag_hint))
                        Text(
                            stringResource(
                                R.string.adjust_coordinates,
                                formatMm(uiState.adjustmentDraft.xMm),
                                formatMm(uiState.adjustmentDraft.yMm),
                            ),
                        )
                        DigitalTargetCard(
                            markers = uiState.markers,
                            selectedShotId = uiState.selectedShot.id,
                            targetFaceCode = uiState.targetFaceCode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            adjustmentDraft = uiState.adjustmentDraft,
                            onUpdateAdjustmentPosition = onUpdateAdjustmentPosition,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = dismissAdjustment) { Text(stringResource(R.string.close)) }
                            Button(
                                onClick = onSaveAdjustments,
                                enabled = uiState.adjustmentDraft.isDirty,
                            ) {
                                Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (uiState.isSessionSetupVisible) {
        if (useDialogForSessionSetup) {
            Dialog(onDismissRequest = onDismissSessionSetup) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.92f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    SessionSetupSheet(
                        setup = uiState.sessionSetup,
                        onAthleteSelected = onSetupAthleteChanged,
                        onDistanceSelected = onSetupDistanceChanged,
                        onArrowsPerEndSelected = onSetupArrowsPerEndChanged,
                        onTargetFaceSelected = onSetupTargetFaceChanged,
                        onTrainingGoalChanged = onTrainingGoalChanged,
                        onCreateSession = onCreateSession,
                    )
                }
            }
        } else {
            ModalBottomSheet(
                onDismissRequest = onDismissSessionSetup,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                SessionSetupSheet(
                    setup = uiState.sessionSetup,
                    onAthleteSelected = onSetupAthleteChanged,
                    onDistanceSelected = onSetupDistanceChanged,
                    onArrowsPerEndSelected = onSetupArrowsPerEndChanged,
                    onTargetFaceSelected = onSetupTargetFaceChanged,
                    onTrainingGoalChanged = onTrainingGoalChanged,
                    onCreateSession = onCreateSession,
                )
            }
        }
    }
}

@Composable
private fun CompactSessionHeader(
    title: String,
    hasSession: Boolean,
    onOpenSessionSetup: () -> Unit,
    onOpenMetrics: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompactHeaderAction(
                    label = stringResource(R.string.new_session),
                    onClick = onOpenSessionSetup,
                )
                if (hasSession) {
                    CompactHeaderAction(
                        label = stringResource(R.string.open_metrics),
                        onClick = onOpenMetrics,
                    )
                }
                CompactHeaderAction(
                    label = stringResource(R.string.open_settings),
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

@Composable
private fun CompactHeaderAction(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CameraPanel(
    uiState: LiveSessionUiState,
    onTogglePaused: () -> Unit,
    onArmRecognition: () -> Unit,
    onResetCalibration: () -> Unit,
    onIncreaseDigitalZoom: () -> Unit,
    onDecreaseDigitalZoom: () -> Unit,
    onClearCurrentEnd: () -> Unit,
    onNextRound: () -> Unit,
    onZoomRatioApplied: (Float) -> Unit,
    onCameraReadyChanged: (Boolean) -> Unit,
    onCameraProfileResolved: (com.autoarchery.app.vision.ResolvedCameraProfile) -> Unit,
    onCalibrationDetected: (com.autoarchery.app.vision.TargetCalibrationResult) -> Unit,
    onShotDetected: (com.autoarchery.app.vision.DetectedShotResult) -> Unit,
    isWideLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = (if (isWideLayout) Modifier.fillMaxSize() else Modifier.fillMaxWidth()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = if (uiState.athleteName.isBlank()) stringResource(R.string.loading) else stringResource(R.string.current_athlete_label, uiState.athleteName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxHeight(),
                    ) {
                        CameraPreviewSurface(
                            modifier = Modifier.fillMaxSize(),
                            sessionId = uiState.sessionId,
                            targetFaceCode = uiState.targetFaceCode,
                            knownShotCount = uiState.shots.size,
                            isPaused = uiState.isPaused || uiState.sessionId == null || !uiState.isRecognitionArmed,
                            zoomRatio = uiState.zoomRatio,
                            analysisResetToken = uiState.analysisResetToken,
                            onCameraReadyChanged = onCameraReadyChanged,
                            onZoomRatioApplied = onZoomRatioApplied,
                            onCameraProfileResolved = onCameraProfileResolved,
                            onCalibrationDetected = onCalibrationDetected,
                            onShotDetected = onShotDetected,
                        )
                        TargetFrameOverlay()
                    }
                    CameraControlColumn(
                        uiState = uiState,
                        onTogglePaused = onTogglePaused,
                        onArmRecognition = onArmRecognition,
                        onResetCalibration = onResetCalibration,
                        onIncreaseDigitalZoom = onIncreaseDigitalZoom,
                        onDecreaseDigitalZoom = onDecreaseDigitalZoom,
                        onClearCurrentEnd = onClearCurrentEnd,
                        onNextRound = onNextRound,
                        isWideLayout = true,
                        modifier = Modifier
                            .width(208.dp)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(304.dp),
                ) {
                    CameraPreviewSurface(
                        modifier = Modifier.fillMaxSize(),
                        sessionId = uiState.sessionId,
                        targetFaceCode = uiState.targetFaceCode,
                        knownShotCount = uiState.shots.size,
                        isPaused = uiState.isPaused || uiState.sessionId == null || !uiState.isRecognitionArmed,
                        zoomRatio = uiState.zoomRatio,
                        analysisResetToken = uiState.analysisResetToken,
                        onCameraReadyChanged = onCameraReadyChanged,
                        onZoomRatioApplied = onZoomRatioApplied,
                        onCameraProfileResolved = onCameraProfileResolved,
                        onCalibrationDetected = onCalibrationDetected,
                        onShotDetected = onShotDetected,
                    )
                    TargetFrameOverlay()
                }
                CameraControlColumn(
                    uiState = uiState,
                    onTogglePaused = onTogglePaused,
                    onArmRecognition = onArmRecognition,
                    onResetCalibration = onResetCalibration,
                    onIncreaseDigitalZoom = onIncreaseDigitalZoom,
                    onDecreaseDigitalZoom = onDecreaseDigitalZoom,
                    onClearCurrentEnd = onClearCurrentEnd,
                    onNextRound = onNextRound,
                    isWideLayout = false,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CameraControlColumn(
    uiState: LiveSessionUiState,
    onTogglePaused: () -> Unit,
    onArmRecognition: () -> Unit,
    onResetCalibration: () -> Unit,
    onIncreaseDigitalZoom: () -> Unit,
    onDecreaseDigitalZoom: () -> Unit,
    onClearCurrentEnd: () -> Unit,
    onNextRound: () -> Unit,
    isWideLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    val canSetRecognition = uiState.sessionId != null &&
        uiState.calibrationState == CalibrationState.Calibrated &&
        !uiState.isRecognitionArmed
    val canAdjustZoom = uiState.sessionId != null && uiState.calibrationState != CalibrationState.NotReady
    val canClearCurrentEnd = uiState.sessionId != null && uiState.shots.isNotEmpty()

    if (isWideLayout) {
        Column(
            modifier = modifier
                .padding(start = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CameraStatusStrip(
                    uiState = uiState,
                    modifier = Modifier.fillMaxWidth(),
                )
                CameraActionStrip(
                    canAdjustZoom = canAdjustZoom,
                    canClearCurrentEnd = canClearCurrentEnd,
                    canSetRecognition = canSetRecognition,
                    hasSession = uiState.sessionId != null,
                    onIncreaseDigitalZoom = onIncreaseDigitalZoom,
                    onDecreaseDigitalZoom = onDecreaseDigitalZoom,
                    onClearCurrentEnd = onClearCurrentEnd,
                    onNextRound = onNextRound,
                    onArmRecognition = onArmRecognition,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DebugControlSection(
                uiState = uiState,
                onTogglePaused = onTogglePaused,
                onResetCalibration = onResetCalibration,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CameraStatusStrip(
                uiState = uiState,
                modifier = Modifier.fillMaxWidth(),
            )
            CameraActionStrip(
                canAdjustZoom = canAdjustZoom,
                canClearCurrentEnd = canClearCurrentEnd,
                canSetRecognition = canSetRecognition,
                hasSession = uiState.sessionId != null,
                onIncreaseDigitalZoom = onIncreaseDigitalZoom,
                onDecreaseDigitalZoom = onDecreaseDigitalZoom,
                onClearCurrentEnd = onClearCurrentEnd,
                onNextRound = onNextRound,
                onArmRecognition = onArmRecognition,
                modifier = Modifier.fillMaxWidth(),
            )
            DebugControlSection(
                uiState = uiState,
                onTogglePaused = onTogglePaused,
                onResetCalibration = onResetCalibration,
            )
        }
    }
}

@Composable
private fun CameraActionStrip(
    canAdjustZoom: Boolean,
    canClearCurrentEnd: Boolean,
    canSetRecognition: Boolean,
    hasSession: Boolean,
    onIncreaseDigitalZoom: () -> Unit,
    onDecreaseDigitalZoom: () -> Unit,
    onClearCurrentEnd: () -> Unit,
    onNextRound: () -> Unit,
    onArmRecognition: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactFilledActionButton(
            label = stringResource(R.string.zoom_in_quarter),
            enabled = canAdjustZoom,
            onClick = onIncreaseDigitalZoom,
        )
        CompactOutlinedActionButton(
            label = stringResource(R.string.zoom_out_quarter),
            enabled = canAdjustZoom,
            onClick = onDecreaseDigitalZoom,
        )
        CompactOutlinedActionButton(
            label = stringResource(R.string.clear_current_end),
            enabled = canClearCurrentEnd,
            onClick = onClearCurrentEnd,
        )
        CompactOutlinedActionButton(
            label = stringResource(R.string.next_round),
            enabled = hasSession,
            onClick = onNextRound,
        )
        CompactFilledActionButton(
            label = stringResource(R.string.set_detection),
            enabled = canSetRecognition,
            onClick = onArmRecognition,
        )
    }
}

@Composable
private fun CameraStatusStrip(
    uiState: LiveSessionUiState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactPillStat(uiState.zoomLabel)
        CompactPillStat(stringResource(R.string.end_counter, uiState.currentEndIndex, uiState.totalEnds))
        CompactPillStat(stringResource(R.string.arrow_counter, uiState.currentArrowIndex.coerceAtMost(uiState.arrowsPerEnd), uiState.arrowsPerEnd))
        CompactPillStat("${stringResource(R.string.recognition_status)}: ${uiState.recognitionLabel}")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DebugControlSection(
    uiState: LiveSessionUiState,
    onTogglePaused: () -> Unit,
    onResetCalibration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.debug_tools),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            CompactPillStat("${stringResource(R.string.calibration_status)}: ${uiState.calibrationLabel}")
            CompactFilledActionButton(
                label = if (uiState.isPaused) stringResource(R.string.resume_auto) else stringResource(R.string.pause_auto),
                enabled = true,
                onClick = onTogglePaused,
            )
            CompactOutlinedActionButton(
                label = stringResource(R.string.recalibrate),
                enabled = true,
                onClick = onResetCalibration,
            )
        }
    }
}

@Composable
private fun TargetFrameOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val side = minOf(size.width, size.height) * 0.8f
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        drawRect(
            color = Color.White.copy(alpha = 0.75f),
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(side, side),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f),
        )
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(size.width / 2f, top),
            end = Offset(size.width / 2f, top + side),
            strokeWidth = 2f,
        )
        drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(left, size.height / 2f),
            end = Offset(left + side, size.height / 2f),
            strokeWidth = 2f,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TargetReviewPanel(
    uiState: LiveSessionUiState,
    onSelectShot: (String) -> Unit,
    onAdjustSelected: () -> Unit,
    onConfirmSelected: () -> Unit,
    onReplaySelected: () -> Unit,
    onApplyFilter: (ReviewFilter) -> Unit,
    onManualAdd: () -> Unit,
    onDeleteLast: () -> Unit,
    onReanalyze: () -> Unit,
    onConfirmHighConfidence: () -> Unit,
    onConfirmEnd: () -> Unit,
    isWideLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = if (isWideLayout) modifier.fillMaxHeight().verticalScroll(rememberScrollState()) else modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                modifier = Modifier.padding(if (isWideLayout) 10.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DigitalTargetCard(
                    markers = uiState.filteredMarkers,
                    selectedShotId = uiState.selectedShot?.id,
                    targetFaceCode = uiState.targetFaceCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isWideLayout) 192.dp else 224.dp),
                    scoreLine = currentEndScoreLine(
                        shots = uiState.shots,
                        arrowsPerEnd = uiState.arrowsPerEnd,
                    ),
                    scoreValues = currentEndScoreValues(
                        shots = uiState.shots,
                        arrowsPerEnd = uiState.arrowsPerEnd,
                    ),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    CompactPillButton(
                        label = stringResource(R.string.filter_all),
                        selected = uiState.reviewFilter == ReviewFilter.All,
                        onClick = { onApplyFilter(ReviewFilter.All) },
                    )
                    CompactPillButton(
                        label = stringResource(R.string.filter_pending),
                        selected = uiState.reviewFilter == ReviewFilter.PendingOnly,
                        onClick = { onApplyFilter(ReviewFilter.PendingOnly) },
                    )
                    CompactPillButton(
                        label = stringResource(R.string.filter_low_conf),
                        selected = uiState.reviewFilter == ReviewFilter.LowConfidenceOnly,
                        onClick = { onApplyFilter(ReviewFilter.LowConfidenceOnly) },
                    )
                    CompactPillStat(label = stringResource(R.string.pending_label, uiState.pendingCount))
                    CompactPillStat(label = stringResource(R.string.low_confidence_label, uiState.lowConfidenceCount))
                }
                if (uiState.pendingCount > 0) {
                    Text(
                        text = stringResource(R.string.pending_help_text),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.shot_list), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (uiState.filteredShots.isEmpty()) {
                    Text(stringResource(R.string.loading))
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        uiState.filteredShots.forEach { shot ->
                            ShotRow(
                                shot = shot,
                                isSelected = uiState.selectedShot?.id == shot.id,
                                onClick = { onSelectShot(shot.id) },
                            )
                        }
                    }
                }
            }
        }

        uiState.selectedShot?.let { selectedShot ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(selectedShot.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(selectedShot.scoreLabel)
                    Text(selectedShot.detailLabel)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onReplaySelected) { Text(stringResource(R.string.replay_tts)) }
                        OutlinedButton(onClick = onAdjustSelected) { Text(stringResource(R.string.adjust)) }
                        Button(onClick = onConfirmSelected) { Text(stringResource(R.string.confirm_selected_shot)) }
                    }
                }
            }
        }

        var showTools by rememberSaveable { mutableStateOf(false) }
        androidx.compose.material3.TextButton(onClick = { showTools = !showTools }) {
            Text(stringResource(if (showTools) R.string.hide_tools else R.string.show_tools))
        }
        if (showTools) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CompactOutlinedActionButton(
                    label = stringResource(R.string.manual_add),
                    enabled = uiState.sessionId != null,
                    onClick = onManualAdd,
                )
                CompactOutlinedActionButton(
                    label = stringResource(R.string.delete_last),
                    enabled = uiState.sessionId != null,
                    onClick = onDeleteLast,
                )
                CompactOutlinedActionButton(
                    label = stringResource(R.string.reanalyze_selected),
                    enabled = uiState.selectedShot != null,
                    onClick = onReanalyze,
                )
                CompactOutlinedActionButton(
                    label = stringResource(R.string.confirm_high_confidence),
                    enabled = uiState.sessionId != null,
                    onClick = onConfirmHighConfidence,
                )
            }
        }

        }
        uiState.endSummary?.let { endSummary ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.end_summary), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(endSummary.summaryLine)
                    Text(endSummary.detailLine)
                    Button(onClick = onConfirmEnd) { Text(stringResource(R.string.confirm_end)) }
                }
            }
        }
    }
}

@Composable
private fun DigitalTargetCard(
    markers: List<ShotMarkerUi>,
    selectedShotId: String?,
    targetFaceCode: String,
    modifier: Modifier = Modifier,
    scoreLine: String? = null,
    scoreValues: List<String> = emptyList(),
    adjustmentDraft: ShotAdjustmentUi? = null,
    onUpdateAdjustmentPosition: ((Float, Float) -> Unit)? = null,
) {
    var isScoreExpanded by rememberSaveable(scoreLine) { mutableStateOf(true) }
    val canvasModifier = if (adjustmentDraft != null && onUpdateAdjustmentPosition != null) {
        Modifier
            .fillMaxSize()
            .pointerInput(adjustmentDraft.id) {
                var currentX = adjustmentDraft.xNormalized
                var currentY = adjustmentDraft.yNormalized
                detectDragGestures(
                    onDragStart = {
                        currentX = adjustmentDraft.xNormalized
                        currentY = adjustmentDraft.yNormalized
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val normalizedDelta = dragAmount.toNormalizedDelta(
                            scoringRadiusPx = targetLayout(size.width.toFloat(), size.height.toFloat()).scoringRadiusPx,
                        )
                        currentX = (currentX + normalizedDelta.x).coerceIn(-1f, 1f)
                        currentY = (currentY + normalizedDelta.y).coerceIn(-1f, 1f)
                        onUpdateAdjustmentPosition(currentX, currentY)
                    },
                )
            }
    } else {
        Modifier.fillMaxSize()
    }

    Surface(modifier = modifier, tonalElevation = 2.dp) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTargetFace(
                    targetFaceCode = targetFaceCode,
                    layout = targetLayout(size.width, size.height),
                )
            }
            Canvas(modifier = canvasModifier) {
                val targetLayout = targetLayout(size.width, size.height)
                val displayedMarkers = markers.map { marker ->
                    if (adjustmentDraft != null && marker.id == adjustmentDraft.id) {
                        marker.copy(
                            xNormalized = adjustmentDraft.xNormalized,
                            yNormalized = adjustmentDraft.yNormalized,
                        )
                    } else {
                        marker
                    }
                }

                displayedMarkers.forEach { marker ->
                    val point = Offset(
                        x = targetLayout.center.x + (marker.xNormalized * targetLayout.scoringRadiusPx),
                        y = targetLayout.center.y + (marker.yNormalized * targetLayout.scoringRadiusPx),
                    )
                    val isSelected = marker.id == selectedShotId
                    val backgroundColor = targetFaceBackgroundColor(
                        targetFaceCode = targetFaceCode,
                        xNormalized = marker.xNormalized,
                        yNormalized = marker.yNormalized,
                    )
                    val contrastColor = contrastColorFor(backgroundColor)
                    drawCircle(
                        color = contrastColor,
                        radius = if (isSelected) 16f else 12f,
                        center = point,
                    )
                    drawCircle(
                        color = if (isSelected) Color(0xFF0D47A1) else marker.status.color,
                        radius = if (isSelected) 11f else 8f,
                        center = point,
                    )
                    if (isSelected) {
                        drawCircle(
                            color = if (Color(0xFF0D47A1).luminance() < 0.45f) Color.White else Color.Black,
                            radius = 4f,
                            center = point,
                        )
                    }
                }
            }
            if (!scoreLine.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { isScoreExpanded = !isScoreExpanded },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(
                                horizontal = if (isScoreExpanded) 10.dp else 12.dp,
                                vertical = if (isScoreExpanded) 6.dp else 4.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        if (isScoreExpanded) {
                            Text(
                                text = stringResource(R.string.current_end_scores_title),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                scoreValues.forEach { value ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        shape = MaterialTheme.shapes.small,
                                    ) {
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(R.string.current_end_scores_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TargetLayout(
    val center: Offset,
    val scoringRadiusPx: Float,
)

private fun targetLayout(width: Float, height: Float): TargetLayout {
    val squareSize = minOf(width, height)
    return TargetLayout(
        center = Offset(width / 2f, height / 2f),
        scoringRadiusPx = squareSize * TARGET_OUTER_RADIUS_FRACTION,
    )
}

private const val TARGET_VIEWBOX_SIZE = 612f
private const val TARGET_OUTER_RADIUS = 280f
private const val TARGET_OUTER_RADIUS_FRACTION = TARGET_OUTER_RADIUS / TARGET_VIEWBOX_SIZE

private fun Offset.toNormalizedDelta(scoringRadiusPx: Float): Offset =
    Offset(
        x = x / scoringRadiusPx,
        y = y / scoringRadiusPx,
    )

private fun targetFaceBackgroundColor(
    targetFaceCode: String,
    xNormalized: Float,
    yNormalized: Float,
): Color {
    val halfExtentMm = normalizationHalfExtentMm(targetFaceCode)
    val xMm = xNormalized * halfExtentMm
    val yMm = yNormalized * halfExtentMm
    val nearestSpot = targetSpotCenters(targetFaceCode).minByOrNull { spot ->
        kotlin.math.sqrt(((xMm - spot.xMm) * (xMm - spot.xMm)) + ((yMm - spot.yMm) * (yMm - spot.yMm)))
    } ?: return TARGET_PAPER_COLOR
    val relativeX = xMm - nearestSpot.xMm
    val relativeY = yMm - nearestSpot.yMm
    val radius = kotlin.math.sqrt((relativeX * relativeX) + (relativeY * relativeY))
    val radiusInRings = radius / targetFaceSpec(targetFaceCode).ringWidthMm
    val band = targetScoringBands(targetFaceCode)
        .firstOrNull { scoringBand -> radiusInRings <= scoringBand.outerRadiusRings }
    return band?.let { targetBandColor(it.id) } ?: TARGET_PAPER_COLOR
}

private fun contrastColorFor(backgroundColor: Color): Color =
    if (backgroundColor.luminance() < 0.45f) Color.White else Color(0xFF101010)

private fun currentEndScoreLine(
    shots: List<ShotRowUi>,
    arrowsPerEnd: Int,
): String {
    return currentEndScoreValues(shots, arrowsPerEnd).joinToString(separator = "·")
}

private fun currentEndScoreValues(
    shots: List<ShotRowUi>,
    arrowsPerEnd: Int,
): List<String> {
    val values = shots.map(::compactScoreLabel).toMutableList()
    repeat((arrowsPerEnd - values.size).coerceAtLeast(0)) {
        values += "—"
    }
    return values
}

private fun compactScoreLabel(shot: ShotRowUi): String =
    when {
        shot.isX -> "X"
        shot.scoreValue <= 0 -> "M"
        else -> shot.scoreValue.toString()
    }

@Composable
private fun CompactPillButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun CompactPillStat(
    label: String,
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun CompactOutlinedActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.55f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 0.85f else 0.35f),
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CompactFilledActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun targetBandColor(id: TargetScoringBandId): Color =
    when (id) {
        TargetScoringBandId.X,
        TargetScoringBandId.Score10,
        TargetScoringBandId.Score9,
        -> Color(0xFFF8CC3B)
        TargetScoringBandId.Score8,
        TargetScoringBandId.Score7,
        -> Color(0xFFF53A18)
        TargetScoringBandId.Score6,
        TargetScoringBandId.Score5,
        -> Color(0xFF44B6F3)
        TargetScoringBandId.Score4,
        TargetScoringBandId.Score3,
        -> Color(0xFF1F1F1F)
        TargetScoringBandId.Score2,
        TargetScoringBandId.Score1,
        -> Color(0xFFF4F4F1)
        TargetScoringBandId.Miss -> Color(0xFF9AA0A6)
    }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTargetFace(
    targetFaceCode: String,
    layout: TargetLayout,
) {
    val spec = targetFaceSpec(targetFaceCode)
    val halfExtentMm = normalizationHalfExtentMm(targetFaceCode)
    val visibleSpotRadiusMm = visibleSpotRadiusMm(targetFaceCode)
    drawRect(color = TARGET_PAPER_COLOR)
    targetSpotCenters(targetFaceCode).forEach { spotCenter ->
        val center = Offset(
            x = layout.center.x + ((spotCenter.xMm / halfExtentMm) * layout.scoringRadiusPx),
            y = layout.center.y + ((spotCenter.yMm / halfExtentMm) * layout.scoringRadiusPx),
        )
        drawCircle(
            color = TARGET_PAPER_COLOR,
            radius = layout.scoringRadiusPx * (visibleSpotRadiusMm / halfExtentMm),
            center = center,
        )
        targetScoringBands(targetFaceCode)
            .sortedByDescending { it.outerRadiusRings }
            .forEach { band ->
                val radiusPx = layout.scoringRadiusPx * ((band.outerRadiusRings * spec.ringWidthMm) / halfExtentMm)
                drawCircle(
                    color = targetBandColor(band.id),
                    radius = radiusPx,
                    center = center,
                )
            }
        for (ringIndex in spec.visibleRingCount downTo 1) {
            val radiusPx = layout.scoringRadiusPx * (((ringIndex.toFloat() * spec.ringWidthMm)) / halfExtentMm)
            drawCircle(
                color = TARGET_RING_STROKE_COLOR,
                radius = radiusPx,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4f),
            )
        }
        drawCircle(
            color = TARGET_RING_STROKE_COLOR,
            radius = layout.scoringRadiusPx * ((0.5f * spec.ringWidthMm) / halfExtentMm),
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2f),
        )
    }
}

@Composable
private fun ShotRow(
    shot: ShotRowUi,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface

    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(shot.label, fontWeight = FontWeight.SemiBold)
                Text("${shot.scoreLabel} • ${shot.clockLabel}")
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(when (shot.status) { com.autoarchery.app.ui.model.ShotStatus.Pending -> R.string.status_pending; com.autoarchery.app.ui.model.ShotStatus.Confirmed -> R.string.status_confirmed; com.autoarchery.app.ui.model.ShotStatus.Edited -> R.string.shot_edited; com.autoarchery.app.ui.model.ShotStatus.LowConfidence -> R.string.status_low }))
                Text(stringResource(if (shot.isEdited) R.string.shot_edited else R.string.shot_auto))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionSetupSheet(
    setup: SessionSetupUi,
    onAthleteSelected: (String) -> Unit,
    onDistanceSelected: (Int) -> Unit,
    onArrowsPerEndSelected: (Int) -> Unit,
    onTargetFaceSelected: (String) -> Unit,
    onTrainingGoalChanged: (String) -> Unit,
    onCreateSession: () -> Unit,
) {
    val setupChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.session_setup_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.select_athlete), fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            setup.athleteOptions.forEach { athlete ->
                FilterChip(
                    selected = setup.selectedAthleteId == athlete.id,
                    onClick = { onAthleteSelected(athlete.id) },
                    colors = setupChipColors,
                    label = { Text(athlete.name) },
                )
            }
        }
        Text(stringResource(R.string.select_distance), fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(18, 30, 50, 70).forEach { distance ->
                FilterChip(
                    selected = setup.selectedDistanceM == distance,
                    onClick = { onDistanceSelected(distance) },
                    colors = setupChipColors,
                    label = { Text("${distance}m") },
                )
            }
        }
        Text(stringResource(R.string.arrows_per_end), fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 6).forEach { count ->
                FilterChip(
                    selected = setup.selectedArrowsPerEnd == count,
                    onClick = { onArrowsPerEndSelected(count) },
                    colors = setupChipColors,
                    label = { Text(count.toString()) },
                )
            }
        }
        Text(stringResource(R.string.target_face), fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            targetFaceOptions().forEach { (value, label) ->
                FilterChip(
                    selected = setup.selectedTargetFace == value,
                    onClick = { onTargetFaceSelected(value) },
                    colors = setupChipColors,
                    label = { Text(label) },
                )
            }
        }
        OutlinedTextField(
            value = setup.trainingGoal,
            onValueChange = onTrainingGoalChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.training_goal_hint)) },
        )
        Button(onClick = onCreateSession, enabled = setup.canCreate, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.create_session))
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun formatMm(value: Float): String = String.format(Locale.US, "%.1f", value)

@Composable
private fun targetFaceOptions(): List<Pair<String, String>> =
    supportedTargetFaceSpecs().map { spec ->
        spec.code to when (spec.code) {
            "FACE_122CM" -> stringResource(R.string.face_122cm)
            "FACE_80CM_10_RING" -> stringResource(R.string.face_80cm_10_ring)
            "FACE_80CM_5_RING" -> stringResource(R.string.face_80cm_inner_5)
            "FACE_60CM_6_RING" -> stringResource(R.string.face_60cm_inner_6)
            "FACE_40CM" -> stringResource(R.string.face_40cm)
            "FACE_40CM_6_RING" -> stringResource(R.string.face_40cm_inner_6)
            "FACE_20CM_5_RING" -> stringResource(R.string.face_20cm_inner_5)
            "FACE_20CM_5_RING_TRIPLE" -> stringResource(R.string.face_20cm_inner_5_triple)
            else -> spec.label
        }
    }

private val TARGET_PAPER_COLOR = Color(0xFFF7F3E8)
private val TARGET_RING_STROKE_COLOR = Color(0xFF2F2A24)
