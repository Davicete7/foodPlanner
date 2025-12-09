package com.example.foodplanner.data.db.entities

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class InventoryItem(
    @DocumentId var id: String? = null,
    val name: String = "",
    val searchableName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "pcs",
    val expirationDate: Long? = null,
    @ServerTimestamp val updatedAt: Date? = null
) {
    // No-arg constructor for Firestore
    constructor() : this(null, "", "", 0.0, "pcs", null, null)
}