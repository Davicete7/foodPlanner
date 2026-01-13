
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

    private lateinit var generativeModel: GenerativeModel

    init {
        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = BuildConfig.GEMINI_API_KEY
            )
        }
    }

    fun getMessagesFlow(): Query {
        return chatRepo.getMessagesFlow(chatId)
    }

    fun sendMessage(question: String) {
        if (question.isBlank() || !::generativeModel.isInitialized) return

        val userMessage = ChatMessage(text = question, isUser = true)
        viewModelScope.launch {
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

                val prompt = """
                    Actúa como un chef experto y amigable llamado "AI Chef". Tu tarea es ayudar a un usuario a decidir qué cocinar.

                    Aquí está el inventario actual del usuario:
                    $inventoryList

                    Esta es la pregunta del usuario:
                    "$question"

                    Instrucciones:
                    1. Recomienda una o dos recetas sencillas que se puedan hacer con los ingredientes del inventario.
                    2. Prioriza el uso de ingredientes que estén a punto de caducar.
                    3. Si para una receta faltan uno o dos ingredientes clave, menciónalo como una sugerencia (ej. "si tuvieras huevos, podrías hacer...").
                    4. La respuesta debe ser concisa, amigable y fácil de entender. No uses formatos complejos.
                    5. Responde siempre en español.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)

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
