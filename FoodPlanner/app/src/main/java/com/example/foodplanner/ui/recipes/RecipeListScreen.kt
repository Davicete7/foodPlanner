package com.example.foodplanner.ui.recipes

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.viewmodel.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(authViewModel: AuthViewModel = viewModel()) {
    val userState by authViewModel.user.collectAsState()
    val context = LocalContext.current

    if (userState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        userState?.let { user ->
            // Correct ViewModel initialization with the Factory and UID
            val factory = PantryViewModel.Factory(context.applicationContext as Application, user.uid)
            val vm: PantryViewModel = viewModel(factory = factory)

            val recipes by vm.recipes.collectAsState()

            LazyColumn {
                items(recipes) { r ->
                    Card(modifier = Modifier.padding(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(r.name, style = MaterialTheme.typography.titleMedium)

                            // Display ingredients in a readable format
                            val ingredientsText = r.ingredients.joinToString(separator = "\n") {
                                "- ${it.name}: ${it.quantity} ${it.unit}"
                            }
                            Text(text = ingredientsText, style = MaterialTheme.typography.bodyMedium)

                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { vm.addRecipeMissingToCart(r) }) {
                                Text("Add missing to cart")
                            }
                        }
                    }
                }
            }
        }
    }
}