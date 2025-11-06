package com.example.foodplanner.data.db.dao

import androidx.room.*
import com.example.foodplanner.data.db.entities.Ingredient
import com.example.foodplanner.data.db.entities.InventoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("""SELECT inventory.* FROM inventory ORDER BY updatedAt DESC""")
    fun observeAll(): Flow<List<InventoryItem>>

    @Query("""SELECT inventory.* FROM inventory WHERE ingredientId = :ingredientId LIMIT 1""")
    suspend fun findByIngredientId(ingredientId: Long): InventoryItem?

    @Query("SELECT * FROM inventory WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): InventoryItem?


    @Insert suspend fun insert(item: InventoryItem): Long
    @Update suspend fun update(item: InventoryItem)
    @Query("DELETE FROM inventory WHERE id = :id") suspend fun deleteById(id: Long)

    //Get all the expired Items
    @Query("SELECT * FROM inventory WHERE expirationDate < :currentTime")
    suspend fun getExpiredItems(currentTime: Long = System.currentTimeMillis()): List<InventoryItem>

    //Order by the closest expiration date
    @Query("SELECT * FROM inventory ORDER BY expirationDate ASC")
    fun observeAllByExpiration(): Flow<List<InventoryItem>>

}

data class InventoryWithIngredient(
    @Embedded val item: InventoryItem,
    @Relation(parentColumn = "ingredientId", entityColumn = "id")
    val ingredient: Ingredient
)