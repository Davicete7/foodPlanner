package com.example.foodplanner.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.foodplanner.data.db.entities.Ingredient
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredientDao {
    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    fun observeAll(): Flow<List<Ingredient>>

    @Query("SELECT * FROM ingredients WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Ingredient?

    @Insert
    suspend fun insert(ingredient: Ingredient): Long

    @Query("SELECT * FROM ingredients WHERE id = :id")
    suspend fun getById(id: Long): Ingredient?

    @Update
    suspend fun update(ingredient: Ingredient)
}