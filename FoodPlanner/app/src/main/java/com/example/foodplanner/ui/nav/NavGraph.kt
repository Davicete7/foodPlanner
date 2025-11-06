package com.example.foodplanner.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodplanner.ui.cart.CartScreen
import com.example.foodplanner.ui.components.GreetingBar
import com.example.foodplanner.ui.pantry.InventoryScreen
import com.example.foodplanner.ui.recipes.RecipeListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()

    Scaffold(
        topBar = { GreetingBar() },
        bottomBar = {
            NavigationBar {
                listOf(
                    Routes.Inventory to "Inventory",
                    Routes.Recipes to "Recipes",
                    Routes.Cart to "Cart",
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        selected = backStack?.destination?.route?.startsWith(route) ?: false,
                        onClick = { nav.navigate(route) { launchSingleTop = true } },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.Inventory, modifier = Modifier.padding(padding)) {
            composable(Routes.Inventory) { InventoryScreen() }
            composable(Routes.Recipes) { RecipeListScreen() }
            composable(Routes.Cart) { CartScreen() }
        }
    }
}
