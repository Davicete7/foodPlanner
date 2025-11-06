package com.example.foodplanner.data.db.dao

import androidx.room.*
import com.example.foodplanner.data.db.entities.CartItem
import com.example.foodplanner.data.db.entities.Ingredient
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart")
    fun observeAll(): Flow<List<CartItem>>

    @Insert suspend fun insert(item: CartItem): Long
    @Update suspend fun update(item: CartItem)
    @Query("DELETE FROM cart WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM cart") suspend fun clear()
    @Query("SELECT * FROM cart WHERE ingredientId = :ingredientId LIMIT 1")
    suspend fun findByIngredientId(ingredientId: Long): CartItem?

    @Query("SELECT * FROM cart WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): CartItem?
}

data class CartWithIngredient(
    @Embedded val item: CartItem,
    @Relation(parentColumn = "ingredientId", entityColumn = "id")
    val ingredient: Ingredient
)
