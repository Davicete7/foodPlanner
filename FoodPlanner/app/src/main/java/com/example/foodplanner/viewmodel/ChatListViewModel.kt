
package com.example.foodplanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.data.repo.ChatRepository
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch

class ChatListViewModel(private val userId: String) : ViewModel() {

    private val chatRepo = ChatRepository(userId)

    fun getChatsFlow(): Query {
        return chatRepo.getChatsFlow()
    }

    fun createNewChat(onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newChatId = chatRepo.createNewChat()
            onChatCreated(newChatId)
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            chatRepo.deleteChat(chatId)
        }
    }
}
