package com.autoarchery.app.ui.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoarchery.app.R

@Composable
fun MetricsRoute(
    sessionId: String,
    onBackToLive: () -> Unit,
    viewModel: MetricsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.bindSession(sessionId)
    }

    MetricsScreen(uiState = uiState, onBackToLive = onBackToLive)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MetricsScreen(
    uiState: MetricsUiState,
    onBackToLive: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.athleteName.isBlank()) {
                            stringResource(R.string.metrics_title)
                        } else {
                            "${uiState.athleteName} • ${stringResource(R.string.metrics_title)}"
                        },
                    )
                },
                actions = {
                    TextButton(onClick = onBackToLive) {
                        Text(stringResource(R.string.back_to_live))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.errorMessage != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            text = uiState.errorMessage,
                            modifier = Modifier.padding(16.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(uiState.sessionTitle.ifBlank { stringResource(R.string.loading) }, fontWeight = FontWeight.Bold)
                        if (uiState.trainingGoal.isNotBlank()) {
                            Text("${stringResource(R.string.training_goal)}: ${uiState.trainingGoal}")
                        }
                        if (uiState.tags.isNotEmpty()) {
                            Text("${stringResource(R.string.tags)}: ${uiState.tags.joinToString()}")
                        }
                        if (uiState.sessionSummary.isNotBlank()) {
                            Text(uiState.sessionSummary)
                        }
                        if (uiState.coachSummary.isNotBlank()) {
                            Text(uiState.coachSummary)
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.comparison), fontWeight = FontWeight.Bold)
                        Text(uiState.comparisonHeadline.ifBlank { stringResource(R.string.loading) })
                        Text(uiState.comparisonDetails.ifBlank { stringResource(R.string.loading) })
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.session_summary), fontWeight = FontWeight.Bold)
                        if (uiState.endSummaries.isEmpty()) {
                            Text(stringResource(R.string.loading))
                        } else {
                            uiState.endSummaries.forEach { row -> Text(row) }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.arrow_table), fontWeight = FontWeight.Bold)
                        if (uiState.arrowRows.isEmpty()) {
                            Text(stringResource(R.string.loading))
                        } else {
                            uiState.arrowRows.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(row.left)
                                    Text(row.right)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(stringResource(R.string.coach_notes), fontWeight = FontWeight.Bold)
                        uiState.notes.forEach { note -> Text(note) }
                    }
                }
            }

            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text(stringResource(R.string.export_png)) }
                    OutlinedButton(onClick = {}) { Text(stringResource(R.string.export_pdf)) }
                    OutlinedButton(onClick = {}) { Text(stringResource(R.string.export_csv)) }
                }
            }
        }
    }
}
