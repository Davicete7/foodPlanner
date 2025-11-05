package com.example.foodplanner.data.repo

import com.example.foodplanner.data.db.AppDatabase
import com.example.foodplanner.data.db.entities.CartItem
import com.example.foodplanner.data.db.entities.Ingredient
import com.example.foodplanner.data.db.entities.InventoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PantryRepository(private val db: AppDatabase) {

    // Flujos básicos
    val inventoryItems = db.inventoryDao().observeAll()
    val cartItems = db.cartDao().observeAll()
    val ingredientsFlow = db.ingredientDao().observeAll()

    // Combinar para obtener nombres en UI (sencillo para Iteración 1)
    data class InventoryRow(val name: String, val unit: String, val quantity: Double, val id: Long)
    data class CartRow(val name: String, val unit: String, val quantity: Double, val id: Long)

    val inventory: Flow<List<InventoryRow>> =
        combine(inventoryItems, ingredientsFlow) { inv, ingredients ->
            val map = ingredients.associateBy { it.id }
            inv.map { item ->
                val ing = map[item.ingredientId]
                InventoryRow(ing?.name ?: "?", ing?.unit ?: "pcs", item.quantity, item.id)
            }.sortedBy { it.name.lowercase() }
        }

    val cart: Flow<List<CartRow>> =
        combine(cartItems, ingredientsFlow) { cart, ingredients ->
            val map = ingredients.associateBy { it.id }
            cart.map { item ->
                val ing = map[item.ingredientId]
                CartRow(ing?.name ?: "?", ing?.unit ?: "pcs", item.quantity, item.id)
            }.sortedBy { it.name.lowercase() }
        }

    // Helpers
    private suspend fun upsertIngredientByName(name: String, unit: String = "pcs"): Ingredient {
        db.ingredientDao().findByName(name)?.let { return it }
        val id = db.ingredientDao().insert(Ingredient(name = name, unit = unit))
        return db.ingredientDao().getById(id)!!
    }

    suspend fun addOrUpdateInventory(name: String, quantity: Double, unit: String = "pcs") {
        val ing = upsertIngredientByName(name, unit)
        val existing = db.inventoryDao().findByIngredientId(ing.id)
        if (existing == null) {
            db.inventoryDao().insert(InventoryItem(ingredientId = ing.id, quantity = quantity))
        } else {
            db.inventoryDao().update(existing.copy(quantity = quantity))
        }
    }

    suspend fun addMissingToCart(need: List<Triple<String, Double, String>>) {
        for ((name, qty, unit) in need) {
            val ing = upsertIngredientByName(name, unit)
            val have = db.inventoryDao().findByIngredientId(ing.id)?.quantity ?: 0.0
            val missing = (qty - have).coerceAtLeast(0.0)
            if (missing > 0) {
                val existing = db.cartDao().findByIngredientId(ing.id)
                if (existing == null) {
                    db.cartDao().insert(CartItem(ingredientId = ing.id, quantity = missing))
                } else {
                    db.cartDao().update(existing.copy(quantity = existing.quantity + missing))
                }
            }
        }
    }

    suspend fun clearCart() = db.cartDao().clear()

    suspend fun updateInventoryItem(id: Long, newName: String, newQty: Double, newUnit: String) {
        val item = db.inventoryDao().findById(id) ?: return
        db.inventoryDao().update(item.copy(quantity = newQty))
        val ingredient = db.ingredientDao().getById(item.ingredientId) ?: return
        db.ingredientDao().update(ingredient.copy(name = newName, unit = newUnit))
    }

    suspend fun deleteInventoryItem(id: Long) {
        val item = db.inventoryDao().findById(id) ?: return
        db.inventoryDao().deleteById(id)
    }
}
