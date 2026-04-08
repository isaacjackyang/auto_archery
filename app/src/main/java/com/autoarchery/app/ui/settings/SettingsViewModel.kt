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

    fun selectLanguage(language: LanguageOptionUi) {
        viewModelScope.launch {
            preferenceRepository.setDisplayLanguage(language.code)
            refresh()
        }
    }

    fun toggleTtsFollowDisplayLanguage(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.toggleTtsFollowDisplayLanguage(enabled)
            refresh()
        }
    }

    fun setDefaultAthlete(athleteId: String) {
        viewModelScope.launch {
            preferenceRepository.setDefaultAthlete(athleteId)
            refresh()
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
        val draftName = _uiState.value.draftAthleteName.trim()
        if (draftName.isBlank()) {
            return
        }

        viewModelScope.launch {
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
        }
    }

    private fun refresh() {
        val addAthleteDialogVisible = _uiState.value.isAddAthleteDialogVisible
        val draftAthleteName = _uiState.value.draftAthleteName
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
                                "Score ${session.totalScore} / ${session.totalArrows}"
                            } else {
                                "No arrows recorded"
                            },
                        )
                    },
                    isLoading = false,
                    isAddAthleteDialogVisible = addAthleteDialogVisible,
                    draftAthleteName = draftAthleteName,
                    errorMessage = null,
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Unknown error",
                    isAddAthleteDialogVisible = addAthleteDialogVisible,
                    draftAthleteName = draftAthleteName,
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
