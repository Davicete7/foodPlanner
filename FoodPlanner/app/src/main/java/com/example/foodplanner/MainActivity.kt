package com.example.foodplanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.material3.MaterialTheme
import com.example.foodplanner.ui.nav.AppNav
import com.example.foodplanner.ui.theme.FoodPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            FoodPlannerTheme {
                MaterialTheme { AppNav() }
            }
        }
    }
}
