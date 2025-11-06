package com.example.foodplanner.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.foodplanner.data.db.AppDatabase
import com.example.foodplanner.data.recipes.RecipeDTO
import com.example.foodplanner.data.recipes.RecipeRepository
import com.example.foodplanner.data.repo.PantryRepository
import kotlinx.coroutines.launch

class PantryViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = PantryRepository(AppDatabase.get(app))
    private val recipeRepo = RecipeRepository(app)

    val inventory = repo.inventory.asLiveData()
    val cart = repo.cart.asLiveData()
    val recipes = MutableLiveData<List<RecipeDTO>>(emptyList())

    init {
        viewModelScope.launch { recipes.postValue(recipeRepo.loadAll()) }
    }

    fun addOrUpdateInventory(name: String, qty: Double, unit: String) =
        viewModelScope.launch { repo.addOrUpdateInventory(name, qty, unit) }

    fun addRecipeMissingToCart(recipe: RecipeDTO) =
        viewModelScope.launch {
            repo.addMissingToCart(recipe.ingredients.map { Triple(it.name, it.quantity, it.unit) })
        }

    fun clearCart() = viewModelScope.launch { repo.clearCart() }

    fun updateInventoryItem(id: Long, newName: String, newQty: Double, newUnit: String) =
        viewModelScope.launch {
            repo.updateInventoryItem(id, newName, newQty, newUnit)
        }

    fun deleteInventoryItem(id: Long) =
        viewModelScope.launch {
            repo.deleteInventoryItem(id)
        }

    fun updateCartItem(id: Long, newName: String, newQty: Double, newUnit: String) =
        viewModelScope.launch {
            repo.updateCartItem(id, newName, newQty, newUnit)
        }

    fun deleteCartItem(id: Long) =
        viewModelScope.launch {
            repo.deleteCartItem(id)
        }

}
