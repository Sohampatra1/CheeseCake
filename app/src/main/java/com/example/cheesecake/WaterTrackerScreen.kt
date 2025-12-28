package com.example.cheesecake

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@Composable
fun WaterTrackerScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val records by viewModel.waterIntakeRecords.collectAsState(initial = emptyList())
    val context = LocalContext.current
    
    // Persistence for Reminder Toggle
    val sharedPref = remember { context.getSharedPreferences("cheesecake_prefs", android.content.Context.MODE_PRIVATE) }
    var isReminderEnabled by remember { 
        mutableStateOf(sharedPref.getBoolean("reminder_enabled", false)) 
    }

    // Ensure worker is scheduled if enabled (idempotent)
    LaunchedEffect(Unit) {
        if (isReminderEnabled) {
            NotificationManager.scheduleReminder(context)
        }
    }

    // Check for Alarm Trigger from Notification
    val activity = context as? android.app.Activity
    val intent = activity?.intent
    val triggerAlarm = intent?.getBooleanExtra("EXTRA_REMINDER_TRIGGER", false) ?: false
    
    LaunchedEffect(triggerAlarm) {
        if (triggerAlarm) {
            navController.navigate("camera?beep=true")
            intent?.removeExtra("EXTRA_REMINDER_TRIGGER")
        }
    }
    
    // Calculate Today's intake
    val todayCount = remember(records) {
        val startOfDay = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        records.count { it.timestamp >= startOfDay }
    }

    // Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7)) // iOS System Gray 6
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = "Hydration",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            // Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$todayCount",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007AFF) // iOS Blue
                            )
                        )
                        Text(
                            text = "Cups Today",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.Gray
                            )
                        )
                    }
                }
            }

            // Quick Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Verify Button (Primary)
                Button(
                    onClick = { navController.navigate("camera") },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF)
                    )
                ) {
                    Text("Verify Drink")
                }

                // Manual Button (Secondary)
                Button(
                    onClick = { viewModel.saveWaterIntakeRecord() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF007AFF)
                    )
                ) {
                    Text("Manual Log")
                }
            }
            
            // Settings / Extras Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // History
                     Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("calendar") }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("View History", style = MaterialTheme.typography.bodyLarge)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = Color.Gray
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // Reminders
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Reminders", style = MaterialTheme.typography.bodyLarge)
                            Text("Every 30 minutes", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { isEnabled ->
                                isReminderEnabled = isEnabled
                                sharedPref.edit().putBoolean("reminder_enabled", isEnabled).apply()
                                if (isEnabled) {
                                    NotificationManager.scheduleReminder(context)
                                } else {
                                    NotificationManager.cancelReminder(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF34C759) // iOS Green
                            )
                        )
                    }
                }
            }
        }
    }
}
