package com.example.foodplanner.ui.activities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodplanner.data.db.entities.ActivityEntry
import com.example.foodplanner.viewmodel.ActivityViewModel
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun ActivityEditScreen(nav: NavController, entryId: Long?, vm: ActivityViewModel = viewModel()) {
    val list by vm.entries.observeAsState(emptyList())
    val original = entryId?.let { id -> list.find { it.id == id } }

    var title by remember { mutableStateOf(original?.title ?: "") }
    var desc by remember { mutableStateOf(original?.description ?: "") }
    var epoch by remember { mutableStateOf((original?.dateTimeEpochSeconds ?: Instant.now().epochSecond).toString()) }

    val scope = rememberCoroutineScope()

    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(desc, { desc = it }, label = { Text("Descripción") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            epoch, { epoch = it },
            label = { Text("Fecha/hora (epoch seconds)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = {
            val whenEpoch = epoch.toLongOrNull() ?: Instant.now().epochSecond
            scope.launch {
                if (original == null) {
                    vm.add(title, desc, Instant.ofEpochSecond(whenEpoch))
                } else {
                    vm.update(original.copy(title = title, description = desc, dateTimeEpochSeconds = whenEpoch))
                }
                nav.popBackStack()
            }
        }) { Text("Guardar") }
    }
}
