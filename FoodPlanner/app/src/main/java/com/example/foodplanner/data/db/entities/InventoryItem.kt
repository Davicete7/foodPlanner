package com.example.foodplanner.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredientId: Long,
    val quantity: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)