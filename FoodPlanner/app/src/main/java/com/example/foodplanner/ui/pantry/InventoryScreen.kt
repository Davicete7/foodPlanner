package com.example.foodplanner.ui.pantry

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape





val availableUnits = listOf("kg", "g", "L", "mL", "pcs")

@Composable
fun InventoryScreen(vm: PantryViewModel = viewModel()) {
    val inv by vm.inventory.observeAsState(emptyList())
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }

    var unit by remember { mutableStateOf("pcs") }

    var ingredientToEdit by remember { mutableStateOf<InventoryRow?>(null) }

    Column {
        OutlinedTextField(name, { name = it }, label = { Text("Ingredient") })
        OutlinedTextField(qty, { qty = it }, label = { Text("Quantity") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Unidad",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        UnitSelector(
            selectedUnit = unit,
            availableUnits = availableUnits,
            onUnitSelected = { unit = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val q = qty.toDoubleOrNull() ?: 0.0
            if (name.isNotBlank()) vm.addOrUpdateInventory(name.trim(), q, unit)
            name = ""; qty = ""
        }) { Text("Save") }

        HorizontalDivider()

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

        // If an ingredient is selected, show the edit dialog
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

@Composable
fun UnitSelector(
    selectedUnit: String,
    availableUnits: List<String>,
    onUnitSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = selectedUnit)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableUnits.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit) },
                    onClick = {
                        onUnitSelected(unit)
                        expanded = false
                    }
                )
            }
        }
    }
}




// Show a row with an ingredient and its options
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
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        expanded = false
                        onEdit(item)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        expanded = false
                        onDelete(item)
                    }
                )
            }
        }
    }
}

// Dialog to modify the ingredient
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
        title = { Text("Edit ingredient") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Unit",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                UnitSelector(
                    selectedUnit = unit,
                    availableUnits = availableUnits,
                    onUnitSelected = { unit = it }
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
            }) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
