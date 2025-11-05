package com.example.foodplanner.ui.cart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.viewmodel.PantryViewModel

@Composable
fun CartScreen(vm: PantryViewModel = viewModel()) {
    val cart by vm.cart.observeAsState(emptyList())
    Column {
        TextButton(onClick = { vm.clearCart() }) { Text("Vaciar") }
        LazyColumn {
            items(cart) { row ->
                ListItem(
                    headlineContent = { Text(row.name) },
                    supportingContent = { Text("${row.quantity} ${row.unit}") }
                )
                HorizontalDivider()
            }
        }
    }
}
