package com.example.foodplanner.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodplanner.R
import com.example.foodplanner.ui.auth.AuthViewModel
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun GreetingBar(
    authViewModel: AuthViewModel,
    onStatsClick: () -> Unit, // Callback to navigate to Analytics screen
    onSettingsClick: () -> Unit
) {
    val user by authViewModel.user.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var isGreetingVisible by remember { mutableStateOf(true) }

    // Logic to select the greeting message based on time of day
    val greetingText = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..18 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        }
    }

    // Auto-hide the notification part after 5 seconds
    LaunchedEffect(Unit) {
        delay(5000)
        isGreetingVisible = false
    }

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

            // --- ANIMATED HEADER CONTENT ---
            Box(modifier = Modifier.weight(1f)) {
                Crossfade(
                    targetState = isGreetingVisible,
                    animationSpec = tween(durationMillis = 500),
                    label = "HeaderAnimation"
                ) { showNotification ->
                    if (showNotification) {
                        // STATE A: Greeting Notification
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(id = greetingText),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                            )
                        }
                    } else {
                        // STATE B: App Identity
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.icon_foreground),
                                    contentDescription = stringResource(id = R.string.app_logo_content_description),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = stringResource(id = R.string.food_planner_title),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            user?.firstName?.let { name ->
                                if (name.isNotEmpty()) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- PROFILE MENU ---
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(id = R.string.profile_content_description),
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Item 1: User Email (Disabled/Info)
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = user?.email ?: stringResource(id = R.string.no_email),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        onClick = { },
                        enabled = false
                    )

                    HorizontalDivider()

                    // Item 2: Analytics & Insights (Navigation)
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.analytics)) },
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onStatsClick() // Trigger navigation
                        }
                    )
                    // Item 3: Settings
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.settings)) },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onSettingsClick()
                        }
                    )

                    // Item 4: Logout
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.logout)) },
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