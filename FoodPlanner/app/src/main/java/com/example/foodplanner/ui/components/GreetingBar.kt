package com.example.foodplanner.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.foodplanner.ui.auth.AuthViewModel
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingBar(authViewModel: AuthViewModel) {
    val user by authViewModel.user.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val text = when (LocalTime.now().hour) {
        in 5..11 -> listOf("Good morning! What's the plan for today?", "Rise and shine! Let's get cooking!", "Good morning! Ready to plan your meals?").random()
        in 12..18 -> listOf("Good afternoon! Let's keep up the productivity!", "Good afternoon! What's for dinner?", "Afternoon! Time to think about your next meal.").random()
        else -> listOf("Good evening! Something quick for tomorrow?", "Good night! Planning ahead for a great day?", "Evening! What deliciousness are we planning?").random()
    }

    TopAppBar(
        title = { Text(text) },
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                // Muestra el correo del usuario (si existe) como un elemento no clicable
                DropdownMenuItem(
                    text = {
                        Text(
                            text = user?.email ?: "No email",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    onClick = { },
                    enabled = false
                )

                HorizontalDivider()

                DropdownMenuItem(
                    text = { Text("Logout") },
                    onClick = {
                        showMenu = false
                        authViewModel.logout()
                    }
                )
            }
        }
    )
}