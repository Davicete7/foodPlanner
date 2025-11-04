package com.example.foodplanner.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "activity_entries")
data class ActivityEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val dateTimeEpochSeconds: Long
) {
    fun asInstant(): Instant = Instant.ofEpochSecond(dateTimeEpochSeconds)
}