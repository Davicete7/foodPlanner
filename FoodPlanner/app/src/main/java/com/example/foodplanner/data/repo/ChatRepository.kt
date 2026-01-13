
package com.example.foodplanner.data.repo

import com.example.foodplanner.data.model.Chat
import com.example.foodplanner.data.model.ChatMessage
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ChatRepository(private val userId: String) {

    private val db = FirebaseFirestore.getInstance()

    private fun getUserChatsRef(): CollectionReference {
        return db.collection("users").document(userId).collection("chats")
    }

    fun getChatsFlow(): Query {
        return getUserChatsRef().orderBy("createdAt", Query.Direction.DESCENDING)
    }

    suspend fun createNewChat(): String {
        val newChat = Chat(userId = userId)
        val docRef = getUserChatsRef().add(newChat).await()
        return docRef.id
    }

    suspend fun deleteChat(chatId: String) {
        getUserChatsRef().document(chatId).delete().await()
        // También necesitarás borrar todos los mensajes de este chat (subcolección)
        // Esto se puede hacer con una función de Cloud o un batch job, ya que borrar colecciones directamente
        // desde el cliente no es recomendable para colecciones grandes.
        // Por simplicidad aquí, lo dejamos así, pero es algo a tener en cuenta.
    }

    fun getMessagesFlow(chatId: String): Query {
        return getUserChatsRef().document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
    }

    suspend fun saveMessage(chatId: String, message: ChatMessage) {
        getUserChatsRef().document(chatId).collection("messages").add(message).await()
    }
}
