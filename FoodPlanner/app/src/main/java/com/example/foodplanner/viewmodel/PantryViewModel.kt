package com.example.foodplanner.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodplanner.data.db.entities.CartItem
import com.example.foodplanner.data.db.entities.InventoryItem
import com.example.foodplanner.data.recipes.RecipeDTO
import com.example.foodplanner.data.recipes.RecipeRepository
import com.example.foodplanner.data.repo.PantryRepository
import com.example.foodplanner.worker.PantryWorker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

enum class SortOrder {
    NAME,
    EXPIRATION
}

// Sealed class for notification events
sealed class NotificationEvent {
    data class ItemAddedToCart(val itemName: String) : NotificationEvent()
}

class PantryViewModel(app: Application, private val userId: String) : AndroidViewModel(app) {
    private val repo = PantryRepository(userId)
    private val recipeRepo = RecipeRepository(app)
    private val workManager = WorkManager.getInstance(app)

    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory.asStateFlow()

    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    val recipes = MutableStateFlow<List<RecipeDTO>>(emptyList())

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.EXPIRATION)
    val sortOrder = _sortOrder.asStateFlow()

    private val _notificationEvent = MutableSharedFlow<NotificationEvent>()
    val notificationEvent = _notificationEvent.asSharedFlow()

    val visibleInventory: StateFlow<List<InventoryItem>> = combine(
        _inventory,
        _searchText,
        _sortOrder
    ) { list, text, order ->
        val filteredList = if (text.isBlank()) {
            list
        } else {
            list.filter { item ->
                item.name.contains(text, ignoreCase = true)
            }
        }

        when (order) {
            SortOrder.NAME -> filteredList.sortedBy { it.name }
            SortOrder.EXPIRATION -> filteredList.sortedBy { it.expirationDate ?: Long.MAX_VALUE }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        repo.inventory
            .onEach { _inventory.value = it }
            .catch { e -> Log.e("PantryViewModel", "Error loading inventory: ${e.message}") }
            .launchIn(viewModelScope)

        repo.cart
            .onEach { _cart.value = it }
            .catch { e -> Log.e("PantryViewModel", "Error loading cart: ${e.message}") }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            try {
                recipes.value = recipeRepo.loadAll()
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error loading recipes: ${e.message}")
            }
        }
    }

    fun schedulePantryCheck() {
        val inputData = Data.Builder().putString("userId", userId).build()
        val workRequest = PeriodicWorkRequestBuilder<PantryWorker>(60, TimeUnit.SECONDS)
            .setInputData(inputData)
            .build()
        workManager.enqueue(workRequest)
    }

    fun addOrUpdateInventory(name: String, qty: Double, unit: String, expirationDate: Long?) =
        viewModelScope.launch {
            try {
                repo.addOrUpdateInventory(name, qty, unit, expirationDate)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error saving item: ${e.message}")
            }
        }

    fun addRecipeMissingToCart(recipe: RecipeDTO) =
        viewModelScope.launch {
            try {
                val addedItems = repo.addMissingToCart(recipe.ingredients.map { Triple(it.name, it.quantity, it.unit) })
                addedItems.forEach { itemName ->
                    _notificationEvent.emit(NotificationEvent.ItemAddedToCart(itemName))
                }
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error adding recipe to cart: ${e.message}")
            }
        }

    fun clearCart() = viewModelScope.launch {
        try {
            repo.clearCart()
        } catch (e: Exception) {
            Log.e("PantryViewModel", "Error clearing cart: ${e.message}")
        }
    }

    fun updateInventoryItem(id: String, newName: String, newQty: Double, newUnit: String, newExpirationDate: Long?) =
        viewModelScope.launch {
            try {
                repo.updateInventoryItem(id, newName, newQty, newUnit, newExpirationDate)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error updating item: ${e.message}")
            }
        }

    fun deleteInventoryItem(id: String) =
        viewModelScope.launch {
            try {
                repo.deleteInventoryItem(id)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error deleting item: ${e.message}")
            }
        }

    fun updateCartItem(id: String, newName: String, newQty: Double, newUnit: String) =
        viewModelScope.launch {
            try {
                repo.updateCartItem(id, newName, newQty, newUnit)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error updating cart: ${e.message}")
            }
        }

    fun deleteCartItem(id: String) =
        viewModelScope.launch {
            try {
                repo.deleteCartItem(id)
            } catch (e: Exception) {
                Log.e("PantryViewModel", "Error deleting from cart: ${e.message}")
            }
        }

    fun onSearchTextChange(text: String) {
        _searchText.value = text
    }

    fun onSortOrderChange(order: SortOrder) {
        _sortOrder.value = order
    }

    fun addToCartManual(name: String, qty: Double, unit: String) = viewModelScope.launch {
        try {
            if (name.isNotBlank() && qty > 0) {
                repo.addOrUpdateCartItem(name, qty, unit)
            }
        } catch (e: Exception) {
            Log.e("PantryViewModel", "Error manually adding to cart: ${e.message}")
        }
    }

    fun purchaseItem(item: CartItem, expirationDate: Long?) = viewModelScope.launch {
        try {
            repo.addOrUpdateInventory(
                name = item.name,
                quantity = item.quantity,
                unit = item.unit,
                expirationDate = expirationDate
            )

            item.id?.let { repo.deleteCartItem(it) }

        } catch (e: Exception) {
            Log.e("PantryViewModel", "Error buying item: ${e.message}")
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