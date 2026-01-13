
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
        val count = getUserChatsRef().get().await().size()
        val newChat = Chat(
            userId = userId,
            title = "New chat ${count + 1}"
        )
        val docRef = getUserChatsRef().add(newChat).await()
        return docRef.id
    }

    suspend fun deleteChat(chatId: String) {
        getUserChatsRef().document(chatId).delete().await()
    }

    fun getMessagesFlow(chatId: String): Query {
        return getUserChatsRef().document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
    }

    suspend fun saveMessage(chatId: String, message: ChatMessage) {
        getUserChatsRef().document(chatId).collection("messages").add(message).await()
    }

    suspend fun updateChatTitle(chatId: String, newTitle: String) {
        getUserChatsRef().document(chatId).update("title", newTitle).await()
    }
}
