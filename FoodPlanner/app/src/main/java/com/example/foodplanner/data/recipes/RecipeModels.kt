package com.example.foodplanner.data.recipes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Respuesta de la API para listas de recetas
@Serializable
data class MealResponse(
    val meals: List<MealApiModel>? = null
)

// Modelo crudo de la API
@Serializable
data class MealApiModel(
    @SerialName("idMeal") val id: String,
    @SerialName("strMeal") val name: String,
    @SerialName("strInstructions") val instructions: String,
    @SerialName("strMealThumb") val thumbnail: String,
    // La API devuelve hasta 20 ingredientes como campos individuales
    val strIngredient1: String? = null,
    val strMeasure1: String? = null,
    val strIngredient2: String? = null,
    val strMeasure2: String? = null,
    val strIngredient3: String? = null,
    val strMeasure3: String? = null,
    val strIngredient4: String? = null,
    val strMeasure4: String? = null,
    val strIngredient5: String? = null,
    val strMeasure5: String? = null,
    // ... puedes añadir hasta el 20 si es necesario ...
    val strIngredient20: String? = null,
    val strMeasure20: String? = null
) {
    // Función auxiliar para convertir el modelo plano de la API a tu lista de ingredientes
    fun toDomainIngredients(): List<RecipeIngredientDTO> {
        val ingredients = mutableListOf<RecipeIngredientDTO>()
        
        // Helper para procesar pares
        fun addIfValid(ingredient: String?, measure: String?) {
            if (!ingredient.isNullOrBlank() && !measure.isNullOrBlank()) {
                val (qty, unit) = parseMeasure(measure)
                ingredients.add(RecipeIngredientDTO(ingredient.trim(), qty, unit))
            }
        }

        addIfValid(strIngredient1, strMeasure1)
        addIfValid(strIngredient2, strMeasure2)
        addIfValid(strIngredient3, strMeasure3)
        addIfValid(strIngredient4, strMeasure4)
        addIfValid(strIngredient5, strMeasure5)
        // ... repetir para los demás ...

        return ingredients
    }

    // Lógica simple para extraer cantidad y unidad de un string como "200g" o "1 cup"
    private fun parseMeasure(measure: String): Pair<Double, String> {
        val cleanMeasure = measure.trim()
        // Regex para buscar un número al principio
        val regex = Regex("^([\\d/\\.]+)\\s*(.*)") 
        val match = regex.find(cleanMeasure)

        return if (match != null) {
            val qtyStr = match.groupValues[1]
            val unitStr = match.groupValues[2]
            
            // Manejar fracciones simples como "1/2"
            val qty = if (qtyStr.contains("/")) {
                val parts = qtyStr.split("/")
                if (parts.size == 2) parts[0].toDoubleOrNull()?.div(parts[1].toDoubleOrNull() ?: 1.0) ?: 1.0
                else 1.0
            } else {
                qtyStr.toDoubleOrNull() ?: 1.0
            }
            
            Pair(qty, unitStr)
        } else {
            // Si no se puede parsear número, asumimos 1 unidad arbitraria
            Pair(1.0, cleanMeasure)
        }
    }
}

// Actualizamos tu DTO existente para que soporte guardar en Firebase
@Serializable
data class RecipeDTO(
    val id: String = "", // Cambiado a String porque la API usa Strings y Firestore también
    val name: String = "",
    val ingredients: List<RecipeIngredientDTO> = emptyList(),
    val instructions: String = "",
    val imageUrl: String = ""
)

@Serializable
data class RecipeIngredientDTO(
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = ""
)
