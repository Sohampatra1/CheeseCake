package com.example.cheesecake

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.clickable
import androidx.navigation.NavController

@Composable
fun PeriodTrackerScreen(
    navController: NavController? = null,
    viewModel: PeriodViewModel = hiltViewModel()
) {
    val records by viewModel.periodRecords.collectAsState()
    val lastPeriod by viewModel.lastPeriod.collectAsState()
    
    val statusText = viewModel.getCycleStatus(lastPeriod?.timestamp)
    val nextDateText = if (lastPeriod != null) {
        val lastDate = java.time.Instant.ofEpochMilli(lastPeriod!!.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .plusDays(28)
        java.time.format.DateTimeFormatter.ofPattern("MMMM dd").format(lastDate)
    } else {
        "--"
    }

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
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Header
            Text(
                text = "Cycle Tracking",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ),
                modifier = Modifier.align(Alignment.Start)
            )

            // Main Cycle Card (Circular Progress Style)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Decorative Circle (Background)
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .background(
                                color = Color(0xFFFFEBEF), // Light Pink
                                shape = CircleShape
                            )
                    )
                    
                    // Decorative Circle (Progress - simulated)
                    // In a real app we'd draw an arc
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (lastPeriod != null) "Next Period" else "Welcome",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.Gray)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (lastPeriod != null) nextDateText else "Start",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF2D55) // iOS Pink/Red
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        )
                    }
                }
            }

            // Log Button
            Button(
                onClick = { viewModel.logPeriodStart() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF2D55) // iOS Pink/Red
                )
            ) {
                Text(
                    text = "Log Period Started Today",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            
    // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Prediction",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Based on a 28-day cycle. Predictions will improve as you log more data.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                    )
                }
            }

            // History Link
            if (navController != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("calendar") }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "View History",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
