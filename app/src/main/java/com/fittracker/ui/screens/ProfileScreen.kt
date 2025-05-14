package com.fittracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.platform.LocalContext
import com.fittracker.utils.AchievementStorage

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user = FirebaseAuth.getInstance().currentUser
    var longestDistance by remember { mutableStateOf(0.0) }
    var fastestSpeed by remember { mutableStateOf(0.0) }
    var highestCalories by remember { mutableStateOf(0.0) }
    var highestHeartRate by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    fun loadAchievements() {
        loading = true
        error = null
        try {
            val achievements = AchievementStorage.loadAchievements(context)
            longestDistance = achievements.longestDistance
            fastestSpeed = achievements.fastestSpeed
            highestCalories = achievements.highestCalories
            highestHeartRate = achievements.highestHeartRate
            loading = false
        } catch (e: Exception) {
            error = "Failed to load achievements: ${e.message}"
            loading = false
        }
    }

    // Load achievements when the screen is first displayed
    LaunchedEffect(user) {
        loadAchievements()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { loadAchievements() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        
        Text("Profile", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Email: ${user?.email ?: "Unknown"}")
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Achievements", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            loading -> {
                CircularProgressIndicator()
            }
            error != null -> {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AchievementItem(
                        label = "Longest Distance",
                        value = String.format("%.2f km", longestDistance / 1000.0)
                    )
                    AchievementItem(
                        label = "Fastest Speed",
                        value = String.format("%.2f km/h", fastestSpeed * 3.6)
                    )
                    AchievementItem(
                        label = "Highest Calories",
                        value = String.format("%.0f kcal", highestCalories)
                    )
                    AchievementItem(
                        label = "Highest Heart Rate",
                        value = "$highestHeartRate bpm"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = {
            FirebaseAuth.getInstance().signOut()
            onLogout()
        }) {
            Text("Log out")
        }
    }
}

@Composable
private fun AchievementItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
} 