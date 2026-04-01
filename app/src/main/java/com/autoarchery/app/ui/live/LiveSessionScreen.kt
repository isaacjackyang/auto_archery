package com.autoarchery.app.ui.live

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoarchery.app.R
import com.autoarchery.app.domain.TargetScoringBandId
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

    LaunchedEffect(sessionId) {
        viewModel.bindSession(sessionId)
    }

    LiveSessionScreen(
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
        onMarkCalibrated = viewModel::markCalibrated,
        onResetCalibration = viewModel::resetCalibration,
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
    onMarkCalibrated: () -> Unit,
    onResetCalibration: () -> Unit,
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
    Scaffold(
        topBar = {
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
        },
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val wideLayout = maxWidth > maxHeight || maxWidth >= 900.dp

            if (wideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CameraPanel(
                        uiState = uiState,
                        onTogglePaused = onTogglePaused,
                        onMarkCalibrated = onMarkCalibrated,
                        onResetCalibration = onResetCalibration,
                        onNextRound = onConfirmEnd,
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
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CameraPanel(
                        uiState = uiState,
                        onTogglePaused = onTogglePaused,
                        onMarkCalibrated = onMarkCalibrated,
                        onResetCalibration = onResetCalibration,
                        onNextRound = onConfirmEnd,
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
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = true),
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
        ModalBottomSheet(onDismissRequest = onDismissAdjustSheet) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            ) {
                val useSplitLayout = maxWidth >= 560.dp

                if (useSplitLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onDismissAdjustSheet) { Text(stringResource(R.string.close)) }
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            adjustmentDraft = uiState.adjustmentDraft,
                            onUpdateAdjustmentPosition = onUpdateAdjustmentPosition,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onDismissAdjustSheet) { Text(stringResource(R.string.close)) }
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
            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    if (uiState.isSessionSetupVisible) {
        ModalBottomSheet(onDismissRequest = onDismissSessionSetup) {
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
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CameraPanel(
    uiState: LiveSessionUiState,
    onTogglePaused: () -> Unit,
    onMarkCalibrated: () -> Unit,
    onResetCalibration: () -> Unit,
    onNextRound: () -> Unit,
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
            modifier = (if (isWideLayout) Modifier.fillMaxSize() else Modifier.fillMaxWidth()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (uiState.athleteName.isBlank()) stringResource(R.string.loading) else stringResource(R.string.current_athlete_label, uiState.athleteName),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (isWideLayout) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .fillMaxHeight(),
                    ) {
                        CameraPreviewSurface(
                            modifier = Modifier.fillMaxSize(),
                            sessionId = uiState.sessionId,
                            knownShotCount = uiState.shots.size,
                            isPaused = uiState.isPaused || uiState.sessionId == null,
                            onCameraReadyChanged = onCameraReadyChanged,
                            onCameraProfileResolved = onCameraProfileResolved,
                            onCalibrationDetected = onCalibrationDetected,
                            onShotDetected = onShotDetected,
                        )
                        TargetFrameOverlay()
                    }
                    CameraControlColumn(
                        uiState = uiState,
                        onTogglePaused = onTogglePaused,
                        onMarkCalibrated = onMarkCalibrated,
                        onResetCalibration = onResetCalibration,
                        onNextRound = onNextRound,
                        isWideLayout = true,
                        modifier = Modifier
                            .width(220.dp)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                ) {
                    CameraPreviewSurface(
                        modifier = Modifier.fillMaxSize(),
                        sessionId = uiState.sessionId,
                        knownShotCount = uiState.shots.size,
                        isPaused = uiState.isPaused || uiState.sessionId == null,
                        onCameraReadyChanged = onCameraReadyChanged,
                        onCameraProfileResolved = onCameraProfileResolved,
                        onCalibrationDetected = onCalibrationDetected,
                        onShotDetected = onShotDetected,
                    )
                    TargetFrameOverlay()
                }
                CameraControlColumn(
                    uiState = uiState,
                    onTogglePaused = onTogglePaused,
                    onMarkCalibrated = onMarkCalibrated,
                    onResetCalibration = onResetCalibration,
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
    onMarkCalibrated: () -> Unit,
    onResetCalibration: () -> Unit,
    onNextRound: () -> Unit,
    isWideLayout: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isWideLayout) {
        Column(
            modifier = modifier
                .padding(start = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AssistChip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                    label = { Text(uiState.zoomLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AssistChip(
                        modifier = Modifier.weight(1f),
                        onClick = {},
                        label = { Text(uiState.currentEndLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                    AssistChip(
                        modifier = Modifier.weight(1f),
                        onClick = {},
                        label = { Text(uiState.currentArrowLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
                AssistChip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {},
                    label = {
                        Text(
                            "${stringResource(R.string.calibration_status)}: ${uiState.calibrationLabel}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                AssistChip(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onTogglePaused,
                    label = {
                        Text(
                            if (uiState.isPaused) stringResource(R.string.resume_auto) else stringResource(R.string.pause_auto),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onMarkCalibrated,
                        enabled = uiState.calibrationState != CalibrationState.Calibrated,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.calibrate), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onResetCalibration,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.recalibrate), maxLines = 1)
                    }
                }
                Button(
                    onClick = onNextRound,
                    enabled = uiState.sessionId != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.next_round), maxLines = 1)
                }
            }
        }
    } else {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(uiState.zoomLabel) })
                AssistChip(onClick = {}, label = { Text(uiState.currentEndLabel) })
                AssistChip(onClick = {}, label = { Text(uiState.currentArrowLabel) })
                AssistChip(onClick = {}, label = { Text("${stringResource(R.string.calibration_status)}: ${uiState.calibrationLabel}") })
                AssistChip(
                    onClick = onTogglePaused,
                    label = { Text(if (uiState.isPaused) stringResource(R.string.resume_auto) else stringResource(R.string.pause_auto)) },
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMarkCalibrated,
                    enabled = uiState.calibrationState != CalibrationState.Calibrated,
                ) {
                    Text(stringResource(R.string.calibrate))
                }
                OutlinedButton(onClick = onResetCalibration) {
                    Text(stringResource(R.string.recalibrate))
                }
                Button(
                    onClick = onNextRound,
                    enabled = uiState.sessionId != null,
                ) {
                    Text(stringResource(R.string.next_round))
                }
            }
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
        modifier = (if (isWideLayout) modifier.fillMaxHeight() else modifier)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(
                modifier = Modifier.padding(if (isWideLayout) 12.dp else 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DigitalTargetCard(
                    markers = uiState.filteredMarkers,
                    selectedShotId = uiState.selectedShot?.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isWideLayout) 200.dp else 240.dp),
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
                        label = "All",
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
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.shot_list), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (uiState.filteredShots.isEmpty()) {
                    Text(stringResource(R.string.loading))
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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

        uiState.endSummary?.let { endSummary ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                detectDragGestures(
                    onDragStart = { offset ->
                        val normalized = offset.toNormalized(size.width.toFloat(), size.height.toFloat())
                        onUpdateAdjustmentPosition(normalized.x, normalized.y)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val normalized = change.position.toNormalized(size.width.toFloat(), size.height.toFloat())
                        onUpdateAdjustmentPosition(normalized.x, normalized.y)
                    },
                )
            }
    } else {
        Modifier.fillMaxSize()
    }

    Surface(modifier = modifier, tonalElevation = 2.dp) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.target_face_classic),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
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
                    val backgroundColor = targetFaceBackgroundColor(marker.xNormalized, marker.yNormalized)
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

private fun Offset.toNormalized(width: Float, height: Float): Offset {
    val targetLayout = targetLayout(width, height)
    val normalizedX = ((x - targetLayout.center.x) / targetLayout.scoringRadiusPx).coerceIn(-1f, 1f)
    val normalizedY = ((y - targetLayout.center.y) / targetLayout.scoringRadiusPx).coerceIn(-1f, 1f)
    return Offset(normalizedX, normalizedY)
}

private fun targetFaceBackgroundColor(
    xNormalized: Float,
    yNormalized: Float,
): Color {
    val radius = kotlin.math.sqrt((xNormalized * xNormalized) + (yNormalized * yNormalized))
    return when {
        radius <= 0.2f -> targetBandColor(TargetScoringBandId.Score10)
        radius <= 0.4f -> targetBandColor(TargetScoringBandId.Score8)
        radius <= 0.6f -> targetBandColor(TargetScoringBandId.Score6)
        radius <= 0.8f -> targetBandColor(TargetScoringBandId.Score4)
        else -> targetBandColor(TargetScoringBandId.Score2)
    }
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
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
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
                Text(shot.status.label)
                Text(if (shot.isEdited) "Edited" else "Auto")
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.session_setup_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.select_athlete), fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            setup.athleteOptions.forEach { athlete ->
                FilterChip(
                    selected = setup.selectedAthleteId == athlete.id,
                    onClick = { onAthleteSelected(athlete.id) },
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
                    label = { Text(count.toString()) },
                )
            }
        }
        Text(stringResource(R.string.target_face), fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("FACE_122CM" to stringResource(R.string.face_122cm), "FACE_40CM" to stringResource(R.string.face_40cm)).forEach { (value, label) ->
                FilterChip(
                    selected = setup.selectedTargetFace == value,
                    onClick = { onTargetFaceSelected(value) },
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
