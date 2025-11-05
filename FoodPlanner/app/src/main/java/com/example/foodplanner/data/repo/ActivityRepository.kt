package com.example.foodplanner.data.repo

import com.example.foodplanner.data.db.AppDatabase
import com.example.foodplanner.data.db.entities.ActivityEntry
import kotlinx.coroutines.flow.Flow

class ActivityRepository(private val db: AppDatabase) {
    val entries: Flow<List<ActivityEntry>> = db.activityDao().observeAll()
    suspend fun get(id: Long) = db.activityDao().getById(id)
    suspend fun add(e: ActivityEntry) = db.activityDao().insert(e)
    suspend fun update(e: ActivityEntry) = db.activityDao().update(e)
    suspend fun delete(e: ActivityEntry) = db.activityDao().delete(e)
}
