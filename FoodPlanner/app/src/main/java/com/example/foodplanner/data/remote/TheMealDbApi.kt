package com.example.foodplanner.data.remote

import com.example.foodplanner.data.recipes.MealResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TheMealDbApi {
    // Buscar recetas por nombre
    @GET("search.php")
    suspend fun searchMeals(@Query("s") query: String): MealResponse

    // Obtener detalles de una receta por ID
    @GET("lookup.php")
    suspend fun getMealById(@Query("i") id: String): MealResponse
}
