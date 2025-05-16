package com.fittracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fittracker.models.Workout
import com.fittracker.storage.LocalWorkoutStorage
import com.fittracker.utils.WorkoutStatistics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WorkoutViewModel(application: Application) : AndroidViewModel(application) {
    private val workoutStorage = LocalWorkoutStorage(application)
    private val auth = FirebaseAuth.getInstance()
    
    private val _workouts = MutableStateFlow<List<Workout>>(emptyList())
    val workouts: StateFlow<List<Workout>> = _workouts

    private val _selectedWorkout = MutableStateFlow<Workout?>(null)
    val selectedWorkout: StateFlow<Workout?> = _selectedWorkout

    private val _workoutStats = MutableStateFlow<WorkoutStatistics.WorkoutStats?>(null)
    val workoutStats: StateFlow<WorkoutStatistics.WorkoutStats?> = _workoutStats

    init {
        loadWorkouts()
    }

    fun loadWorkouts() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val loadedWorkouts = workoutStorage.getWorkoutsByUser(userId)
            _workouts.value = loadedWorkouts
            _workoutStats.value = WorkoutStatistics.calculateStats(loadedWorkouts)
        }
    }

    fun getWorkout(workoutId: String) {
        viewModelScope.launch {
            _selectedWorkout.value = workoutStorage.getWorkout(workoutId)
        }
    }

    fun updateWorkout(workoutId: String, updatedWorkout: Workout) {
        viewModelScope.launch {
            val workoutWithId = updatedWorkout.copy(id = workoutId)
            if (workoutStorage.updateWorkout(workoutId, workoutWithId)) {
                loadWorkouts() // Reload all data to update statistics
            }
        }
    }

    fun deleteWorkout(workoutId: String) {
        viewModelScope.launch {
            if (workoutStorage.deleteWorkout(workoutId)) {
                loadWorkouts() // Reload all data to update statistics
            }
        }
    }

    fun createWorkoutWithDistance(distance: Float) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            val now = System.currentTimeMillis()
            val workout = Workout(
                userId = userId,
                startTime = java.util.Date(now),
                endTime = java.util.Date(now + (distance * 10 * 60 * 1000).toLong()), // fake duration: 10min/km
                distance = distance,
                duration = (distance * 10 * 60 * 1000).toLong(), // fake duration: 10min/km
                steps = (distance * 1300).toInt(), // fake steps: 1300 per km
                averageSpeed = 6f, // fake speed
                calories = (distance * 60).toInt(), // fake calories: 60 per km
                averageHeartRate = 120, // fake HR
                maxHeartRate = 150, // fake HR
                locations = emptyList()
            )
            workoutStorage.saveWorkout(workout)
            loadWorkouts()
        }
    }
} 