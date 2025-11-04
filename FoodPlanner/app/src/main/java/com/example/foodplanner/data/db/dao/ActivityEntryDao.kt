package com.example.foodplanner.data.db.dao

import androidx.room.*
import com.example.foodplanner.data.db.entities.ActivityEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEntryDao {
    @Query("SELECT * FROM activity_entries ORDER BY dateTimeEpochSeconds DESC")
    fun observeAll(): Flow<List<ActivityEntry>>

    @Query("SELECT * FROM activity_entries WHERE id = :id")
    suspend fun getById(id: Long): ActivityEntry?

    @Insert suspend fun insert(entry: ActivityEntry): Long
    @Update suspend fun update(entry: ActivityEntry)
    @Delete suspend fun delete(entry: ActivityEntry)
}