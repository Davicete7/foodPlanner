package com.example.foodplanner.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingBar() {
    val text = when (LocalTime.now().hour) {
        in 5..11 -> listOf("Good morning! What's the plan for today?", "Rise and shine! Let's get cooking!", "Good morning! Ready to plan your meals?").random()
        in 12..18 -> listOf("Good afternoon! Let's keep up the productivity!", "Good afternoon! What's for dinner?", "Afternoon! Time to think about your next meal.").random()
        else -> listOf("Good evening! Something quick for tomorrow?", "Good night! Planning ahead for a great day?", "Evening! What deliciousness are we planning?").random()
    }
    TopAppBar(title = { Text(text) })
}
