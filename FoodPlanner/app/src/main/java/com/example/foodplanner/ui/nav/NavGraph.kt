package com.example.foodplanner.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Face
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.foodplanner.ui.auth.AuthScreen
import com.example.foodplanner.ui.auth.AuthState
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.ui.cart.CartScreen
import com.example.foodplanner.ui.chat.ChatListScreen
import com.example.foodplanner.ui.chat.ChatScreen
import com.example.foodplanner.ui.pantry.InventoryScreen
import com.example.foodplanner.ui.recipes.RecipeListScreen
import com.example.foodplanner.ui.settings.SettingsScreen
import com.example.foodplanner.ui.stats.StatsScreen

/**
 * Root navigation component that handles the high-level flow between Authentication and the Main App.
 */
@Composable
fun AppNav() {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    val navController = rememberNavController()

    // Redirect user based on authentication status
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

/**
 * The main application shell containing the Bottom Navigation Bar and the nested navigation host
 * for the core features (Inventory, Recipes, Cart, Chat).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(navController = navController, startDestination = Routes.Inventory) {

                // Inventory Tab
                composable(Routes.Inventory) {
                    InventoryScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }

                // Recipes Tab
                composable(Routes.Recipes) { RecipeListScreen(navController = navController) }

                // Cart Tab
                composable(Routes.Cart) {
                    CartScreen(
                        navController = navController,
                        authViewModel = authViewModel
                    )
                }

                // Chat/Chef Tab
                composable(Routes.ChatList) {
                    ChatListScreen(navController = navController,
                        authViewModel = authViewModel,
                        onChatClick = { chatId ->
                            navController.navigate(Routes.chat(chatId))
                        })
                }

                // Navigation route for specific chat conversations
                composable(
                    route = "chat/{chatId}",
                    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                    ChatScreen(
                        chatId = chatId,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Analytics/Stats Screen:
                // This is a secondary screen accessed from Inventory.
                composable("stats") {
                    StatsScreen(
                        authViewModel = authViewModel,
                        navController = navController, // Pass NavController
                        onBack = { navController.popBackStack() }
                    )
                }
                // Settings Screen
                composable(Routes.Settings) {
                    SettingsScreen(
                        authViewModel = authViewModel
                    )
                }
            }
        }
    }
}

/**
 * Renders the bottom navigation items and handles navigation logic.
 *
 * NOTE: I have configured the navigation logic to strictly reset the destination state.
 * 'saveState' and 'restoreState' are omitted to ensure that clicking a tab (e.g., Inventory)
 * always returns the user to the root of that tab, closing any temporary screens like Stats.
 */
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    NavigationBar {
        val items = listOf(
            Triple(Routes.Inventory, "Inventory", Icons.AutoMirrored.Filled.List),
            Triple(Routes.Recipes, "Recipes", Icons.AutoMirrored.Filled.List),
            Triple(Routes.Cart, "Cart", Icons.AutoMirrored.Filled.List),
            Triple(Routes.ChatList, "Chef", Icons.Default.Face)
        )
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    navController.navigate(route) {
                        // Clear the back stack up to the start destination to prevent stack accumulation
                        popUpTo(navController.graph.findStartDestination().id) {
                            // saveState = true // Disabled: We want to clear nested screens (like Stats) on tab switch
                        }
                        // Avoid multiple copies of the same destination
                        launchSingleTop = true
                        // restoreState = true // Disabled: Forces a fresh load of the tab, fixing "stuck on Stats" issue
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label) }
            )
        }
    }
}