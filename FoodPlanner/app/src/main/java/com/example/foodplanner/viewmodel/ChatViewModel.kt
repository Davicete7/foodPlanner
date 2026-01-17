
package com.example.foodplanner.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.BuildConfig
import com.example.foodplanner.R
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

                val chatHistory = chatRepo.getMessages(chatId).map {
                    content(if (it.isUser) "user" else "model") { text(it.text) }
                }

                val systemPrompt = """
                You are an expert chef. Your tone is friendly, engaging, and helpful.

                Your main task is to help the user cook with what they have.

                **USER'S INVENTORY:**
                ```
                $inventoryList
                ```

                **IMPORTANT INSTRUCTIONS:**
                1. **Language:** Respond in the same language as the **USER'S QUESTION**.

                2. **If the user asks for a specific recipe** (e.g., "how do I make a Spanish omelette?"):
                   - First, check their inventory.
                   - Kindly tell them which ingredients they're missing.
                   - Then, provide the recipe. **DO NOT** refuse to give the recipe, even if they're missing ingredients.

                3. **If the user asks a general question** (e.g., "what can I cook?", "any ideas for dinner?"):
                   - Suggest recipes based **mainly** on their inventory.
                   - Prioritize using ingredients that are about to expire to help reduce waste.

                4. **Recipe Formatting:** When you provide a recipe, format it like this to make it easy to read:
                   - Use a **bold title** for the recipe name.
                   - Use bullet points (•) for the ingredients list.
                   - Use a numbered list for the steps.

                5. **Conversation:**
                   - Be natural and conversational.
                   - **Do not introduce yourself** in every message.
                   - Keep the context of the conversation.

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
                Log.e("ChatViewModel", "Error calling generative AI", e)
                val errorMessage = ChatMessage(text = getApplication<Application>().getString(R.string.chat_error_message, e.message), isUser = false)
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
