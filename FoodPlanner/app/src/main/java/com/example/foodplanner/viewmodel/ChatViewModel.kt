
package com.example.foodplanner.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val functions = Firebase.functions("us-central1") // Asegúrate que coincida con la región de despliegue

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun sendMessage(question: String) {
        if (question.isBlank()) return

        // Añadir mensaje del usuario
        val currentList = _messages.value.toMutableList()
        currentList.add(ChatMessage(question, true))
        _messages.value = currentList

        _isLoading.value = true

        viewModelScope.launch {
            try {
                // Llamada a la Cloud Function "suggestRecipe"
                val result = functions
                    .getHttpsCallable("suggestRecipe")
                    .call(hashMapOf("question" to question))
                    .await()

                val data = result.data as? Map<String, Any>
                val responseText = data?.get("text") as? String ?: "Error al procesar respuesta"

                // Añadir respuesta de la IA
                val newList = _messages.value.toMutableList()
                newList.add(ChatMessage(responseText, false))
                _messages.value = newList

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error calling function", e)
                val newList = _messages.value.toMutableList()
                newList.add(ChatMessage("Error: ${e.message}", false))
                _messages.value = newList
            } finally {
                _isLoading.value = false
            }
        }
    }
}
