package com.example.foodplanner.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.data.model.Chat
import com.example.foodplanner.viewmodel.ChatListViewModel
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.compose.FirestoreLazyPagingItems
import com.firebase.ui.firestore.compose.items
import com.firebase.ui.firestore.compose.rememberFirestoreLazyPagingItems
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(onChatClick: (String) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Inicia sesión para ver tus chats.")
        }
        return
    }

    val viewModel: ChatListViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ChatListViewModel(userId) as T
        }
    })

    val chatsQuery = viewModel.getChatsFlow()
    val options = FirestoreRecyclerOptions.Builder<Chat>()
        .setQuery(chatsQuery, Chat::class.java)
        .build()
    val chats: FirestoreLazyPagingItems<Chat> = rememberFirestoreLazyPagingItems(options = options)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.createNewChat(onChatClick)
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo Chat")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(chats) { chat ->
                chat?.let {
                    ChatItem(
                        chat = it,
                        onDelete = { viewModel.deleteChat(it.id) },
                        onClick = { onChatClick(it.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatItem(chat: Chat, onDelete: () -> Unit, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(chat.title) },
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar Chat")
            }
        }
    )
}
