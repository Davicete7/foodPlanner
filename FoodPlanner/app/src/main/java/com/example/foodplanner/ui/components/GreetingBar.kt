package com.example.foodplanner.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingBar() {
    val text = when (LocalTime.now().hour) {
        in 5..11 -> "¡Buenos días! ¿Plan de hoy?"
        in 12..18 -> "¡Buenas tardes! ¿Seguimos productivos?"
        else -> "¡Buenas noches! ¿Algo rápido para mañana?"
    }
    TopAppBar(title = { Text(text) })
}
