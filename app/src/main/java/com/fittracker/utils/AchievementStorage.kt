package com.fittracker.utils

import android.content.Context

object AchievementStorage {
    private const val PREFS_NAME = "achievements_prefs"
    private const val KEY_LONGEST_DISTANCE = "longest_distance"
    private const val KEY_FASTEST_SPEED = "fastest_speed"
    private const val KEY_HIGHEST_CALORIES = "highest_calories"
    private const val KEY_HIGHEST_HEART_RATE = "highest_heart_rate"

    fun saveAchievements(
        context: Context,
        longestDistance: Double,
        fastestSpeed: Double,
        highestCalories: Double,
        highestHeartRate: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_LONGEST_DISTANCE, longestDistance.toFloat())
            .putFloat(KEY_FASTEST_SPEED, fastestSpeed.toFloat())
            .putFloat(KEY_HIGHEST_CALORIES, highestCalories.toFloat())
            .putInt(KEY_HIGHEST_HEART_RATE, highestHeartRate)
            .apply()
    }

    fun loadAchievements(context: Context): Achievements {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Achievements(
            longestDistance = prefs.getFloat(KEY_LONGEST_DISTANCE, 0f).toDouble(),
            fastestSpeed = prefs.getFloat(KEY_FASTEST_SPEED, 0f).toDouble(),
            highestCalories = prefs.getFloat(KEY_HIGHEST_CALORIES, 0f).toDouble(),
            highestHeartRate = prefs.getInt(KEY_HIGHEST_HEART_RATE, 0)
        )
    }

    data class Achievements(
        val longestDistance: Double,
        val fastestSpeed: Double,
        val highestCalories: Double,
        val highestHeartRate: Int
    )
} 