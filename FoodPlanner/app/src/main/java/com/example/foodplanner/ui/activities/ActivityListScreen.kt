package com.example.foodplanner.ui.activities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.foodplanner.data.db.entities.ActivityEntry
import com.example.foodplanner.ui.nav.Routes
import com.example.foodplanner.viewmodel.ActivityViewModel
import java.time.Instant

@Composable
fun ActivityListScreen(nav: NavController, vm: ActivityViewModel = viewModel()) {
    val entries by vm.entries.observeAsState(emptyList())
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(Routes.ActivityEdit) }) {
                Text("+")
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(entries) { e ->
                ListItem(
                    headlineContent = { Text(e.title) },
                    supportingContent = {
                        Text("${e.description} • ${Instant.ofEpochSecond(e.dateTimeEpochSeconds)}")
                    },
                    modifier = Modifier
                        .clickable { nav.navigate("activity_detail/${e.id}") }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Divider()
            }
        }
    }
}
