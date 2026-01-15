package com.example.foodplanner.ui.pantry

import android.app.Application
import android.app.DatePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.data.db.entities.InventoryItem
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.ui.components.UnitSelector
import com.example.foodplanner.ui.components.availableUnits
import com.example.foodplanner.viewmodel.PantryViewModel
import com.example.foodplanner.viewmodel.SortOrder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(authViewModel: AuthViewModel = viewModel()) {
    val userState by authViewModel.user.collectAsState()
    val context = LocalContext.current

    if (userState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = userState!!
    val factory = PantryViewModel.Factory(context.applicationContext as Application, user.uid)
    val vm: PantryViewModel = viewModel(factory = factory)

    val inv by vm.visibleInventory.collectAsState()
    val searchText by vm.searchText.collectAsState()
    val currentSort by vm.sortOrder.collectAsState()

    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var expirationDate by remember { mutableStateOf<Long?>(null) }

    var ingredientToEdit by remember { mutableStateOf<InventoryItem?>(null) }

    val screenPadding = 16.dp

    Scaffold(
        topBar = { TopAppBar(title = { Text("Inventory") }) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = screenPadding, vertical = 12.dp)
        ) {
            // --- Add / Update ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add / update item", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Ingredient") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = qty,
                        onValueChange = { qty = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Unit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    UnitSelector(
                        selectedUnit = unit,
                        availableUnits = availableUnits,
                        onUnitSelected = { unit = it }
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Expiration date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    DateSelector(
                        selectedDate = expirationDate,
                        onDateSelected = { expirationDate = it }
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val q = qty.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                vm.addOrUpdateInventory(name.trim(), q, unit, expirationDate)
                            }
                            name = ""
                            qty = ""
                            unit = "pcs"
                            expirationDate = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // --- Search + Sort ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { vm.onSearchTextChange(it) },
                        label = { Text("Search ingredient") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- List ---
            if (inv.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No items in inventory yet.\nAdd ingredients above!",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(inv, key = { it.id ?: it.hashCode() }) { row ->
                        InventoryRow(
                            item = row,
                            onEdit = { ingredientToEdit = it },
                            onDelete = { deletedItem ->
                                deletedItem.id?.let { vm.deleteInventoryItem(it) }
                            }
                        )
                    }
                }
            }
        }
    }

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

@Composable
fun DateSelector(
    selectedDate: Long?,
    onDateSelected: (Long) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val displayText = selectedDate?.let {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
    } ?: "dd/MM/yyyy"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
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
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = displayText)
            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null)
        }
    }
}

@Composable
fun InventoryRow(
    item: InventoryItem,
    onEdit: (InventoryItem) -> Unit,
    onDelete: (InventoryItem) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val expiryText = if (item.expirationDate != null) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(item.expirationDate))
    } else {
        "None"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Expiration: $expiryText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
}

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
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                UnitSelector(
                    selectedUnit = unit,
                    availableUnits = availableUnits,
                    onUnitSelected = { unit = it }
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Expiration date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
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
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}