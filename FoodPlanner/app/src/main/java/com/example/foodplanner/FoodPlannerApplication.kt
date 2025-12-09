package com.example.foodplanner

import android.app.Application
import com.google.firebase.FirebaseApp

class FoodPlannerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
