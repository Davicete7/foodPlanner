package com.example.foodplanner.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val email: String = "",
    val ingredients: List<String> = emptyList(),
    val cart: List<String> = emptyList()
)