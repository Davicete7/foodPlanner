package com.example.foodplanner.ui.cart

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.data.db.entities.CartItem
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.ui.components.UnitSelector
import com.example.foodplanner.ui.components.availableUnits
import com.example.foodplanner.ui.pantry.DateSelector
import com.example.foodplanner.viewmodel.PantryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(authViewModel: AuthViewModel = viewModel()) {
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

    val cart by vm.cart.collectAsState()

    var ingredientToEdit by remember { mutableStateOf<CartItem?>(null) }
    var itemToBuy by remember { mutableStateOf<CartItem?>(null) }

    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }

    Scaffold(
        // Eliminada la TopAppBar para que no haya espacio blanco doble con la GreetingBar
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- ADD ITEM FORM ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Add item manually", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Product") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = qty,
                                onValueChange = { qty = it },
                                label = { Text("Qty") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(modifier = Modifier.weight(1f)) {
                                UnitSelector(
                                    selectedUnit = unit,
                                    availableUnits = availableUnits,
                                    onUnitSelected = { unit = it }
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val quantityDouble = qty.toDoubleOrNull() ?: 0.0
                                if (name.isNotBlank() && quantityDouble > 0.0) {
                                    vm.addToCartManual(name, quantityDouble, unit)
                                    name = ""
                                    qty = ""
                                    unit = "pcs"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = name.isNotBlank() && qty.isNotBlank()
                        ) {
                            Text("Add to cart")
                        }
                    }
                }
            }

            // --- LIST HEADER & CLEAR BUTTON ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Items", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${cart.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Botón CLEAR aquí, mucho más limpio y contextual
                    if (cart.isNotEmpty()) {
                        TextButton(
                            onClick = { vm.clearCart() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Clear All")
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            }

            // --- CART ITEMS LIST ---
            if (cart.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Your cart is empty.\nAdd items manually or from recipes.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(cart, key = { it.id ?: it.hashCode() }) { row ->
                    CartItemCard(
                        item = row,
                        onEdit = { ingredientToEdit = it },
                        onDelete = { vm.deleteCartItem(it.id!!) },
                        onBuy = { itemToBuy = it }
                    )
                }
            }
        }
    }

    // --- DIALOGS (Sin cambios) ---
    itemToBuy?.let { item ->
        BuyItemDialog(
            item = item,
            onDismiss = { itemToBuy = null },
            onConfirm = { date ->
                vm.purchaseItem(item, date)
                itemToBuy = null
            }
        )
    }

    ingredientToEdit?.let { ingredient ->
        EditCartDialog(
            ingredient = ingredient,
            availableUnits = availableUnits,
            onDismiss = { ingredientToEdit = null },
            onSave = { updatedItem ->
                updatedItem.id?.let { id ->
                    vm.updateCartItem(id, updatedItem.name, updatedItem.quantity, updatedItem.unit)
                }
                ingredientToEdit = null
            }
        )
    }
}

// Las funciones auxiliares (CartItemCard, EditCartDialog, BuyItemDialog) se mantienen igual que en tu código original.
// Solo asegúrate de copiar también esas funciones al final del archivo si no están importadas de otro sitio.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CartItemCard(
    item: CartItem,
    onEdit: (CartItem) -> Unit,
    onDelete: (CartItem) -> Unit,
    onBuy: (CartItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onBuy(item) },
        shape = MaterialTheme.shapes.medium, // Unificado con el estilo general
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onBuy(item) }) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Buy",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { onEdit(item) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { onDelete(item) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

// ... (Resto de diálogos: EditCartDialog y BuyItemDialog igual que en tu código)
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
        title = { Text("Edit item") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
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

@Composable
fun BuyItemDialog(
    item: CartItem,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit
) {
    var expirationDate by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product purchased") },
        text = {
            Column {
                Text("Adding '${item.name}' to inventory.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select expiration date (optional):", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                DateSelector(
                    selectedDate = expirationDate,
                    onDateSelected = { expirationDate = it }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(expirationDate) }) {
                Text("Add to inventory")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}