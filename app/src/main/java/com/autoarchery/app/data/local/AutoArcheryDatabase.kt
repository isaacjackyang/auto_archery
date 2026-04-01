package com.autoarchery.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.autoarchery.app.data.local.dao.AppPreferenceDao
import com.autoarchery.app.data.local.dao.ArrowShotDao
import com.autoarchery.app.data.local.dao.AthleteDao
import com.autoarchery.app.data.local.dao.CoachNoteDao
import com.autoarchery.app.data.local.dao.OperationLogDao
import com.autoarchery.app.data.local.dao.SessionExportDao
import com.autoarchery.app.data.local.dao.TrainingEndDao
import com.autoarchery.app.data.local.dao.TrainingSessionDao
import com.autoarchery.app.data.local.entity.AppPreferenceEntity
import com.autoarchery.app.data.local.entity.ArrowShotEntity
import com.autoarchery.app.data.local.entity.AthleteProfileEntity
import com.autoarchery.app.data.local.entity.CoachNoteEntity
import com.autoarchery.app.data.local.entity.OperationLogEntity
import com.autoarchery.app.data.local.entity.SessionExportEntity
import com.autoarchery.app.data.local.entity.TrainingEndEntity
import com.autoarchery.app.data.local.entity.TrainingSessionEntity

@Database(
    entities = [
        AthleteProfileEntity::class,
        TrainingSessionEntity::class,
        TrainingEndEntity::class,
        ArrowShotEntity::class,
        CoachNoteEntity::class,
        OperationLogEntity::class,
        SessionExportEntity::class,
        AppPreferenceEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AutoArcheryDatabase : RoomDatabase() {
    abstract fun athleteDao(): AthleteDao
    abstract fun trainingSessionDao(): TrainingSessionDao
    abstract fun trainingEndDao(): TrainingEndDao
    abstract fun arrowShotDao(): ArrowShotDao
    abstract fun coachNoteDao(): CoachNoteDao
    abstract fun operationLogDao(): OperationLogDao
    abstract fun sessionExportDao(): SessionExportDao
    abstract fun appPreferenceDao(): AppPreferenceDao
}
