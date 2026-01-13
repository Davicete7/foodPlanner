package com.example.foodplanner.ui.chat

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.data.model.ChatMessage
import com.example.foodplanner.viewmodel.ChatViewModel
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.compose.FirestoreLazyPagingItems
import com.firebase.ui.firestore.compose.rememberFirestoreLazyPagingItems
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ChatScreen(chatId: String) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    if (userId == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Inicia sesión para usar el AI Chef.")
        }
        return
    }

    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.provideFactory(application, userId, chatId)
    )

    val messagesQuery = viewModel.getMessagesFlow()
    val options = FirestoreRecyclerOptions.Builder<ChatMessage>()
        .setQuery(messagesQuery, ChatMessage::class.java)
        .build()
    val messages: FirestoreLazyPagingItems<ChatMessage> = rememberFirestoreLazyPagingItems(options = options)

    var inputText by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = false
        ) {
            items(count = messages.itemCount) { index ->
                val msg = messages[index]
                if (msg != null) {
                    MessageBubble(msg.text, msg.isUser)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("¿Qué cocino hoy?") },
                enabled = !isLoading
            )
            IconButton(onClick = {
                if (inputText.isNotBlank()) {
                    viewModel.sendMessage(inputText)
                    inputText = ""
                }
            }, enabled = !isLoading) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

@Composable
fun MessageBubble(text: String, isUser: Boolean) {
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val align = if (isUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
