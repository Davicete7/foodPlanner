
package com.example.foodplanner.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    val chatsQuery = viewModel.getChatsFlow()
    val chats by produceState<List<Chat>>(initialValue = emptyList(), key1 = chatsQuery) {
        val listener = chatsQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }
            if (snapshot != null) {
                value = snapshot.toObjects(Chat::class.java)
            }
        }
        awaitDispose { listener.remove() }
    }

    var editingChat by remember { mutableStateOf<Chat?>(null) }

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
                ChatItem(
                    chat = chat,
                    onDelete = { viewModel.deleteChat(chat.id) },
                    onEdit = { editingChat = chat },
                    onClick = { onChatClick(chat.id) }
                )
            }
        }

        editingChat?.let {
            EditChatTitleDialog(
                chat = it,
                onDismiss = { editingChat = null },
                onConfirm = { newTitle ->
                    viewModel.updateChatTitle(it.id, newTitle)
                    editingChat = null
                }
            )
        }
    }
}

@Composable
fun ChatItem(chat: Chat, onDelete: () -> Unit, onEdit: () -> Unit, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(chat.title.ifBlank { "Conversación sin título" }) },
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar Título")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar Chat")
                }
            }
        }
    )
}

@Composable
fun EditChatTitleDialog(chat: Chat, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var title by remember { mutableStateOf(chat.title) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar título del chat") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nuevo título") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(title) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
