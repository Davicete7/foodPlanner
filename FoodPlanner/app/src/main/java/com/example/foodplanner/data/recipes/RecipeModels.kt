package com.example.foodplanner.data.recipes

@kotlinx.serialization.Serializable
data class RecipeDTO(
    val id: Long,
    val name: String,
    val ingredients: List<RecipeIngredientDTO>
)

@kotlinx.serialization.Serializable
data class RecipeIngredientDTO(
    val name: String,
    val quantity: Double,
    val unit: String
)
