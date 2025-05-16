package com.fittracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fittracker.models.Workout
import com.fittracker.viewmodel.WorkoutViewModel
import com.fittracker.utils.WorkoutStatistics
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WorkoutHistoryScreen(
    onWorkoutClick: (String) -> Unit,
    viewModel: WorkoutViewModel = viewModel()
) {
    val workouts by viewModel.workouts.collectAsState()
    val stats by viewModel.workoutStats.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Statistics Section
        stats?.let { workoutStats ->
            StatisticsSection(workoutStats = workoutStats)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workout List
        Text(
            text = "Workout History",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (workouts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No workouts yet",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(workouts) { workout ->
                    WorkoutItem(
                        workout = workout,
                        dateFormat = dateFormat,
                        timeFormat = timeFormat,
                        onClick = { onWorkoutClick(workout.id!!) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatisticsSection(workoutStats: WorkoutStatistics.WorkoutStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Summary Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Total Workouts", value = workoutStats.totalWorkouts.toString())
                StatItem(label = "Total Distance", value = "${String.format("%.1f", workoutStats.totalDistance)} km")
                StatItem(label = "Total Calories", value = "${workoutStats.totalCalories} kcal")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Averages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Avg Distance", value = "${String.format("%.1f", workoutStats.averageDistance)} km")
                StatItem(label = "Avg Speed", value = "${String.format("%.1f", workoutStats.averageSpeed)} km/h")
                StatItem(label = "Avg Heart Rate", value = "${workoutStats.averageHeartRate} bpm")
            }

            // Records
            workoutStats.longestWorkout?.let { workout ->
                Text(
                    text = "Longest Workout: ${String.format("%.1f", workout.distance)} km",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            workoutStats.fastestWorkout?.let { workout ->
                Text(
                    text = "Fastest Workout: ${String.format("%.1f", workout.averageSpeed)} km/h",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            workoutStats.mostCaloriesBurned?.let { workout ->
                Text(
                    text = "Most Calories: ${workout.calories} kcal",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WorkoutItem(
    workout: Workout,
    dateFormat: SimpleDateFormat,
    timeFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateFormat.format(workout.startTime),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${timeFormat.format(workout.startTime)} - ${timeFormat.format(workout.endTime)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format("%.1f", workout.distance)} km",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${workout.calories} kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
} 