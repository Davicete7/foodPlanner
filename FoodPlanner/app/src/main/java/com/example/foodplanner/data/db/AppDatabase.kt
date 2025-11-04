package com.example.foodplanner.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.foodplanner.data.db.dao.*
import com.example.foodplanner.data.db.entities.*

@Database(
    entities = [ActivityEntry::class, Ingredient::class, InventoryItem::class, CartItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityDao(): ActivityEntryDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "foodplanner.db"
                ).build().also { INSTANCE = it }
            }
    }
}
