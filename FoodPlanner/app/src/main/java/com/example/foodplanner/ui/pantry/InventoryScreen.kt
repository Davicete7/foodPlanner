package com.example.foodplanner.ui.pantry

import android.app.Application
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.viewmodel.PantryViewModel
import com.example.foodplanner.data.db.entities.InventoryItem
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.shape.RoundedCornerShape
import android.app.DatePickerDialog
import androidx.compose.ui.platform.LocalContext
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.ui.components.UnitSelector
import com.example.foodplanner.ui.components.availableUnits
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.foodplanner.viewmodel.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(authViewModel: AuthViewModel = viewModel()) {
    val userState by authViewModel.user.collectAsState()
    val context = LocalContext.current

    if (userState == null) {
        CircularProgressIndicator()
    } else {
        userState?.let { user ->
            val factory = PantryViewModel.Factory(context.applicationContext as Application, user.uid)
            val vm: PantryViewModel = viewModel(factory = factory)

            val inv by vm.visibleInventory.collectAsState()
            val searchText by vm.searchText.collectAsState()
            val currentSort by vm.sortOrder.collectAsState()
            var name by remember { mutableStateOf("") }
            var qty by remember { mutableStateOf("") }
            var unit by remember { mutableStateOf("pcs") }
            var expirationDate by remember { mutableStateOf<Long?>(null) } // fecha en milisegundos


            var ingredientToEdit by remember { mutableStateOf<InventoryItem?>(null) }

            Column {
                OutlinedTextField(name, { name = it }, label = { Text("Ingredient") })
                OutlinedTextField(
                    qty, { qty = it }, label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

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

                Text(
                    text = "Expiration Date",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                DateSelector(
                    selectedDate = expirationDate,
                    onDateSelected = { expirationDate = it }
                )


                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    val q = qty.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) vm.addOrUpdateInventory(name.trim(), q, unit, expirationDate)
                    name = ""; qty = ""; unit = "pcs"; expirationDate = null;
                }) { Text("Save") }

                HorizontalDivider()

                Spacer(modifier = Modifier.height(8.dp))

                // 1. Search abr
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { vm.onSearchTextChange(it) },
                    label = { Text("Buscar ingrediente...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. Chips for sorting
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip for expiration
                    FilterChip(
                        selected = currentSort == SortOrder.EXPIRATION,
                        onClick = { vm.onSortOrderChange(SortOrder.EXPIRATION) },
                        label = { Text("Expiration") },
                        leadingIcon = {
                            if (currentSort == SortOrder.EXPIRATION) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )

                    // Chip for name
                    FilterChip(
                        selected = currentSort == SortOrder.NAME,
                        onClick = { vm.onSortOrderChange(SortOrder.NAME) },
                        label = { Text("Name") },
                        leadingIcon = {
                            if (currentSort == SortOrder.NAME) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))


                LazyColumn {
                    items(inv) { row ->
                        InventoryRow(
                            item = row,
                            onEdit = { editedItem ->
                                ingredientToEdit = editedItem
                            },
                            onDelete = { deletedItem ->
                                deletedItem.id?.let { vm.deleteInventoryItem(it) }
                            }
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
                            updatedItem.id?.let {
                                vm.updateInventoryItem(
                                    it,
                                    updatedItem.name,
                                    updatedItem.quantity,
                                    updatedItem.unit,
                                    updatedItem.expirationDate
                                )
                            }
                            ingredientToEdit = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    selectedDate: Long?,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val displayText = selectedDate?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
    } ?: "dd/MM/yyyy" // placeholder

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                RoundedCornerShape(4.dp)
            )
            .clickable {
                val picker = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        calendar.set(year, month, dayOfMonth)
                        onDateSelected(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                picker.show()
            }
            .padding(horizontal = 12.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = displayText)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }
    }
}




// Show a row with an ingredient and its options
@Composable
fun InventoryRow(
    item: InventoryItem,
    onEdit: (InventoryItem) -> Unit,
    onDelete: (InventoryItem) -> Unit
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

            val expiryText = if (item.expirationDate != null) {
                SimpleDateFormat("dd/MM/yyyy").format(Date(item.expirationDate))
            } else {
                "None"
            }

            Text("Expiration Date: $expiryText", style = MaterialTheme.typography.bodyMedium)
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
    ingredient: InventoryItem,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf(ingredient.name) }
    var qty by remember { mutableStateOf(ingredient.quantity.toString()) }
    var unit by remember { mutableStateOf(ingredient.unit) }
    var expirationDate by remember { mutableStateOf(ingredient.expirationDate) }

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

                Text(
                    text = "Expiration Date",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
                DateSelector(
                    selectedDate = expirationDate,
                    onDateSelected = { expirationDate = it }
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
                        unit = unit.trim(),
                        expirationDate = expirationDate
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
