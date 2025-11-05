package com.example.foodplanner.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodplanner.ui.activities.ActivityDetailScreen
import com.example.foodplanner.ui.activities.ActivityEditScreen
import com.example.foodplanner.ui.activities.ActivityListScreen
import com.example.foodplanner.ui.cart.CartScreen
import com.example.foodplanner.ui.components.GreetingBar
import com.example.foodplanner.ui.pantry.InventoryScreen
import com.example.foodplanner.ui.recipes.RecipeListScreen

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()

    Scaffold(
        topBar = { GreetingBar() },
        bottomBar = {
            NavigationBar {
                listOf(
                    Routes.Activities to "Actividades",
                    Routes.Inventory to "Inventario",
                    Routes.Recipes to "Recetas",
                    Routes.Cart to "Carrito",
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        selected = backStack?.destination?.route == route,
                        onClick = { nav.navigate(route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.List, contentDescription = null) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Routes.Activities, modifier = Modifier.padding(padding)) {
            composable(Routes.Activities) { ActivityListScreen(nav) }
            composable(Routes.ActivityEdit) { ActivityEditScreen(nav, null) }
            composable(Routes.ActivityDetail) { back ->
                val id = back.arguments?.getString("id")!!.toLong()
                ActivityDetailScreen(nav, id)
            }
            composable(Routes.Inventory) { InventoryScreen() }
            composable(Routes.Recipes) { RecipeListScreen() }
            composable(Routes.Cart) { CartScreen() }
        }
    }
}
