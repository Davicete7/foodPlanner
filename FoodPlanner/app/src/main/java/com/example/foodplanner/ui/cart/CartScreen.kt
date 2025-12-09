package com.example.foodplanner.ui.cart

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.data.db.entities.CartItem
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.ui.components.UnitSelector
import com.example.foodplanner.ui.components.availableUnits
import com.example.foodplanner.viewmodel.PantryViewModel

@Composable
fun CartScreen(authViewModel: AuthViewModel = viewModel()) {
    val userState by authViewModel.user.collectAsState()
    val context = LocalContext.current

    // Esperar a que el usuario esté cargado para inicializar el PantryViewModel
    if (userState == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        userState?.let { user ->
            // Creamos la factory con el UID del usuario actual
            val factory = PantryViewModel.Factory(context.applicationContext as Application, user.uid)
            val vm: PantryViewModel = viewModel(factory = factory)

            var ingredientToEdit by remember { mutableStateOf<CartItem?>(null) }
            val cart by vm.cart.collectAsState()

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { vm.clearCart() }) { Text("Clear Cart") }
                }

                LazyColumn {
                    items(cart) { row ->
                        CartRowItem(
                            item = row,
                            onEdit = { ingredientToEdit = it },
                            onDelete = { vm.deleteCartItem(it.id!!) }
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
                            updatedItem.id?.let { id ->
                                vm.updateCartItem(
                                    id,
                                    updatedItem.name,
                                    updatedItem.quantity,
                                    updatedItem.unit
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
fun CartRowItem(
    item: CartItem,
    onEdit: (CartItem) -> Unit,
    onDelete: (CartItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
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
    ingredient: CartItem,
    availableUnits: List<String>,
    onDismiss: () -> Unit,
    onSave: (CartItem) -> Unit
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