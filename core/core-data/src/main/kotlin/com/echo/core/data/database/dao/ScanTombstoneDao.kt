package com.echo.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.echo.core.data.database.entity.ScanTombstoneEntity

@Dao
interface ScanTombstoneDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tombstone: ScanTombstoneEntity)
}
