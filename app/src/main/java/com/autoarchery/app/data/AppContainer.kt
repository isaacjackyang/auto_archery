package com.autoarchery.app.data

import com.autoarchery.app.data.local.AutoArcheryDatabase
import com.autoarchery.app.data.repository.AthleteRepository
import com.autoarchery.app.data.repository.PreferenceRepository
import com.autoarchery.app.data.repository.SessionRepository
import com.autoarchery.app.speech.SpeechCoordinator

class AppContainer(
    private val applicationContext: android.content.Context,
    database: AutoArcheryDatabase,
) {
    val preferenceRepository = PreferenceRepository(database.appPreferenceDao())
    val athleteRepository = AthleteRepository(database.athleteDao())
    val sessionRepository = SessionRepository(
        trainingSessionDao = database.trainingSessionDao(),
        trainingEndDao = database.trainingEndDao(),
        arrowShotDao = database.arrowShotDao(),
        coachNoteDao = database.coachNoteDao(),
        operationLogDao = database.operationLogDao(),
        sessionExportDao = database.sessionExportDao(),
    )
    val speechCoordinator = SpeechCoordinator(applicationContext)
}
