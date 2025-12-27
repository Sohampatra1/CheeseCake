package com.example.cheesecake

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    navController: NavController,
    viewModel: MainViewModel = hiltViewModel()
) {
    val records by viewModel.waterIntakeRecords.collectAsState(initial = emptyList())
    val currentMonth = remember { mutableStateOf(YearMonth.now()) }
    val selectedDate = remember { mutableStateOf(LocalDate.now()) }

    // Group records by Date
    val recordsByDate = remember(records) {
        records.groupBy {
            Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Hydration History",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Calendar Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth.value = currentMonth.value.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, "Prev Month")
            }
            Text(
                "${currentMonth.value.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.value.year}",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = { currentMonth.value = currentMonth.value.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, "Next Month")
            }
        }

        // Days Header
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(day, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }

        // Calendar Grid
        val daysInMonth = currentMonth.value.lengthOfMonth()
        val firstDayOfMonth = currentMonth.value.atDay(1).dayOfWeek.value % 7 // Sun=0, Sat=6 handling
        // Note: java.time DayOfWeek is Mon=1 ... Sun=7. We want Sun=0 for standard grid.
        // If Mon=1, Sun=7.  %7 -> Mon=1, Sun=0. Correct.

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.height(300.dp)
        ) {
            // Empty slots for start of month
            items(firstDayOfMonth) {
                Box(modifier = Modifier.size(40.dp))
            }

            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val date = currentMonth.value.atDay(day)
                val isSelected = date == selectedDate.value
                val hasHydration = recordsByDate.containsKey(date)
                val isToday = date == LocalDate.now()

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(2.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { selectedDate.value = date },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day.toString(),
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        if (hasHydration && !isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(Color.Blue, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // Selected Date Details
        Text(
            "Records for ${selectedDate.value.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val dayRecords = recordsByDate[selectedDate.value] ?: emptyList()

        if (dayRecords.isEmpty()) {
            Text(
                "No hydration records for this day.",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn {
                items(dayRecords) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.WaterDrop, // Assuming available or generic
                                contentDescription = "Water",
                                tint = Color.Blue
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = Instant.ofEpochMilli(record.timestamp)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofPattern("hh:mm a")),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
