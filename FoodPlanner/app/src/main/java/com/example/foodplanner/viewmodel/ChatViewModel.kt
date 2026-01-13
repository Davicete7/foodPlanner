
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
                    "El inventario está vacío."
                } else {
                    inventory.joinToString("\n") { item ->
                        val expiry = item.expirationDate?.let {
                            " (caduca: ${java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date(it))})"
                        } ?: ""
                        "- ${item.name}: ${item.quantity} ${item.unit}${expiry}"
                    }
                }

                // Cargar el historial de chat actualizado cada vez
                val chatHistory = chatRepo.getMessages(chatId).map {
                    content(if (it.isUser) "user" else "model") { text(it.text) }
                }

                val systemPrompt = """
                Eres un chef experto. Tu tono es amigable y servicial.
                Tu tarea principal es ayudar al usuario a cocinar con lo que tiene.

                **INVENTARIO DEL USUARIO:**
                ```
                $inventoryList
                ```

                **INSTRUCCIONES IMPORTANTES:**
                1. **Si el usuario pide una receta específica** (ej: "¿cómo hago una tortilla de patatas?"):
                   - Primero, compara los ingredientes de la receta con el **INVENTARIO DEL USUARIO**.
                   - Informa amablemente qué ingredientes le faltan.
                   - Después, proporciona la receta completa (ingredientes y pasos).
                   - **NO** te niegues a dar la receta aunque no tenga los ingredientes.

                2. **Si el usuario hace una pregunta general** (ej: "¿qué puedo cocinar?", "¿alguna idea para la cena?"):
                   - Basa tus sugerencias **principalmente** en el **INVENTARIO DEL USUARIO**.
                   - Prioriza el uso de ingredientes que estén a punto de caducar.

                3. **Conversación:**
                   - Responde de forma natural. **No te presentes ("Hola, soy AI Chef") en cada mensaje.**
                   - Mantén el contexto de los mensajes anteriores.
                   - Sé conciso y responde siempre en español.

                **PREGUNTA DEL USUARIO:**
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
                val errorMessage = ChatMessage(text = "Hubo un error al contactar con el AI Chef: ${e.message}", isUser = false)
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
