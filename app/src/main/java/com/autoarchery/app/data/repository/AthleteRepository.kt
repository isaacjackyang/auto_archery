package com.autoarchery.app.data.repository

import com.autoarchery.app.data.local.dao.AthleteDao
import com.autoarchery.app.data.local.entity.AthleteProfileEntity
import java.util.UUID

class AthleteRepository(
    private val athleteDao: AthleteDao,
) {
    suspend fun getAll(): List<AthleteProfileEntity> = athleteDao.getAll()

    suspend fun getById(athleteId: String): AthleteProfileEntity? = athleteDao.getById(athleteId)

    suspend fun ensureSeedAthlete(): AthleteProfileEntity {
        if (athleteDao.countAll() > 0) {
            return athleteDao.getAll().first()
        }

        val now = System.currentTimeMillis()
        val athlete = AthleteProfileEntity(
            id = UUID.randomUUID().toString(),
            displayName = "Athlete 1",
            createdAt = now,
            updatedAt = now,
        )
        athleteDao.upsert(athlete)
        return athlete
    }

    suspend fun addAthlete(name: String): AthleteProfileEntity {
        val now = System.currentTimeMillis()
        val athlete = AthleteProfileEntity(
            id = UUID.randomUUID().toString(),
            displayName = name.trim(),
            createdAt = now,
            updatedAt = now,
        )
        athleteDao.upsert(athlete)
        return athlete
    }

    suspend fun setStatus(athleteId: String, status: String) {
        val athlete = athleteDao.getById(athleteId) ?: return
        athleteDao.upsert(
            athlete.copy(
                status = status,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}

