package com.autoarchery.app.data.repository

import com.autoarchery.app.data.local.dao.AppPreferenceDao
import com.autoarchery.app.data.local.entity.AppPreferenceEntity

class PreferenceRepository(
    private val preferenceDao: AppPreferenceDao,
) {
    fun observePreference() = preferenceDao.observePreference()

    suspend fun getPreference(): AppPreferenceEntity {
        val existing = preferenceDao.getPreference()
        if (existing != null) return existing

        val now = System.currentTimeMillis()
        val created = AppPreferenceEntity(
            createdAt = now,
            updatedAt = now,
        )
        preferenceDao.upsert(created)
        return created
    }

    suspend fun setDefaultAthlete(athleteId: String) {
        val preference = getPreference()
        preferenceDao.upsert(
            preference.copy(
                defaultAthleteId = athleteId,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun setDisplayLanguage(language: String) {
        val preference = getPreference()
        preferenceDao.upsert(
            preference.copy(
                displayLanguage = language,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun toggleTtsFollowDisplayLanguage(enabled: Boolean) {
        val preference = getPreference()
        preferenceDao.upsert(
            preference.copy(
                ttsFollowDisplayLanguage = enabled,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}

