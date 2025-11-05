package com.example.foodplanner.ui.activities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodplanner.viewmodel.ActivityViewModel
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun ActivityDetailScreen(nav: NavController, id: Long, vm: ActivityViewModel = viewModel()) {
    val list by vm.entries.observeAsState(emptyList())
    val entry = list.find { it.id == id }
    val scope = rememberCoroutineScope()

    if (entry == null) {
        Text("Not found"); return
    }

    Column(Modifier.padding(16.dp)) {
        Text(entry.title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(entry.description)
        Spacer(Modifier.height(8.dp))
        Text("Date: ${Instant.ofEpochSecond(entry.dateTimeEpochSeconds)}")
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = { nav.navigate("activity_edit?entryId=${entry.id}") }) { Text("Edit") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = {
                scope.launch { vm.delete(entry); nav.popBackStack() }
            }) { Text("Delete") }
        }
    }
}
