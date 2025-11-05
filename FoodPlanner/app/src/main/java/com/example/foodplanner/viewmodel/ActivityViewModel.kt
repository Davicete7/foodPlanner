package com.example.foodplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.data.db.AppDatabase
import com.example.foodplanner.data.db.entities.ActivityEntry
import com.example.foodplanner.data.repo.ActivityRepository
import kotlinx.coroutines.launch
import java.time.Instant

class ActivityViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ActivityRepository(AppDatabase.get(app))
    val entries = repo.entries.asLiveData()

    fun add(title: String, description: String, instant: Instant) = viewModelScope.launch {
        repo.add(ActivityEntry(title = title, description = description, dateTimeEpochSeconds = instant.epochSecond))
    }
    fun update(e: ActivityEntry) = viewModelScope.launch { repo.update(e) }
    fun delete(e: ActivityEntry) = viewModelScope.launch { repo.delete(e) }
}
