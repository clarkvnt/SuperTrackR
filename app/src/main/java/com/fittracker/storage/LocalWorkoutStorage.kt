package com.fittracker.storage

import android.content.Context
import android.content.SharedPreferences
import com.fittracker.models.Workout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class LocalWorkoutStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveWorkout(workout: Workout): String {
        val workouts = getWorkoutsByUser(workout.userId).toMutableList()
        val workoutId = UUID.randomUUID().toString()
        val workoutWithId = workout.copy(id = workoutId)
        workouts.add(workoutWithId)
        saveWorkouts(workout.userId, workouts)
        return workoutId
    }

    fun getWorkoutsByUser(userId: String): List<Workout> {
        val json = prefs.getString(getKeyForUser(userId), null) ?: return emptyList()
        val type = object : TypeToken<List<Workout>>() {}.type
        return gson.fromJson(json, type)
    }

    fun getWorkout(workoutId: String): Workout? {
        val allWorkouts = getAllWorkouts()
        return allWorkouts.find { it.id == workoutId }
    }

    fun updateWorkout(workoutId: String, updatedWorkout: Workout): Boolean {
        // Update in global list
        val allWorkouts = getAllWorkouts().toMutableList()
        val index = allWorkouts.indexOfFirst { it.id == workoutId }
        if (index == -1) return false
        allWorkouts[index] = updatedWorkout.copy(id = workoutId)
        saveAllWorkouts(allWorkouts)

        // Update in per-user list
        val userId = updatedWorkout.userId
        val userWorkouts = getWorkoutsByUser(userId).toMutableList()
        val userIndex = userWorkouts.indexOfFirst { it.id == workoutId }
        if (userIndex != -1) {
            userWorkouts[userIndex] = updatedWorkout.copy(id = workoutId)
            saveWorkouts(userId, userWorkouts)
        }
        return true
    }

    fun deleteWorkout(workoutId: String): Boolean {
        // Remove from global list
        val allWorkouts = getAllWorkouts().toMutableList()
        val workout = allWorkouts.find { it.id == workoutId } ?: return false
        allWorkouts.removeIf { it.id == workoutId }
        saveAllWorkouts(allWorkouts)

        // Remove from per-user list
        val userId = workout.userId
        val userWorkouts = getWorkoutsByUser(userId).toMutableList()
        val removed = userWorkouts.removeIf { it.id == workoutId }
        if (removed) {
            saveWorkouts(userId, userWorkouts)
        }
        return true
    }

    private fun getAllWorkouts(): List<Workout> {
        val json = prefs.getString(KEY_ALL_WORKOUTS, null) ?: return emptyList()
        val type = object : TypeToken<List<Workout>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveAllWorkouts(workouts: List<Workout>) {
        val json = gson.toJson(workouts)
        prefs.edit().putString(KEY_ALL_WORKOUTS, json).apply()
    }

    private fun saveWorkouts(userId: String, workouts: List<Workout>) {
        val json = gson.toJson(workouts)
        prefs.edit().putString(getKeyForUser(userId), json).apply()
        
        // Also update the all workouts list
        val allWorkouts = getAllWorkouts().toMutableList()
        workouts.forEach { workout ->
            val index = allWorkouts.indexOfFirst { it.id == workout.id }
            if (index != -1) {
                allWorkouts[index] = workout
            } else {
                allWorkouts.add(workout)
            }
        }
        saveAllWorkouts(allWorkouts)
    }

    private fun getKeyForUser(userId: String) = "workouts_$userId"

    companion object {
        private const val PREFS_NAME = "workout_storage"
        private const val KEY_ALL_WORKOUTS = "all_workouts"
    }
} 