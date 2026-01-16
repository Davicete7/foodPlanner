package com.example.foodplanner.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val ingredients: MutableList<String> = mutableListOf(),
    val cart: MutableList<String> = mutableListOf(),
    val recipes: MutableList<String> = mutableListOf()
)