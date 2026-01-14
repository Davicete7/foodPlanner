package com.example.foodplanner.data.recipes

import com.example.foodplanner.data.remote.TheMealDbApi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class RecipeRepository(
    private val userId: String // Necesitamos el userId para guardar en su colección
) {
    private val db = FirebaseFirestore.getInstance()
    private val savedRecipesCollection = db.collection("users").document(userId).collection("saved_recipes")

    // Configuración de Retrofit (Idealmente esto iría en un módulo de inyección de dependencias como Hilt)
    private val json = Json { ignoreUnknownKeys = true }
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.themealdb.com/api/json/v1/1/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    private val api = retrofit.create(TheMealDbApi::class.java)

    // --- API REMOTA ---

    suspend fun searchRecipes(query: String): List<RecipeDTO> {
        return try {
            val response = api.searchMeals(query)
            response.meals?.map { mealApi ->
                RecipeDTO(
                    id = mealApi.id,
                    name = mealApi.name,
                    instructions = mealApi.instructions,
                    imageUrl = mealApi.thumbnail,
                    ingredients = mealApi.toDomainIngredients()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getRecipeFromApi(id: String): RecipeDTO? {
        return try {
            val response = api.getMealById(id)
            response.meals?.firstOrNull()?.let { mealApi ->
                 RecipeDTO(
                    id = mealApi.id,
                    name = mealApi.name,
                    instructions = mealApi.instructions,
                    imageUrl = mealApi.thumbnail,
                    ingredients = mealApi.toDomainIngredients()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    // --- FIREBASE (Recetas Guardadas) ---

    suspend fun saveRecipe(recipe: RecipeDTO) {
        // Usamos el ID de la API como ID del documento
        savedRecipesCollection.document(recipe.id).set(recipe).await()
    }

    suspend fun getSavedRecipes(): List<RecipeDTO> {
        return try {
            val snapshot = savedRecipesCollection.get().await()
            snapshot.toObjects<RecipeDTO>()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun deleteSavedRecipe(recipeId: String) {
        savedRecipesCollection.document(recipeId).delete().await()
    }
}
