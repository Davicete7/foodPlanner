package com.example.foodplanner.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodplanner.ui.auth.AuthScreen
import com.example.foodplanner.ui.auth.AuthState
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.ui.cart.CartScreen
import com.example.foodplanner.ui.chat.ChatScreen
import com.example.foodplanner.ui.components.GreetingBar
import com.example.foodplanner.ui.pantry.InventoryScreen
import com.example.foodplanner.ui.recipes.RecipeListScreen

@Composable
fun AppNav() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate("main") { popUpTo("loading") { inclusive = true } }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate(Routes.Auth) { popUpTo("loading") { inclusive = true } }
            }
            else -> Unit
        }
    }

    NavHost(navController = navController, startDestination = "loading") {
        composable("loading") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable(Routes.Auth) {
            AuthScreen(authViewModel = authViewModel)
        }
        composable("main") {
            MainScreen(authViewModel = authViewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    Scaffold(
        // Pasamos el authViewModel al GreetingBar modificado
        topBar = { GreetingBar(authViewModel) },
        bottomBar = { BottomNavigationBar(navController) }
        // Se ha eliminado el floatingActionButton
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(navController = navController, startDestination = Routes.Inventory) {
                composable(Routes.Inventory) { InventoryScreen() }
                composable(Routes.Recipes) { RecipeListScreen() }
                composable(Routes.Cart) { CartScreen() }
                composable(Routes.Chat) { ChatScreen() } // Nueva ruta
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        val items = listOf(
            Triple(Routes.Inventory, "Inventory", Icons.AutoMirrored.Filled.List),
            Triple(Routes.Recipes, "Recipes", Icons.AutoMirrored.Filled.List),
            Triple(Routes.Cart, "Cart", Icons.AutoMirrored.Filled.List),
            Triple(Routes.Chat, "AI Chef", Icons.Default.Face) // Nueva pestaña
        )
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label) }
            )
        }
    }
}