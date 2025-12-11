package com.example.foodplanner.data.repo

import com.example.foodplanner.data.db.entities.CartItem
import com.example.foodplanner.data.db.entities.InventoryItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.example.foodplanner.utils.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PantryRepository(private val userId: String) {

    private val db = FirebaseFirestore.getInstance()
    private val userInventory = db.collection("users").document(userId).collection("inventory")
    private val userCart = db.collection("users").document(userId).collection("cart")

    val inventory: Flow<List<InventoryItem>> = userInventory.snapshots().map {
        it.toObjects(InventoryItem::class.java)
    }

    val cart: Flow<List<CartItem>> = userCart.snapshots().map {
        it.toObjects(CartItem::class.java)
    }

    /**
     * Añadir o actualizar un item de inventario por nombre (searchableName).
     * Se ha eliminado el uso incorrecto de transaction.get(query),
     * que no está soportado. Ahora se hace un get normal y luego se actualiza.
     */
    suspend fun addOrUpdateInventory(
        name: String,
        quantity: Double,
        unit: String,
        expirationDate: Long?
    ) {
        val searchableName = name.lowercase()

        // Primero buscamos si ya existe un documento con ese searchableName
        val snapshot = userInventory
            .whereEqualTo("searchableName", searchableName)
            .limit(1)
            .get()
            .await()

        if (snapshot == null || snapshot.isEmpty) {
            // Crear nuevo documento
            val newItemRef = userInventory.document()
            val newItem = InventoryItem(
                name = name,
                searchableName = searchableName,
                quantity = quantity,
                unit = unit,
                expirationDate = expirationDate
            )
            newItemRef.set(newItem).await()
        } else {
            // Actualizar documento existente (aquí puedes decidir qué campos actualizar)
            val docRef = snapshot.documents.first().reference
            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "searchableName" to searchableName,
                "quantity" to quantity,
                "unit" to unit
            )
            expirationDate?.let { updates["expirationDate"] = it }
            docRef.update(updates).await()
        }
    }



    suspend fun addMissingToCart(need: List<Triple<String, Double, String>>) {
        val currentInventory = inventory.first().associateBy { it.searchableName }
        val currentCart = cart.first().associateBy { it.searchableName }
        val batch = db.batch()

        for ((name, qtyNeeded, unitNeeded) in need) {
            val searchableName = name.lowercase()
            val inventoryItem = currentInventory[searchableName]

            // Calculate the quantity that we have to add to the cart
            // checking the ingridients that we already have in the inventory
            val quantityToAdd = if (inventoryItem != null) {
                // If we have the item, we chek if we can subtract them
                calculateMissingAmount(
                    neededQty = qtyNeeded,
                    neededUnit = unitNeeded,
                    haveQty = inventoryItem.quantity,
                    haveUnit = inventoryItem.unit
                )
            } else {
                // If not, add all the quantity
                qtyNeeded
            }

            // If the quantity needed is positive, add it to the cart
            if (quantityToAdd > 0) {
                val existingCartItem = currentCart[searchableName]

                if (existingCartItem == null) {
                    val newCartItemRef = userCart.document()
                    batch.set(
                        newCartItemRef,
                        CartItem(
                            name = name,
                            searchableName = searchableName,
                            quantity = quantityToAdd,
                            unit = unitNeeded
                        )
                    )
                } else {
                    existingCartItem.id?.let {
                        val docRef = userCart.document(it)
                        // Add the quantity to the existing item
                        val newQty = existingCartItem.quantity + quantityToAdd
                        batch.update(docRef, "quantity", newQty)
                    }
                }
            }
        }
        batch.commit().await()
    }


    private fun calculateMissingAmount(
        neededQty: Double,
        neededUnit: String,
        haveQty: Double,
        haveUnit: String
    ): Double {
        val neededType = getUnitType(neededUnit)
        val haveType = getUnitType(haveUnit)

        // If the unit types doesn't match (ex: Kg vs Liters, o Kg vs Pcs),
        // we cannot subtract them. Add the quantity of the recipe.
        if (neededType != haveType) {
            return neededQty
        }

        // If the unit types match, we can subtract them, but we have to convert them to base
        return when (neededType) {
            UnitType.MASS, UnitType.VOLUME -> {
                // Convert g or ml to base
                val neededInBase = toBase(neededQty, neededUnit)
                val haveInBase = toBase(haveQty, haveUnit)
                val missingInBase = (neededInBase - haveInBase).coerceAtLeast(0.0)
                fromBase(missingInBase, neededUnit)
            }
            UnitType.OTHER -> {
                // Fot other units we subtract the quantities directly (this is the case of pcs)
                (neededQty - haveQty).coerceAtLeast(0.0)
            }
        }
    }

    enum class UnitType { MASS, VOLUME, OTHER }

    private fun getUnitType(unit: String): UnitType {
        return when (unit.lowercase().trim()) {
            "kg", "g", "gr", "gram", "grams", "kilogram", "kilograms" -> UnitType.MASS
            "l", "ml", "liter", "liters", "milliliter", "milliliters" -> UnitType.VOLUME
            else -> UnitType.OTHER
        }
    }

    // Convert to base (g/ml)
    private fun toBase(qty: Double, unit: String): Double {
        val u = unit.lowercase().trim()
        return when {
            // Mass, base in g
            u.startsWith("k") -> qty * 1000.0 // kg -> g
            u == "g" || u == "gr" || u.startsWith("gram") -> qty // ya está en g

            // Volume, base in ml
            u == "l" || u.startsWith("liter") || u.startsWith("litro") -> qty * 1000.0 // L -> ml
            else -> qty // ml o desconocido se queda igual
        }
    }

    // Convert from base (g/ml) to target unit
    private fun fromBase(baseQty: Double, targetUnit: String): Double {
        val u = targetUnit.lowercase().trim()
        return when {
            u.startsWith("k") -> baseQty / 1000.0 // g -> kg
            u == "l" || u.startsWith("liter") || u.startsWith("litro") -> baseQty / 1000.0 // ml -> L
            else -> baseQty
        }
    }

    suspend fun clearCart() {
        val batch = db.batch()
        val allCartItems = userCart.get().await()
        allCartItems?.let {
            for (doc in it.documents) {
                batch.delete(doc.reference)
            }
        }
        batch.commit().await()
    }

    /**
     * Importante: el Map pasado a update tiene que ser Map<String, Any>,
     * no Map<String, Any?>. Construimos el mapa sin nulos y sólo añadimos
     * expirationDate si no es null.
     */
    suspend fun updateInventoryItem(
        id: String,
        newName: String,
        newQty: Double,
        newUnit: String,
        newExpirationDate: Long?
    ) {
        val updates = mutableMapOf<String, Any>(
            "name" to newName,
            "searchableName" to newName.lowercase(),
            "quantity" to newQty,
            "unit" to newUnit
        )
        newExpirationDate?.let { updates["expirationDate"] = it }

        userInventory.document(id).update(updates).await()
    }

    suspend fun deleteInventoryItem(id: String) {
        userInventory.document(id).delete().await()
    }

    suspend fun updateCartItem(id: String, newName: String, newQty: Double, newUnit: String) {
        val updates = mapOf(
            "name" to newName,
            "searchableName" to newName.lowercase(),
            "quantity" to newQty,
            "unit" to newUnit
        )
        userCart.document(id).update(updates).await()
    }

    suspend fun deleteCartItem(id: String) {
        userCart.document(id).delete().await()
    }

    // Add manually to cart
    suspend fun addOrUpdateCartItem(name: String, quantity: Double, unit: String) {
        val searchableName = name.lowercase().trim()
        val incomingType = getUnitType(unit)

        // Take all the ingridients with the same name
        val snapshot = userCart
            .whereEqualTo("searchableName", searchableName)
            .get()
            .await()

        // Check if any of them is compatible
        var compatibleDoc: com.google.firebase.firestore.DocumentSnapshot? = null

        if (snapshot != null && !snapshot.isEmpty) {
            for (doc in snapshot.documents) {
                val docUnit = doc.getString("unit") ?: ""
                val docType = getUnitType(docUnit)

                // Verify if the unit type is compatible
                val isCompatible = when {
                    incomingType == UnitType.MASS && docType == UnitType.MASS -> true
                    incomingType == UnitType.VOLUME && docType == UnitType.VOLUME -> true
                    incomingType == UnitType.OTHER && docType == UnitType.OTHER -> true // For pcs
                    else -> false
                }

                if (isCompatible) {
                    compatibleDoc = doc
                    break // Stops with the first compatible one
                }
            }
        }

        // Decide: update or create
        if (compatibleDoc != null) {
            // Update: add the quantity
            val currentQty = compatibleDoc.getDouble("quantity") ?: 0.0
            val currentUnit = compatibleDoc.getString("unit") ?: ""
            val currentInBase = toBase(currentQty, currentUnit)
            val incomingInBase = toBase(quantity, unit)

            val totalInBase = currentInBase + incomingInBase

            // Convert to the unit that was already in the cart
            val newTotalQty = fromBase(totalInBase, currentUnit)

            compatibleDoc.reference.update("quantity", newTotalQty).await()

        } else {
            // Crate (if not exists in the list or the unit is incompatible)
            val newItemRef = userCart.document()
            val newItem = CartItem(
                name = name.trim(),
                searchableName = searchableName,
                quantity = quantity,
                unit = unit
            )
            newItemRef.set(newItem).await()
        }
    }

}
