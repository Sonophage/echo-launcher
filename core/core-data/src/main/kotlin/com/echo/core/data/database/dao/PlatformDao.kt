package com.echo.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.echo.core.data.database.entity.PlatformEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlatformDao {
    @Query("SELECT * FROM platforms ORDER BY name ASC")
    fun observeAll(): Flow<List<PlatformEntity>>

    @Query("SELECT * FROM platforms WHERE id = :id")
    suspend fun getById(id: String): PlatformEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(platforms: List<PlatformEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(platform: PlatformEntity)

    @Update
    suspend fun update(platform: PlatformEntity)

    @Query("UPDATE platforms SET is_pinned_to_bar = :pinned, bar_position = :position WHERE id = :id")
    suspend fun setPinned(id: String, pinned: Boolean, position: Int)

    @Query("UPDATE platforms SET preferred_emulator_package = :packageName WHERE id = :id")
    suspend fun setPreferredEmulator(id: String, packageName: String?)
}
