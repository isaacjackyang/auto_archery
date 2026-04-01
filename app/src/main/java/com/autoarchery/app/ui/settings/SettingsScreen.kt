package com.autoarchery.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autoarchery.app.R

@Composable
fun SettingsRoute(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onSelectLanguage = viewModel::selectLanguage,
        onToggleTtsFollowDisplayLanguage = viewModel::toggleTtsFollowDisplayLanguage,
        onSetDefaultAthlete = viewModel::setDefaultAthlete,
        onOpenAddAthleteDialog = viewModel::openAddAthleteDialog,
        onDismissAddAthleteDialog = viewModel::dismissAddAthleteDialog,
        onDraftAthleteNameChanged = viewModel::updateDraftAthleteName,
        onSaveAthlete = viewModel::saveAthlete,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    onNavigateBack: () -> Unit,
    onSelectLanguage: (LanguageOptionUi) -> Unit,
    onToggleTtsFollowDisplayLanguage: (Boolean) -> Unit,
    onSetDefaultAthlete: (String) -> Unit,
    onOpenAddAthleteDialog: () -> Unit,
    onDismissAddAthleteDialog: () -> Unit,
    onDraftAthleteNameChanged: (String) -> Unit,
    onSaveAthlete: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                actions = {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.close))
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
            item {
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.select_athlete), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.default_athlete, uiState.defaultAthleteName.ifBlank { "-" }))
                        uiState.athletes.forEach { athlete ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(athlete.name, fontWeight = FontWeight.SemiBold)
                                    Text(athlete.status)
                                }
                                FilterChip(
                                    selected = athlete.isDefault,
                                    onClick = { onSetDefaultAthlete(athlete.id) },
                                    label = {
                                        Text(
                                            if (athlete.isDefault) {
                                                stringResource(R.string.default_badge)
                                            } else {
                                                stringResource(R.string.set_default)
                                            },
                                        )
                                    },
                                )
                            }
                        }
                        OutlinedButton(onClick = onOpenAddAthleteDialog) {
                            Text(stringResource(R.string.add_athlete))
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.language), fontWeight = FontWeight.Bold)
                        uiState.languages.forEach { option ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = uiState.selectedLanguage == option,
                                    onClick = { onSelectLanguage(option) },
                                )
                                Text(stringResource(option.labelRes))
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.tts_follow_display_language))
                            Switch(
                                checked = uiState.ttsFollowDisplayLanguage,
                                onCheckedChange = onToggleTtsFollowDisplayLanguage,
                            )
                        }
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.training_records), fontWeight = FontWeight.Bold)
                        if (uiState.recentSessions.isEmpty()) {
                            Text(stringResource(R.string.no_training_records))
                        } else {
                            uiState.recentSessions.forEach { session ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(session.title, fontWeight = FontWeight.SemiBold)
                                    Text(session.subtitle)
                                    Text(session.scoreSummary)
                                }
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let { errorMessage ->
                item {
                    Text(
                        text = errorMessage,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (uiState.isAddAthleteDialogVisible) {
        AlertDialog(
            onDismissRequest = onDismissAddAthleteDialog,
            title = { Text(stringResource(R.string.add_athlete)) },
            text = {
                OutlinedTextField(
                    value = uiState.draftAthleteName,
                    onValueChange = onDraftAthleteNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.athlete_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onSaveAthlete,
                    enabled = uiState.draftAthleteName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAddAthleteDialog) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
