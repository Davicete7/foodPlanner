package com.example.foodplanner.ui.pantry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.viewmodel.PantryViewModel
import com.example.foodplanner.data.repo.PantryRepository.InventoryRow
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon




@Composable
fun InventoryScreen(vm: PantryViewModel = viewModel()) {
    val inv by vm.inventory.observeAsState(emptyList())
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var ingredientToEdit by remember { mutableStateOf<InventoryRow?>(null) }

    Column {
        OutlinedTextField(name, { name = it }, label = { Text("Ingrediente") })
        OutlinedTextField(qty, { qty = it }, label = { Text("Cantidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        Button(onClick = {
            val q = qty.toDoubleOrNull() ?: 0.0
            if (name.isNotBlank()) vm.addOrUpdateInventory(name.trim(), q, "pcs")
            name = ""; qty = ""
        }) { Text("Guardar") }

        HorizontalDivider()
        /*LazyColumn {
            items(inv) { row ->
                ListItem(
                    headlineContent = { Text(row.name) },
                    supportingContent = { Text("${row.quantity} ${row.unit}") }
                )
                HorizontalDivider()
            }
        }*/
        LazyColumn {
            items(inv) { row ->
                InventoryRow(
                    item = row,
                    onEdit = { editedItem ->
                        ingredientToEdit = editedItem },
                    onDelete = { deletedItem ->
                        vm.deleteInventoryItem(deletedItem.id) }
                )
                HorizontalDivider()
            }
        }

        //If there's any ingredient slected shows the edit dialog
        ingredientToEdit?.let { ingredient ->
            EditIngredientDialog(
                ingredient = ingredient,
                onDismiss = { ingredientToEdit = null },
                onSave = { updatedItem ->
                    vm.updateInventoryItem(updatedItem.id,
                        updatedItem.name,
                        updatedItem.quantity,
                        updatedItem.unit)
                    ingredientToEdit = null
                }
            )
        }
    }
}

//Show a row with an ingridient an its options
@Composable
fun InventoryRow(
    item: InventoryRow,
    onEdit: (InventoryRow) -> Unit,
    onDelete: (InventoryRow) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            Text("${item.quantity} ${item.unit}", style = MaterialTheme.typography.bodyMedium)
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Opciones")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Editar") },
                    onClick = {
                        expanded = false
                        onEdit(item)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Eliminar") },
                    onClick = {
                        expanded = false
                        onDelete(item)
                    }
                )
            }
        }
    }
}



//Dialog to modify the ingridient
@Composable
fun EditIngredientDialog(
    ingredient: InventoryRow,
    onDismiss: () -> Unit,
    onSave: (InventoryRow) -> Unit
) {
    var name by remember { mutableStateOf(ingredient.name) }
    var qty by remember { mutableStateOf(ingredient.quantity.toString()) }
    var unit by remember { mutableStateOf(ingredient.unit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar ingrediente") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") }
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Cantidad") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unidad") }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val quantityDouble = qty.toDoubleOrNull() ?: ingredient.quantity
                onSave(
                    ingredient.copy(
                        name = name.trim(),
                        quantity = quantityDouble,
                        unit = unit.trim()
                    )
                )
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}



