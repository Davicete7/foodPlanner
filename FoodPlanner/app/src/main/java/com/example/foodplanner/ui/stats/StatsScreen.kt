package com.example.foodplanner.ui.stats

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodplanner.ui.auth.AuthViewModel
import com.example.foodplanner.ui.components.GreetingBar
import com.example.foodplanner.viewmodel.PantryViewModel

@Composable
fun StatsScreen(
    authViewModel: AuthViewModel = viewModel(),
    onBack: () -> Unit // Callback to navigate back if needed
) {
    val context = LocalContext.current
    val userState by authViewModel.user.collectAsState()

    if (userState == null) return

    // Initialize ViewModel to access real inventory data
    val factory = PantryViewModel.Factory(context.applicationContext as Application, userState!!.uid)
    val vm: PantryViewModel = viewModel(factory = factory)

    val inventory by vm.visibleInventory.collectAsState()

    // --- ANALYTICS CALCULATIONS ---

    // 1. Total count of items currently in the pantry
    val totalItems = inventory.size

    // 2. Identify items expiring within the next 7 days
    val currentTime = System.currentTimeMillis()
    val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000
    val expiringSoonCount = inventory.count { item ->
        item.expirationDate != null && (item.expirationDate!! - currentTime) < oneWeekInMillis
    }

    // 3. Group items by unit type for the Pie Chart distribution
    val unitDistribution = inventory.groupBy { it.unit }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    // Color palette for the charts
    val chartColors = listOf(
        Color(0xFF5C6BC0), // Indigo
        Color(0xFFEC407A), // Pink
        Color(0xFF26A69A), // Teal
        Color(0xFFFFA726), // Orange
        Color(0xFF78909C)  // Blue Grey
    )

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Reusable header component with profile menu
            GreetingBar(
                authViewModel = authViewModel,
                onStatsClick = { } // No action needed since we are already on the Stats screen
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Screen Title
                item {
                    Text(
                        text = "Analytics Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // --- SECTION 1: KEY METRICS ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SummaryCard(
                            title = "Total Items",
                            value = totalItems.toString(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            title = "Expiring Soon",
                            value = expiringSoonCount.toString(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // --- SECTION 2: INVENTORY COMPOSITION (REAL DATA) ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Inventory Composition", style = MaterialTheme.typography.titleMedium)
                            Text("Distribution by unit type", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(24.dp))

                            if (totalItems > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    // Custom Pie Chart using Canvas
                                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                        SimplePieChart(
                                            data = unitDistribution.map { it.second.toFloat() },
                                            colors = chartColors
                                        )
                                        // Display total count in the center of the donut chart
                                        Text(
                                            text = totalItems.toString(),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Dynamic Legend
                                    Column {
                                        unitDistribution.take(4).forEachIndexed { index, (unit, count) ->
                                            LegendItem(
                                                color = chartColors.getOrElse(index) { Color.Gray },
                                                text = "$unit: $count"
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "No items available to analyze.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )
                            }
                        }
                    }
                }

                // --- SECTION 3: WEEKLY ACTIVITY (MOCK DATA) ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Weekly Activity", style = MaterialTheme.typography.titleMedium)
                            Text("Items added vs consumed (Mock Data)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(24.dp))

                            // Custom Bar Chart using Canvas
                            ActivityBarChart(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER COMPONENTS ---

@Composable
fun SummaryCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Draws a simple Donut/Pie chart using Android Canvas.
 * Calculates sweep angles based on the percentage of total items.
 */
@Composable
fun SimplePieChart(data: List<Float>, colors: List<Color>) {
    val total = data.sum()
    var startAngle = -90f // Start from top

    Canvas(modifier = Modifier.fillMaxSize()) {
        data.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f
            val color = colors.getOrElse(index) { Color.Gray }

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 24.dp.toPx())
            )
            startAngle += sweepAngle
        }
    }
}

/**
 * Draws a Bar Chart to visualize user activity.
 * Since historical data is not yet in the DB, this uses mock data for demonstration.
 */
@Composable
fun ActivityBarChart(modifier: Modifier = Modifier) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    // Simulated data for items added and consumed
    val added = listOf(2, 5, 3, 4, 6, 2, 8)
    val consumed = listOf(1, 2, 4, 2, 3, 5, 2)
    val maxVal = 10f // Scale reference

    Canvas(modifier = modifier) {
        val barWidth = size.width / (days.size * 2.5f)
        val spacing = size.width / days.size

        days.forEachIndexed { index, day ->
            val x = index * spacing + spacing / 4

            // Draw "Added" bar (Blue)
            val addedHeight = (added[index] / maxVal) * size.height
            drawRect(
                color = Color(0xFF5C6BC0),
                topLeft = Offset(x, size.height - addedHeight),
                size = Size(barWidth, addedHeight)
            )

            // Draw "Consumed" bar (Pink/Transparent)
            val consumedHeight = (consumed[index] / maxVal) * size.height
            drawRect(
                color = Color(0xFFEC407A).copy(alpha = 0.8f),
                topLeft = Offset(x + barWidth, size.height - consumedHeight),
                size = Size(barWidth, consumedHeight)
            )
        }

        // Draw baseline
        drawLine(
            color = Color.LightGray,
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f
        )
    }

    // Labels row below the canvas
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
    }
}