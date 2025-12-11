package com.example.foodplanner.viewmodel

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

enum class SortOrder {
    NAME,
    EXPIRATION
}

class PantryViewModel(app: Application, private val userId: String) : AndroidViewModel(app) {
    private val repo = PantryRepository(userId)
    private val recipeRepo = RecipeRepository(app)

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val recipes = MutableStateFlow<List<RecipeDTO>>(emptyList())

    // Variables for searching and sorting in the inventory
    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.EXPIRATION)
    val sortOrder = _sortOrder.asStateFlow()

    // Combines the original list of ingridients with
    // the search term and sort order
    val visibleInventory: StateFlow<List<InventoryItem>> = combine(
        _inventory,
        _searchText,
        _sortOrder
    ) { list, text, order ->
        // Filter
        val filteredList = if (text.isBlank()) {
            list
        } else {
            list.filter { item ->
                item.name.contains(text, ignoreCase = true)
            }
        }

        // Sort
        when (order) {
            SortOrder.NAME -> filteredList.sortedBy { it.name }
            SortOrder.EXPIRATION -> filteredList.sortedBy {
                // If there is not an expiration date, we put it at the end
                it.expirationDate ?: Long.MAX_VALUE
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    init {
        // Cargar inventario con manejo de errores
        repo.inventory
            .onEach { _inventory.value = it }
            .catch { e -> Log.e("PantryViewModel", "Error cargando inventario: ${e.message}") }
            .launchIn(viewModelScope)

        // Cargar carrito con manejo de errores
        repo.cart
            .onEach { _cart.value = it }
            .catch { e -> Log.e("PantryViewModel", "Error cargando carrito: ${e.message}") }
            .launchIn(viewModelScope)

        // Cargar recetas locales
        viewModelScope.launch {
            try {
                recipes.value = recipeRepo.loadAll()
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error cargando recetas: ${e.message}")
            }
        }
    }

    fun addOrUpdateInventory(name: String, qty: Double, unit: String, expirationDate: Long?) =
        viewModelScope.launch {
            try {
                repo.addOrUpdateInventory(name, qty, unit, expirationDate)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error al guardar item: ${e.message}")
            }
        }

    fun addRecipeMissingToCart(recipe: RecipeDTO) =
        viewModelScope.launch {
            try {
                repo.addMissingToCart(recipe.ingredients.map { Triple(it.name, it.quantity, it.unit) })
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error agregando receta al carrito: ${e.message}")
            }
        }

    fun clearCart() = viewModelScope.launch {
        try {
            repo.clearCart()
        } catch (e: Exception) {
            Log.e("PantryViewModel", "Error limpiando carrito: ${e.message}")
        }
    }

    fun updateInventoryItem(id: String, newName: String, newQty: Double, newUnit: String, newExpirationDate: Long?) =
        viewModelScope.launch {
            try {
                repo.updateInventoryItem(id, newName, newQty, newUnit, newExpirationDate)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error actualizando item: ${e.message}")
            }
        }

    fun deleteInventoryItem(id: String) =
        viewModelScope.launch {
            try {
                repo.deleteInventoryItem(id)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error borrando item: ${e.message}")
            }
        }

    fun updateCartItem(id: String, newName: String, newQty: Double, newUnit: String) =
        viewModelScope.launch {
            try {
                repo.updateCartItem(id, newName, newQty, newUnit)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error actualizando carrito: ${e.message}")
            }
        }

    fun deleteCartItem(id: String) =
        viewModelScope.launch {
            try {
                repo.deleteCartItem(id)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error borrando del carrito: ${e.message}")
            }
        }

    // Change the search term
    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    // Change the the term of sorting
    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
    }

    fun addToCartManual(name: String, qty: Double, unit: String) = viewModelScope.launch {
        try {
            if (name.isNotBlank() && qty > 0) {
                repo.addOrUpdateCartItem(name, qty, unit)
            }
        } catch (e: Exception) {
            Log.e("PantryViewModel", "Error añadiendo manual al carro: ${e.message}")
        }
    }

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