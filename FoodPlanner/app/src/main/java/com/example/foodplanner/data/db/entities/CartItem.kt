package com.example.foodplanner.data.db.entities

import com.google.firebase.firestore.DocumentId

data class CartItem(
    @DocumentId var id: String? = null,
    val name: String = "",
    val searchableName: String = "",
    val quantity: Double = 0.0,
    val unit: String = "pcs"
) {
    // No-arg constructor for Firestore
    constructor() : this(null, "", "", 0.0, "pcs")
}