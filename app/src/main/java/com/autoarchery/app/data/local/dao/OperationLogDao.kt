package com.autoarchery.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import com.autoarchery.app.data.local.entity.OperationLogEntity

@Dao
interface OperationLogDao {
    @Insert
    suspend fun insert(log: OperationLogEntity)
}

