package com.example.foodplanner.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodplanner.R // <--- IMPORTANT: Verify this import matches your package
import com.example.foodplanner.ui.auth.AuthViewModel
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun GreetingBar(authViewModel: AuthViewModel) {
    val user by authViewModel.user.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var isGreetingVisible by remember { mutableStateOf(true) }

    // Determine greeting based on current time
    val greetingText = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> "Good morning! Ready to plan your meals?"
            in 12..18 -> "Good afternoon! Keep up the energy."
            else -> "Good evening! Planning something delicious?"
        }
    }

    // Auto-dismiss notification after 5 seconds
    LaunchedEffect(Unit) {
        delay(5000)
        isGreetingVisible = false
    }

    // Main Header Surface (Blue bar)
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // --- ANIMATED LEFT SECTION (Notification <-> Logo) ---
            Box(modifier = Modifier.weight(1f)) {
                Crossfade(
                    targetState = isGreetingVisible,
                    animationSpec = tween(durationMillis = 500),
                    label = "HeaderAnimation"
                ) { showNotification ->
                    if (showNotification) {
                        // STATE A: Notification Message
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = greetingText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                            )
                        }
                    } else {
                        // STATE B: App Logo and Title
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.icon_foreground),
                                contentDescription = "App Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Food Planner",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- PERMANENT RIGHT SECTION (Profile) ---
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = user?.email ?: "No email",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        onClick = { },
                        enabled = false
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Logout") },
                        onClick = {
                            showMenu = false
                            authViewModel.logout()
                        }
                    )
                }
            }
        }
    }
}