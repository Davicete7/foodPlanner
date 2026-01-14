package com.example.foodplanner.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.data.repo.PantryRepository
import com.example.foodplanner.data.recipes.RecipeDTO
import com.example.foodplanner.data.recipes.RecipeRepository
import kotlinx.coroutines.launch

class RecipeViewModel(
    private val recipeRepository: RecipeRepository,
    private val pantryRepository: PantryRepository
) : ViewModel() {

    // ... estado de la UI ...

    fun search(query: String) {
        viewModelScope.launch {
            val results = recipeRepository.searchRecipes(query)
            // Actualizar estado UI con results
        }
    }

    fun saveRecipe(recipe: RecipeDTO) {
        viewModelScope.launch {
            recipeRepository.saveRecipe(recipe)
        }
    }

    fun addRecipeIngredientsToCart(recipe: RecipeDTO) {
        viewModelScope.launch {
            // Convertimos los ingredientes del DTO al formato que espera PantryRepository
            // PantryRepository.addMissingToCart espera: List<Triple<String, Double, String>>
            
            val ingredientsNeeded = recipe.ingredients.map { ingredient ->
                Triple(
                    ingredient.name,
                    ingredient.quantity,
                    ingredient.unit
                )
            }

            // Llamamos a la función que ya existe y maneja la lógica de "lo que falta"
            val addedItems = pantryRepository.addMissingToCart(ingredientsNeeded)
            
            if (addedItems.isNotEmpty()) {
                // Notificar al usuario (Toast o Snackbar) que se añadieron items
                println("Se añadieron al carrito: $addedItems")
            } else {
                println("No fue necesario añadir nada, ya tienes todo en el inventario.")
            }
        }
    }
}
