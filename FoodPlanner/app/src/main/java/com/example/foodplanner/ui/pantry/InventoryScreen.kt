package com.example.foodplanner.ui.pantry

import android.app.Application
import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodplanner.data.db.entities.InventoryItem
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.ui.components.GreetingBar
import com.example.foodplanner.ui.components.UnitSelector
import com.example.foodplanner.ui.components.availableUnits
import com.example.foodplanner.viewmodel.PantryViewModel
import com.example.foodplanner.viewmodel.SortOrder
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
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

    // UI visibility states
    var isFormVisible by remember { mutableStateOf(false) }
    var isSearchVisible by remember { mutableStateOf(false) }

    // Input fields state
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var expirationDate by remember { mutableStateOf<Long?>(null) }

    var ingredientToEdit by remember { mutableStateOf<InventoryItem?>(null) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Custom Header with Profile and Navigation to Stats
            GreetingBar(
                authViewModel = authViewModel,
                onStatsClick = { navController.navigate("stats") }
            )

            // Main Content List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {

                // --- ACTION BUTTONS ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Toggle Add Item Form
                        Button(
                            onClick = { isFormVisible = !isFormVisible },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = if (isFormVisible) {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                ButtonDefaults.buttonColors()
                            }
                        ) {
                            Icon(
                                imageVector = if (isFormVisible) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isFormVisible) "Close" else "Add Item")
                        }

                        // Toggle Search Bar
                        OutlinedButton(
                            onClick = { isSearchVisible = !isSearchVisible },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = if (isSearchVisible) {
                                ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Search")
                        }
                    }
                }

                // --- ADD INGREDIENT FORM ---
                item {
                    AnimatedVisibility(
                        visible = isFormVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("New Ingredient", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = qty,
                                        onValueChange = { qty = it },
                                        label = { Text("Qty") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                                        UnitSelector(
                                            selectedUnit = unit,
                                            availableUnits = availableUnits,
                                            onUnitSelected = { unit = it }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                Text("Expiration date", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(4.dp))
                                DateSelector(
                                    selectedDate = expirationDate,
                                    onDateSelected = { expirationDate = it }
                                )

                                Spacer(Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        val q = qty.toDoubleOrNull() ?: 0.0
                                        if (name.isNotBlank()) {
                                            vm.addOrUpdateInventory(name.trim(), q, unit, expirationDate)
                                            // Reset fields
                                            name = ""
                                            qty = ""
                                            unit = "pcs"
                                            expirationDate = null
                                            isFormVisible = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = name.isNotBlank()
                                ) {
                                    Text("Save to Inventory")
                                }
                            }
                        }
                    }
                }

                // --- SEARCH AND FILTERS ---
                item {
                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { vm.onSearchTextChange(it) },
                                    placeholder = { Text("Filter ingredients...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = currentSort == SortOrder.EXPIRATION,
                                        onClick = { vm.onSortOrderChange(SortOrder.EXPIRATION) },
                                        label = { Text("By Expiration") },
                                        leadingIcon = {
                                            if (currentSort == SortOrder.EXPIRATION) Icon(Icons.Default.Check, null)
                                        }
                                    )
                                    FilterChip(
                                        selected = currentSort == SortOrder.NAME,
                                        onClick = { vm.onSortOrderChange(SortOrder.NAME) },
                                        label = { Text("By Name") },
                                        leadingIcon = {
                                            if (currentSort == SortOrder.NAME) Icon(Icons.Default.Check, null)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // --- INVENTORY LIST ---
                if (inv.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Your pantry is empty.\nUse the 'Add Item' button to start.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
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

    // Edit Item Dialog
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

// --- HELPER COMPOSABLES ---

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