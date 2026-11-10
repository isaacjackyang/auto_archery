package com.autoarchery.app.ui.settings

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoarchery.app.AutoArcheryApplication
import com.autoarchery.app.R
import com.autoarchery.app.domain.targetFaceLabel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

data class SettingsUiState(
    val defaultAthleteName: String,
    val athletes: List<AthleteItemUi>,
    val languages: List<LanguageOptionUi>,
    val selectedLanguage: LanguageOptionUi,
    val ttsFollowDisplayLanguage: Boolean,
    val recentSessions: List<TrainingRecordUi>,
    val isLoading: Boolean,
    val isAddAthleteDialogVisible: Boolean,
    val draftAthleteName: String,
    val errorMessage: String?,
)

data class AthleteItemUi(
    val id: String,
    val name: String,
    val status: String,
    val isDefault: Boolean,
)

data class TrainingRecordUi(
    val id: String,
    val title: String,
    val subtitle: String,
    val scoreSummary: String,
)

enum class LanguageOptionUi(
    @param:StringRes val labelRes: Int,
    val code: String,
) {
    TraditionalChinese(R.string.language_traditional_chinese, "zh-TW"),
    English(R.string.language_english, "en"),
}

class SettingsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContainer = (application as AutoArcheryApplication).appContainer
    private val athleteRepository = appContainer.athleteRepository
    private val preferenceRepository = appContainer.preferenceRepository
    private val sessionRepository = appContainer.sessionRepository
    private val dateFormatter: DateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultAthleteName = "",
            athletes = emptyList(),
            languages = LanguageOptionUi.entries.toList(),
            selectedLanguage = LanguageOptionUi.TraditionalChinese,
            ttsFollowDisplayLanguage = true,
            recentSessions = emptyList(),
            isLoading = false,
            isAddAthleteDialogVisible = false,
            draftAthleteName = "",
            errorMessage = null,
        ),
    )

    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectLanguage(language: LanguageOptionUi) = savePreference {
        preferenceRepository.setDisplayLanguage(language.code)
    }

    fun toggleTtsFollowDisplayLanguage(enabled: Boolean) = savePreference {
        preferenceRepository.toggleTtsFollowDisplayLanguage(enabled)
    }

    fun setDefaultAthlete(athleteId: String) = savePreference {
        preferenceRepository.setDefaultAthlete(athleteId)
    }

    private fun savePreference(change: suspend () -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                change()
                refresh()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.value = _uiState.value.copy(isLoading = false,
                    errorMessage = getApplication<Application>().getString(R.string.settings_save_error))
            }
        }
    }

    fun openAddAthleteDialog() {
        _uiState.value = _uiState.value.copy(
            isAddAthleteDialogVisible = true,
            draftAthleteName = "",
            errorMessage = null,
        )
    }

    fun dismissAddAthleteDialog() {
        _uiState.value = _uiState.value.copy(
            isAddAthleteDialogVisible = false,
            draftAthleteName = "",
        )
    }

    fun updateDraftAthleteName(name: String) {
        _uiState.value = _uiState.value.copy(draftAthleteName = name)
    }

    fun saveAthlete() {
        if (_uiState.value.isLoading) return
        val draftName = _uiState.value.draftAthleteName.trim()
        if (draftName.isBlank()) {
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val athlete = athleteRepository.addAthlete(draftName)
                val preference = preferenceRepository.getPreference()
                if (preference.defaultAthleteId == null) {
                    preferenceRepository.setDefaultAthlete(athlete.id)
                }
                _uiState.value = _uiState.value.copy(
                    isAddAthleteDialogVisible = false,
                    draftAthleteName = "",
                )
                refresh()
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.value = _uiState.value.copy(isLoading = false,
                    errorMessage = getApplication<Application>().getString(R.string.settings_save_error))
            }
        }
    }

    fun refreshLanguage() { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                ensureSeedData()

                val preference = preferenceRepository.getPreference()
                val athletes = athleteRepository.getAll()
                val defaultAthlete = athletes.firstOrNull { it.id == preference.defaultAthleteId }
                val recentSessions = sessionRepository.getRecentSessions()

                _uiState.value = SettingsUiState(
                    defaultAthleteName = defaultAthlete?.displayName ?: athletes.firstOrNull()?.displayName.orEmpty(),
                    athletes = athletes.map { athlete ->
                        AthleteItemUi(
                            id = athlete.id,
                            name = athlete.displayName,
                            status = athlete.status,
                            isDefault = athlete.id == preference.defaultAthleteId,
                        )
                    },
                    languages = LanguageOptionUi.entries.toList(),
                    selectedLanguage = LanguageOptionUi.entries.firstOrNull { it.code == preference.displayLanguage }
                        ?: LanguageOptionUi.TraditionalChinese,
                    ttsFollowDisplayLanguage = preference.ttsFollowDisplayLanguage,
                    recentSessions = recentSessions.map { session ->
                        val dateText = dateFormatter.format(Date(session.startedAt))
                        TrainingRecordUi(
                            id = session.id,
                            title = session.title,
                            subtitle = "$dateText • ${session.distanceM}m • ${targetFaceLabel(session.targetFaceCode)}",
                            scoreSummary = if (session.totalArrows > 0) {
                                getApplication<Application>().getString(R.string.record_summary, session.totalScore, session.totalArrows)
                            } else {
                                getApplication<Application>().getString(R.string.no_arrows_yet)
                            },
                        )
                    },
                    isLoading = false,
                    isAddAthleteDialogVisible = _uiState.value.isAddAthleteDialogVisible,
                    draftAthleteName = _uiState.value.draftAthleteName,
                    errorMessage = null,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unknown error",
                    isAddAthleteDialogVisible = _uiState.value.isAddAthleteDialogVisible,
                    draftAthleteName = _uiState.value.draftAthleteName,
                )
            }
        }
    }

    private suspend fun ensureSeedData() {
        val seedAthlete = athleteRepository.ensureSeedAthlete()
        val preference = preferenceRepository.getPreference()
        if (preference.defaultAthleteId == null) {
            preferenceRepository.setDefaultAthlete(seedAthlete.id)
        }
    }

}
