package com.fittracker.service

import android.location.Location
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TrackingStats(
    val isTracking: Boolean = false,
    val distance: Float = 0f, // meters
    val duration: Long = 0L, // ms
    val locations: List<Location> = emptyList(),
    val steps: Int = 0,
    val speed: Float = 0f, // m/s
    val calories: Float = 0f,
    val currentHeartRate: Int = 0,
    val averageHeartRate: Int = 0,
    val maxHeartRate: Int = 0
)

class TrackingState {
    private val _stats = MutableStateFlow(TrackingStats())
    val stats: StateFlow<TrackingStats> = _stats

    fun updateStats(newStats: TrackingStats) {
        _stats.value = newStats
    }
}

object TrackingStatsCalculator {
    // Estimate steps: average step length = 0.78m
    fun estimateSteps(distance: Float): Int = (distance / 0.78f).toInt()

    // Calculate average speed in m/s, with smoothing
    fun calculateSpeed(distance: Float, duration: Long): Float {
        if (duration <= 0) return 0f
        
        // Convert to km/h for more intuitive values
        val speedKmh = (distance / 1000f) / (duration / 3600000f)
        
        // Apply smoothing and limits
        return when {
            speedKmh < 0.5f -> 0f // Ignore very slow speeds (likely GPS drift)
            speedKmh > 200f -> 200f // Cap at 200 km/h
            else -> speedKmh / 3.6f // Convert back to m/s
        }
    }

    // Estimate calories burned using a more accurate formula
    // Based on MET (Metabolic Equivalent of Task) values
    fun estimateCalories(distance: Float, weightKg: Float = 70f): Float {
        // Average walking/running MET value
        val met = 6.0f
        
        // Calculate time in hours (assuming average speed of 2.5 m/s)
        val timeHours = (distance / 2.5f) / 3600f
        
        // Calories = MET * weight * time
        return met * weightKg * timeHours
    }
} 