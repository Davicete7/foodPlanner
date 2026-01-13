package com.example.foodplanner.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.foodplanner.data.repo.PantryRepository
import com.example.foodplanner.utils.NotificationManager
import kotlinx.coroutines.flow.first

class PantryWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    private val notificationManager = NotificationManager(appContext)

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()
        val pantryRepository = PantryRepository(userId)

        return try {
            val inventory = pantryRepository.inventory.first()

            // Check for items nearing expiration
            val expiringSoon = inventory.filter { it.isExpiringSoon() }
            expiringSoon.forEach { 
                notificationManager.sendNotification("Expiration Alert", "Attention! Your ${it.name} expires in 3 days.")
            }

            // Check for low stock items
            val lowStock = inventory.filter { it.isLowStock() }
            lowStock.forEach { 
                notificationManager.sendNotification("Low Stock", "You are running low on ${it.name}.")
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
