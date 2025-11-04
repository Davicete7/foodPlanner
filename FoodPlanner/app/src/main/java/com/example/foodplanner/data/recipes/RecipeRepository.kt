package com.example.foodplanner.data.recipes

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class RecipeRepository(private val context: Context) {
    suspend fun loadAll(): List<RecipeDTO> = withContext(Dispatchers.IO) {
        val json = context.assets.open("recipes.json").bufferedReader().use { it.readText() }
        Json.decodeFromString(json)
    }
}
