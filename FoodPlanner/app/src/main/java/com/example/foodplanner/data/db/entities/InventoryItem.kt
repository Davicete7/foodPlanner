package com.example.foodplanner.data.db.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import java.util.concurrent.TimeUnit

data class InventoryItem(
    @DocumentId var id: String? = null,
    val name: String = "",
    val searchableName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    val expirationDate: Long? = null,
    @ServerTimestamp val updatedAt: Date? = null
) {
    // No-arg constructor required for Firestore
    constructor() : this(null, "", "", 0.0, "pcs", null, null)

    fun isExpiringSoon(days: Int = 3): Boolean {
        if (expirationDate == null) return false
        val diff = expirationDate - System.currentTimeMillis()
        return diff > 0 && diff <= TimeUnit.DAYS.toMillis(days.toLong())
    }

    fun isLowStock(threshold: Double = 1.0): Boolean {
        return quantity <= threshold
    }
}