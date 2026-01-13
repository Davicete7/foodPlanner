package com.example.foodplanner.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.data.model.Chat
import com.example.foodplanner.viewmodel.ChatListViewModel
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

    // Opción A: Convertir la Query de Firestore a un Estado de Compose manualmente.
    // Esto evita usar la librería 'firebase-ui-compose' que faltaba.
    val chatsQuery = viewModel.getChatsFlow()

    val chats by produceState<List<Chat>>(initialValue = emptyList(), key1 = chatsQuery) {
        // Agregamos un listener en tiempo real
        val listener = chatsQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Puedes manejar el error aquí si lo deseas
                return@addSnapshotListener
            }
            if (snapshot != null) {
                // Convertimos los documentos a objetos Chat
                value = snapshot.toObjects(Chat::class.java)
            }
        }
        // Limpiamos el listener cuando la pantalla se destruye
        awaitDispose { listener.remove() }
    }

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
            // Usamos 'items' estándar de Compose
            items(chats) { chat ->
                ChatItem(
                    chat = chat,
                    onDelete = { viewModel.deleteChat(chat.id) },
                    onClick = { onChatClick(chat.id) }
                )
            }
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onDelete: () -> Unit, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(chat.title.ifBlank { "Conversación sin título" }) },
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar Chat")
            }
        }
    )
}