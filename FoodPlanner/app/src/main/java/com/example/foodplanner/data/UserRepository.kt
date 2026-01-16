package com.example.foodplanner.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.example.foodplanner.utils.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun createUser(user: User) {
        db.collection("users").document(user.uid).set(user).await()
    }

    suspend fun getUser(uid: String): User? {
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot?.toObject<User>()
    }

    suspend fun updateUser(uid: String, updates: Map<String, Any>) {
        db.collection("users").document(uid).update(updates).await()
    }
}