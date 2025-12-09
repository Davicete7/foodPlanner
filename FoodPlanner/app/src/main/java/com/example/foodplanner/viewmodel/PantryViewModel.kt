package com.example.foodplanner.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.data.db.entities.CartItem
import com.example.foodplanner.data.db.entities.InventoryItem
import com.example.foodplanner.data.recipes.RecipeDTO
import com.example.foodplanner.data.recipes.RecipeRepository
import com.example.foodplanner.data.repo.PantryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class PantryViewModel(app: Application, private val userId: String) : AndroidViewModel(app) {
    private val repo = PantryRepository(userId)
    private val recipeRepo = RecipeRepository(app)

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val recipes = MutableStateFlow<List<RecipeDTO>>(emptyList())

    init {
        repo.inventory.onEach { _inventory.value = it }.launchIn(viewModelScope)
        repo.cart.onEach { _cart.value = it }.launchIn(viewModelScope)
        viewModelScope.launch { recipes.value = recipeRepo.loadAll() }
    }

    fun addOrUpdateInventory(name: String, qty: Double, unit: String, expirationDate: Long?) =
        viewModelScope.launch { repo.addOrUpdateInventory(name, qty, unit, expirationDate) }

    fun addRecipeMissingToCart(recipe: RecipeDTO) =
        viewModelScope.launch {
            repo.addMissingToCart(recipe.ingredients.map { Triple(it.name, it.quantity, it.unit) })
        }

    fun clearCart() = viewModelScope.launch { repo.clearCart() }

    fun updateInventoryItem(id: String, newName: String, newQty: Double, newUnit: String, newExpirationDate: Long?) =
        viewModelScope.launch {
            repo.updateInventoryItem(id, newName, newQty, newUnit, newExpirationDate)
        }

    fun deleteInventoryItem(id: String) =
        viewModelScope.launch { repo.deleteInventoryItem(id) }

    fun updateCartItem(id: String, newName: String, newQty: Double, newUnit: String) =
        viewModelScope.launch { repo.updateCartItem(id, newName, newQty, newUnit) }

    fun deleteCartItem(id: String) =
        viewModelScope.launch { repo.deleteCartItem(id) }

    class Factory(private val app: Application, private val userId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PantryViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return PantryViewModel(app, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}