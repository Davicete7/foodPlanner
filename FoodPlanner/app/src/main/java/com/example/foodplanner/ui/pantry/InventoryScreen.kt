package com.example.foodplanner.ui.pantry

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.viewmodel.PantryViewModel

@Composable
fun InventoryScreen(vm: PantryViewModel = viewModel()) {
    val inv by vm.inventory.observeAsState(emptyList())
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }

    Column {
        OutlinedTextField(name, { name = it }, label = { Text("Ingrediente") })
        OutlinedTextField(qty, { qty = it }, label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Button(onClick = {
            val q = qty.toDoubleOrNull() ?: 0.0
            if (name.isNotBlank()) vm.addOrUpdateInventory(name.trim(), q, "pcs")
            name = ""; qty = ""
        }) { Text("Guardar") }

        Divider()
        LazyColumn {
            items(inv) { row ->
                ListItem(
                    headlineContent = { Text(row.name) },
                    supportingContent = { Text("${row.quantity} ${row.unit}") }
                )
                Divider()
            }
        }
    }
}
