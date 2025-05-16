package com.fittracker.utils

import com.fittracker.models.Workout
import java.util.*
import kotlin.math.roundToInt

class WorkoutStatistics {
    data class WorkoutStats(
        val totalWorkouts: Int,
        val totalDistance: Float,
        val totalDuration: Long,
        val totalCalories: Int,
        val averageDistance: Float,
        val averageDuration: Long,
        val averageCalories: Int,
        val averageSpeed: Float,
        val averageHeartRate: Int,
        val maxHeartRate: Int,
        val longestWorkout: Workout?,
        val fastestWorkout: Workout?,
        val mostCaloriesBurned: Workout?,
        val weeklyProgress: List<WeeklyProgress>,
        val monthlyProgress: List<MonthlyProgress>
    )

    data class WeeklyProgress(
        val weekStart: Date,
        val totalDistance: Float,
        val totalDuration: Long,
        val totalCalories: Int,
        val workoutCount: Int
    )

    data class MonthlyProgress(
        val monthStart: Date,
        val totalDistance: Float,
        val totalDuration: Long,
        val totalCalories: Int,
        val workoutCount: Int
    )

    companion object {
        fun calculateStats(workouts: List<Workout>): WorkoutStats {
            if (workouts.isEmpty()) {
                return WorkoutStats(
                    totalWorkouts = 0,
                    totalDistance = 0f,
                    totalDuration = 0L,
                    totalCalories = 0,
                    averageDistance = 0f,
                    averageDuration = 0L,
                    averageCalories = 0,
                    averageSpeed = 0f,
                    averageHeartRate = 0,
                    maxHeartRate = 0,
                    longestWorkout = null,
                    fastestWorkout = null,
                    mostCaloriesBurned = null,
                    weeklyProgress = emptyList(),
                    monthlyProgress = emptyList()
                )
            }

            val totalDistance = workouts.sumOf { it.distance.toDouble() }.toFloat()
            val totalDuration = workouts.sumOf { it.duration }
            val totalCalories = workouts.sumOf { it.calories }
            val totalHeartRate = workouts.sumOf { it.averageHeartRate }

            val longestWorkout = workouts.maxByOrNull { it.duration }
            val fastestWorkout = workouts.maxByOrNull { it.averageSpeed }
            val mostCaloriesBurned = workouts.maxByOrNull { it.calories }

            // Calculate weekly progress
            val weeklyProgress = calculateWeeklyProgress(workouts)
            
            // Calculate monthly progress
            val monthlyProgress = calculateMonthlyProgress(workouts)

            return WorkoutStats(
                totalWorkouts = workouts.size,
                totalDistance = totalDistance,
                totalDuration = totalDuration,
                totalCalories = totalCalories,
                averageDistance = totalDistance / workouts.size,
                averageDuration = totalDuration / workouts.size,
                averageCalories = totalCalories / workouts.size,
                averageSpeed = workouts.map { it.averageSpeed }.average().toFloat(),
                averageHeartRate = totalHeartRate / workouts.size,
                maxHeartRate = workouts.maxOf { it.maxHeartRate },
                longestWorkout = longestWorkout,
                fastestWorkout = fastestWorkout,
                mostCaloriesBurned = mostCaloriesBurned,
                weeklyProgress = weeklyProgress,
                monthlyProgress = monthlyProgress
            )
        }

        private fun calculateWeeklyProgress(workouts: List<Workout>): List<WeeklyProgress> {
            val calendar = Calendar.getInstance()
            val weeklyMap = mutableMapOf<Date, WeeklyProgress>()

            workouts.forEach { workout ->
                calendar.time = workout.startTime
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = calendar.time

                val current = weeklyMap.getOrPut(weekStart) {
                    WeeklyProgress(weekStart, 0f, 0L, 0, 0)
                }

                weeklyMap[weekStart] = current.copy(
                    totalDistance = current.totalDistance + workout.distance,
                    totalDuration = current.totalDuration + workout.duration,
                    totalCalories = current.totalCalories + workout.calories,
                    workoutCount = current.workoutCount + 1
                )
            }

            return weeklyMap.values.sortedBy { it.weekStart }
        }

        private fun calculateMonthlyProgress(workouts: List<Workout>): List<MonthlyProgress> {
            val calendar = Calendar.getInstance()
            val monthlyMap = mutableMapOf<Date, MonthlyProgress>()

            workouts.forEach { workout ->
                calendar.time = workout.startTime
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.time

                val current = monthlyMap.getOrPut(monthStart) {
                    MonthlyProgress(monthStart, 0f, 0L, 0, 0)
                }

                monthlyMap[monthStart] = current.copy(
                    totalDistance = current.totalDistance + workout.distance,
                    totalDuration = current.totalDuration + workout.duration,
                    totalCalories = current.totalCalories + workout.calories,
                    workoutCount = current.workoutCount + 1
                )
            }

            return monthlyMap.values.sortedBy { it.monthStart }
        }
    }
} 