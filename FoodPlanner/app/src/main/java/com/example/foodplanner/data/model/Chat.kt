
package com.example.foodplanner.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Chat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Nueva Conversación",
    @ServerTimestamp val createdAt: Date? = null
)

data class ChatMessage(
    @DocumentId val id: String = "",
    val text: String = "",
    val isUser: Boolean = true,
    @ServerTimestamp val timestamp: Date? = null
)
