package com.example.foodplanner.ui.cart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.viewmodel.PantryViewModel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foodplanner.data.repo.PantryRepository.CartRow
import com.example.foodplanner.ui.pantry.UnitSelector
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.text.input.KeyboardType



val availableUnits = listOf("kg", "g", "L", "mL", "pcs")


@Composable
fun CartScreen(vm: PantryViewModel = viewModel()) {
    var ingredientToEdit by remember { mutableStateOf<CartRow?>(null) }
    val cart by vm.cart.observeAsState(emptyList())


    Column {
        TextButton(onClick = { vm.clearCart() }) { Text("Clear") }
        LazyColumn {
            items(cart) { row ->
                CartRowItem(
                    item = row,
                    onEdit = { ingredientToEdit = it },
                    onDelete = { vm.deleteCartItem(it.id) }
                )
                HorizontalDivider()
            }
        }

        ingredientToEdit?.let { ingredient ->
            EditCartDialog(
                ingredient = ingredient,
                availableUnits = availableUnits,
                onDismiss = { ingredientToEdit = null },
                onSave = { updatedItem ->
                    vm.updateCartItem(
                        updatedItem.id,
                        updatedItem.name,
                        updatedItem.quantity,
                        updatedItem.unit
                    )
                    ingredientToEdit = null
                }
            )
        }

    }
}

@Composable
fun CartRowItem(
    item: CartRow,
    onEdit: (CartRow) -> Unit,
    onDelete: (CartRow) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(item.name, style = MaterialTheme.typography.titleMedium)
            Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodyMedium)
        }
        Row {
            IconButton(onClick = { onEdit(item) }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
            IconButton(onClick = { onDelete(item) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}


@Composable
fun EditCartDialog(
    ingredient: CartRow,
    availableUnits: List<String>,
    onDismiss: () -> Unit,
    onSave: (CartRow) -> Unit
) {
    var name by remember { mutableStateOf(ingredient.name) }
    var qty by remember { mutableStateOf(ingredient.quantity.toString()) }
    var unit by remember { mutableStateOf(ingredient.unit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Ingredient") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
                Text("Unit", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                UnitSelector(
                    selectedUnit = unit,
                    availableUnits = availableUnits,
                    onUnitSelected = { unit = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updatedQty = qty.toDoubleOrNull() ?: ingredient.quantity
                onSave(ingredient.copy(name = name, quantity = updatedQty, unit = unit))
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
