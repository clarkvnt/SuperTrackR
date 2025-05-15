package com.fittracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.fittracker.R
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.fittracker.utils.AchievementStorage
import com.fittracker.api.ApiClient
import com.fittracker.models.Workout
import com.fittracker.models.LocationPoint
import android.util.Log
import java.util.Date
import com.fittracker.storage.LocalWorkoutStorage
import android.content.Context

class LocationTrackingService : Service(), SensorEventListener {
    private val binder = LocalBinder()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null

    private val _locationUpdates = MutableStateFlow<List<Location>>(emptyList())
    val locationUpdates: StateFlow<List<Location>> = _locationUpdates

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking

    private var startTime: Long = 0
    private var totalDistance: Float = 0f
    private var lastLocation: Location? = null
    
    private var currentHeartRate: Int = 0
    private var maxHeartRate: Int = 0
    private var heartRateSum: Int = 0
    private var heartRateCount: Int = 0

    private lateinit var workoutStorage: LocalWorkoutStorage

    inner class LocalBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        createNotificationChannel()
        setupHeartRateSensor()
        workoutStorage = LocalWorkoutStorage(applicationContext)
    }

    private fun setupHeartRateSensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val heartRate = event.values[0].toInt()
            if (heartRate > 0) {
                currentHeartRate = heartRate
                maxHeartRate = maxOf(maxHeartRate, heartRate)
                heartRateSum += heartRate
                heartRateCount++
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for heart rate sensor
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TRACKING -> startTracking()
            ACTION_STOP_TRACKING -> stopTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    serviceScope.launch {
                        // Filter out unrealistic speeds (e.g., > 200 km/h)
                        val isRealisticSpeed = lastLocation?.let { last ->
                            val timeDiff = location.time - last.time
                            val distance = last.distanceTo(location)
                            val speed = (distance / timeDiff) * 3.6f // Convert to km/h
                            speed <= 200f // Filter out speeds above 200 km/h
                        } ?: true

                        if (isRealisticSpeed) {
                            val currentLocations = _locationUpdates.value.toMutableList()
                            currentLocations.add(location)
                            _locationUpdates.value = currentLocations

                            // Calculate distance with minimum threshold
                            lastLocation?.let { last ->
                                val distance = last.distanceTo(location)
                                // Only add distance if it's reasonable (e.g., > 1 meter)
                                if (distance > 1f) {
                                    totalDistance += distance
                                }
                            }
                            lastLocation = location
                        }
                    }
                }
            }
        }
    }

    private fun startTracking() {
        if (_isTracking.value) return

        startTime = System.currentTimeMillis()
        totalDistance = 0f
        _locationUpdates.value = emptyList()
        _isTracking.value = true
        currentHeartRate = 0
        maxHeartRate = 0
        heartRateSum = 0
        heartRateCount = 0

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000) // Changed to 1 second
            .setWaitForAccurateLocation(true) // Wait for accurate location
            .setMinUpdateIntervalMillis(1000) // Minimum 1 second between updates
            .setMaxUpdateDelayMillis(2000) // Maximum 2 seconds between updates
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            // Start heart rate monitoring
            heartRateSensor?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        } catch (e: SecurityException) {
            // Handle permission not granted
        }

        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun stopTracking() {
        if (!_isTracking.value) return

        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
        _isTracking.value = false
        updateAchievements()
        stopForeground(true)
        stopSelf()
    }

    private fun updateAchievements() {
        val context = applicationContext
        val stats = getTrackingStats()

        // Load current local achievements
        val current = AchievementStorage.loadAchievements(context)

        // Calculate new values
        val longestDistance = maxOf(stats.distance.toDouble(), current.longestDistance)
        val fastestSpeed = maxOf(stats.speed.toDouble(), current.fastestSpeed)
        val highestCalories = maxOf(stats.calories.toDouble(), current.highestCalories)
        val highestHeartRate = maxOf(stats.maxHeartRate, current.highestHeartRate)

        // Save locally
        AchievementStorage.saveAchievements(
            context,
            longestDistance,
            fastestSpeed,
            highestCalories,
            highestHeartRate
        )

        // Save workout to local storage
        saveWorkoutLocally(stats)
    }

    private fun saveWorkoutLocally(stats: TrackingStats) {
        serviceScope.launch {
            try {
                val auth = FirebaseAuth.getInstance()
                val userId = auth.currentUser?.uid ?: return@launch
                
                val workout = Workout(
                    userId = userId,
                    startTime = Date(startTime),
                    endTime = Date(System.currentTimeMillis()),
                    distance = stats.distance,
                    duration = stats.duration,
                    steps = stats.steps,
                    averageSpeed = stats.speed,
                    calories = stats.calories.toInt(),
                    averageHeartRate = stats.averageHeartRate,
                    maxHeartRate = stats.maxHeartRate,
                    locations = stats.locations.map { location ->
                        LocationPoint(
                            latitude = location.latitude,
                            longitude = location.longitude,
                            timestamp = Date(location.time)
                        )
                    }
                )

                val workoutId = workoutStorage.saveWorkout(workout)
                Log.d("LocationTrackingService", "Workout saved locally with ID: $workoutId")
            } catch (e: Exception) {
                Log.e("LocationTrackingService", "Error saving workout locally", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tracking Activity")
            .setContentText("Recording your route...")
            .setSmallIcon(R.drawable.ic_location)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun getTrackingStats(): TrackingStats {
        val duration = if (_isTracking.value) System.currentTimeMillis() - startTime else 0L
        val distance = totalDistance
        val steps = TrackingStatsCalculator.estimateSteps(distance)
        val speed = TrackingStatsCalculator.calculateSpeed(distance, duration)
        val calories = TrackingStatsCalculator.estimateCalories(distance)
        val averageHeartRate = if (heartRateCount > 0) heartRateSum / heartRateCount else 0
        return TrackingStats(
            isTracking = _isTracking.value,
            distance = distance,
            duration = duration,
            locations = ArrayList(_locationUpdates.value),
            steps = steps,
            speed = speed,
            calories = calories,
            currentHeartRate = currentHeartRate,
            averageHeartRate = averageHeartRate,
            maxHeartRate = maxHeartRate
        )
    }

    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START_TRACKING = "com.fittracker.action.START_TRACKING"
        const val ACTION_STOP_TRACKING = "com.fittracker.action.STOP_TRACKING"
    }
} 