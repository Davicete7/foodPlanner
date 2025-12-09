package com.example.foodplanner.ui.recipes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.viewmodel.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(vm: PantryViewModel = viewModel()) {
    // CORRECCIÓN: Usar collectAsState() en lugar de observeAsState()
    val recipes by vm.recipes.collectAsState()

    LazyColumn {
        items(recipes) { r ->
            // Se recomienda usar el parámetro nombrado para el modifier en Card para mayor claridad
            Card(modifier = Modifier.padding(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(r.name, style = MaterialTheme.typography.titleMedium)
                    Text(r.ingredients.joinToString { "${it.name} ${it.quantity}${it.unit}" })
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { vm.addRecipeMissingToCart(r) }) {
                        Text("Add missing to cart")
                    }
                }
            }
        }
    }
}