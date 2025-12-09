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

        for ((name, qty, unit) in need) {
            val searchableName = name.lowercase()
            val have = currentInventory[searchableName]?.quantity ?: 0.0
            val missing = (qty - have).coerceAtLeast(0.0)

            if (missing > 0) {
                val existingCartItem = currentCart[searchableName]
                if (existingCartItem == null) {
                    val newCartItemRef = userCart.document()
                    batch.set(
                        newCartItemRef,
                        CartItem(
                            name = name,
                            searchableName = searchableName,
                            quantity = missing,
                            unit = unit
                        )
                    )
                } else {
                    existingCartItem.id?.let {
                        val docRef = userCart.document(it)
                        val newQty = existingCartItem.quantity + missing
                        batch.update(docRef, "quantity", newQty)
                    }
                }
            }
        }
        batch.commit().await()
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
}
