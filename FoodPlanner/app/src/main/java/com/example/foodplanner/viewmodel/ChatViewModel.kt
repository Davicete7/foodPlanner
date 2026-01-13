
package com.example.foodplanner.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.BuildConfig
import com.example.foodplanner.data.model.ChatMessage
import com.example.foodplanner.data.repo.ChatRepository
import com.example.foodplanner.data.repo.PantryRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChatViewModel(app: Application, private val userId: String, private val chatId: String) : AndroidViewModel(app) {

    private val pantryRepo = PantryRepository(userId)
    private val chatRepo = ChatRepository(userId)

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val generativeModel: GenerativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    fun getMessagesFlow(): Query {
        return chatRepo.getMessagesFlow(chatId)
    }

    fun sendMessage(question: String) {
        if (question.isBlank()) return

        viewModelScope.launch {
            val userMessage = ChatMessage(text = question, isUser = true)
            chatRepo.saveMessage(chatId, userMessage)
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val inventory = pantryRepo.inventory.first()
                val inventoryList = if (inventory.isEmpty()) {
                    "The inventory is empty."
                } else {
                    inventory.joinToString("\n") { item ->
                        val expiry = item.expirationDate?.let {
                            " (expires: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(it))})"
                        } ?: ""
                        "- ${item.name}: ${item.quantity} ${item.unit}${expiry}"
                    }
                }

                // Load the updated chat history each time
                val chatHistory = chatRepo.getMessages(chatId).map {
                    content(if (it.isUser) "user" else "model") { text(it.text) }
                }

                val systemPrompt = """
                You are an expert chef. Your tone is friendly and helpful.
                Your main task is to help the user cook with what they have.

                **USER'S INVENTORY:**
                ```
                $inventoryList
                ```

                **IMPORTANT INSTRUCTIONS:**
                1. **If the user asks for a specific recipe** (e.g., "how do I make a Spanish omelette?"):
                   - First, compare the recipe's ingredients with the **USER'S INVENTORY**.
                   - Kindly inform them which ingredients are missing.
                   - Then, provide the full recipe (ingredients and steps).
                   - **DO NOT** refuse to provide the recipe even if they don't have the ingredients.

                2. **If the user asks a general question** (e.g., "what can I cook?", "any ideas for dinner?"):
                   - Base your suggestions **mainly** on the **USER'S INVENTORY**.
                   - Prioritize using ingredients that are about to expire.

                3. **Conversation:**
                   - Respond naturally. **Do not introduce yourself ("Hello, I'm AI Chef") in every message.**
                   - Maintain the context of previous messages.
                   - Be concise and always respond in English.

                **USER'S QUESTION:**
                "$question"
                """

                val chat = generativeModel.startChat(history = chatHistory)
                val response = chat.sendMessage(systemPrompt)

                response.text?.let {
                    val aiMessage = ChatMessage(text = it, isUser = false)
                    chatRepo.saveMessage(chatId, aiMessage)
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error calling Gemini API", e)
                val errorMessage = ChatMessage(text = "There was an error contacting the AI Chef: ${e.message}", isUser = false)
                chatRepo.saveMessage(chatId, errorMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    companion object {
        fun provideFactory(
            app: Application,
            userId: String,
            chatId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return ChatViewModel(app, userId, chatId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
