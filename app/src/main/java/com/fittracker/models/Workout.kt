package com.fittracker.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Workout(
    @SerializedName("_id") val id: String? = null,
    val userId: String,
    val startTime: Date,
    val endTime: Date,
    val distance: Float,
    val duration: Long,
    val steps: Int,
    val averageSpeed: Float,
    val calories: Int,
    val averageHeartRate: Int,
    val maxHeartRate: Int,
    val locations: List<LocationPoint>,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Date
) 