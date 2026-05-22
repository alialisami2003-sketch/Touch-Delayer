package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CircleOverlayDao {
    @Query("SELECT * FROM circle_overlays ORDER BY id ASC")
    fun getAllOverlays(): Flow<List<CircleOverlay>>

    @Query("SELECT * FROM circle_overlays WHERE id = :id")
    suspend fun getOverlayById(id: Int): CircleOverlay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverlay(overlay: CircleOverlay): Long

    @Update
    suspend fun updateOverlay(overlay: CircleOverlay)

    @Delete
    suspend fun deleteOverlay(overlay: CircleOverlay)

    @Query("DELETE FROM circle_overlays")
    suspend fun deleteAll()
}
