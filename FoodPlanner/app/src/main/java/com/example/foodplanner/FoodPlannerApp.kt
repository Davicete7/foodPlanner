package com.example.foodplanner

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodplanner.worker.PantryWorker
import java.util.concurrent.TimeUnit

class FoodPlannerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupPantryWorker()
    }

    private fun setupPantryWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<PantryWorker>(
            repeatInterval = 1, 
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
        .setConstraints(constraints)
        .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "pantry_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}
