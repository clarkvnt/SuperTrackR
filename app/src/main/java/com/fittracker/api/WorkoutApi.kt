package com.fittracker.api

import com.fittracker.models.Workout
import retrofit2.Response
import retrofit2.http.*

interface WorkoutApi {
    @POST("workouts")
    suspend fun createWorkout(@Body workout: Workout): Response<Workout>

    @GET("workouts/user/{userId}")
    suspend fun getWorkoutsByUser(@Path("userId") userId: String): Response<List<Workout>>

    @GET("workouts/{id}")
    suspend fun getWorkoutById(@Path("id") workoutId: String): Response<Workout>

    @PUT("workouts/{id}")
    suspend fun updateWorkout(
        @Path("id") workoutId: String,
        @Body workout: Workout
    ): Response<Workout>

    @DELETE("workouts/{id}")
    suspend fun deleteWorkout(@Path("id") workoutId: String): Response<Unit>
} 